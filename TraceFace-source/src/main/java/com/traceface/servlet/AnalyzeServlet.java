package com.traceface.servlet;

import com.traceface.config.AppConfig;
import com.traceface.model.BlockchainInfo;
import com.traceface.model.EvidenceRecord;
import com.traceface.model.SearchResults;
import com.traceface.model.WebMatch;
import com.traceface.service.BlockchainService;
import com.traceface.service.EvidenceService;
import com.traceface.service.FaceProcessingException;
import com.traceface.service.FaceService;
import com.traceface.service.WebFaceMatcher;
import com.traceface.service.WebSearchService;
import org.opencv.core.Mat;
import org.opencv.imgcodecs.Imgcodecs;
import org.web3j.protocol.core.methods.response.TransactionReceipt;

import javax.servlet.ServletException;
import javax.servlet.annotation.MultipartConfig;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.Part;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * POST /analyze
 * upload -> face detection -> Google Lens search -> face comparison
 * -> evidence (SHA-256) -> blockchain record -> verification -> result.jsp
 */
@WebServlet("/analyze")
@MultipartConfig(maxFileSize = 10L * 1024 * 1024, maxRequestSize = 12L * 1024 * 1024)
public class AnalyzeServlet extends HttpServlet {

    private static final long serialVersionUID = 1L;
    private static final Logger LOG = Logger.getLogger(AnalyzeServlet.class.getName());
    private static final Set<String> ALLOWED_EXTENSIONS = Set.of("jpg", "jpeg", "png", "webp", "bmp");

    private transient FaceService faceService;
    private transient WebSearchService webSearchService;
    private transient WebFaceMatcher webFaceMatcher;
    private transient EvidenceService evidenceService;
    private transient BlockchainService blockchainService;

    @Override
    public void init() throws ServletException {
        faceService = (FaceService) getServletContext().getAttribute("faceService");
        webSearchService = (WebSearchService) getServletContext().getAttribute("webSearchService");
        webFaceMatcher = (WebFaceMatcher) getServletContext().getAttribute("webFaceMatcher");
        evidenceService = (EvidenceService) getServletContext().getAttribute("evidenceService");
        blockchainService = (BlockchainService) getServletContext().getAttribute("blockchainService");
    }

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws IOException {
        response.sendRedirect(request.getContextPath() + "/index.jsp");
    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {

        // ---------------- Check upload ----------------
        Part photo = request.getPart("photo");
        if (photo == null) {
            fail(request, response, "No photo uploaded.", null);
            return;
        }

        String submitted = photo.getSubmittedFileName();
        if (submitted == null || submitted.isBlank()) {
            fail(request, response, "No file selected.", null);
            return;
        }

        String filename = secureFilename(submitted);
        if (filename.isEmpty()) {
            fail(request, response, "Invalid filename.", null);
            return;
        }

        // ---------------- Save upload ----------------
        Path uploadedPath = AppConfig.uploadDir().resolve(filename);
        try (InputStream in = photo.getInputStream()) {
            Files.copy(in, uploadedPath, StandardCopyOption.REPLACE_EXISTING);
        } catch (IOException e) {
            fail(request, response, "Could not save uploaded image.", null);
            return;
        }

        // ---------------- Read upload ----------------
        Mat queryImage = Imgcodecs.imread(uploadedPath.toString());
        if (queryImage.empty()) {
            fail(request, response, "Could not read uploaded image.", null);
            return;
        }

        // ---------------- Detect + encode uploaded face ----------------
        Mat queryFeature;
        try {
            queryFeature = faceService.createQueryFeature(queryImage);
        } catch (FaceProcessingException e) {
            fail(request, response, e.getMessage(), null);
            return;
        } finally {
            queryImage.release();
        }

        try {
            // ---------------- Web search ----------------
            SearchResults webResults;
            try {
                webResults = webSearchService.search(uploadedPath);
            } catch (Exception e) {
                LOG.log(Level.WARNING, "Web search failed", e);
                fail(request, response,
                        "Face detection succeeded, but web search failed: " + e.getMessage(), null);
                return;
            }
            int totalWebResults = webResults.getTotalImageResults();

            // ---------------- Face match web results ----------------
            List<WebMatch> matches = webFaceMatcher.findMatches(queryFeature, webResults);
            if (matches.isEmpty()) {
                fail(request, response,
                        "Google Lens found " + totalWebResults + " web image results, but no returned "
                                + "image could be successfully processed for a face match.",
                        totalWebResults);
                return;
            }

            WebMatch best = matches.get(0);
            boolean matchFound = best.getSimilarity() >= AppConfig.MATCH_THRESHOLD;

            // ---------------- Evidence + blockchain ----------------
            EvidenceRecord evidence = null;
            String blockchainStatus = BlockchainInfo.NOT_RECORDED;
            String transactionHash = null;

            if (matchFound) {
                try {
                    evidence = evidenceService.createEvidence(best, filename);
                } catch (Exception e) {
                    LOG.log(Level.WARNING, "Evidence creation failed", e);
                    fail(request, response,
                            "Web face match succeeded, but evidence creation failed: " + e.getMessage(), null);
                    return;
                }
                recordOnBlockchain(evidence);
                blockchainStatus = evidence.getBlockchain().getStatus();
                transactionHash = evidence.getBlockchain().getTransactionHash();
            }

            // ---------------- Display ----------------
            request.setAttribute("success", true);
            request.setAttribute("matchFound", matchFound);
            request.setAttribute("bestMatch", best);
            request.setAttribute("matches", matches);
            request.setAttribute("evidenceRecord", evidence);
            request.setAttribute("blockchainStatus", blockchainStatus);
            request.setAttribute("transactionHash", transactionHash);
            request.setAttribute("webResultsCount", totalWebResults);
            request.setAttribute("message",
                    "Face detected, public web content searched, and returned images "
                            + "were compared using SFace.");
            request.getRequestDispatcher("/result.jsp").forward(request, response);

        } finally {
            queryFeature.release();
        }
    }

    // ------------------------------------------------------------------
    // Blockchain submission, confirmation and verification
    // ------------------------------------------------------------------

    private void recordOnBlockchain(EvidenceRecord evidence) {
        String evidenceId = evidence.getEvidence().getEvidenceId();
        String sha256 = evidence.getSha256();
        String txHash = null;

        try {
            updateStatus(evidence, new BlockchainInfo(BlockchainInfo.SUBMITTING, null));

            txHash = blockchainService.recordHashOnChain(sha256);

            updateStatus(evidence, new BlockchainInfo(BlockchainInfo.CONFIRMING, txHash));

            TransactionReceipt receipt = blockchainService.waitForConfirmation(txHash);
            long blockNumber = receipt.getBlockNumber().longValue();

            if (!receipt.isStatusOK()) {
                updateStatus(evidence,
                        new BlockchainInfo(BlockchainInfo.FAILED, txHash).withBlockNumber(blockNumber));
                return;
            }

            BlockchainService.Verification v = blockchainService.verifyHash(txHash, sha256);
            String status = v.valid ? BlockchainInfo.CONFIRMED : BlockchainInfo.VERIFICATION_FAILED;

            updateStatus(evidence, new BlockchainInfo(status, txHash)
                    .withBlockNumber(blockNumber)
                    .withOnChainSha256(v.blockchainHash)
                    .withVerified(v.valid));

        } catch (Exception e) {
            LOG.log(Level.WARNING, "Blockchain step failed for " + evidenceId, e);
            updateStatus(evidence,
                    new BlockchainInfo(BlockchainInfo.FAILED, txHash).withError(e.getMessage()));
        }
    }

    private void updateStatus(EvidenceRecord evidence, BlockchainInfo info) {
        evidenceService.updateBlockchain(evidence.getEvidence().getEvidenceId(), info);
        evidence.setBlockchain(info);
    }

    // ------------------------------------------------------------------
    // Helpers
    // ------------------------------------------------------------------

    private void fail(HttpServletRequest request, HttpServletResponse response,
                      String message, Integer webResultsCount) throws ServletException, IOException {
        request.setAttribute("success", false);
        request.setAttribute("message", message);
        if (webResultsCount != null) {
            request.setAttribute("webResultsCount", webResultsCount);
        }
        request.getRequestDispatcher("/result.jsp").forward(request, response);
    }

    /** Equivalent of Werkzeug's secure_filename plus an image-extension whitelist. */
    static String secureFilename(String original) {
        String name = original.replace('\\', '/');
        name = name.substring(name.lastIndexOf('/') + 1);
        name = name.replaceAll("[^A-Za-z0-9._-]", "_").replaceAll("^[._]+", "");

        int dot = name.lastIndexOf('.');
        String extension = dot >= 0 ? name.substring(dot + 1).toLowerCase(Locale.ROOT) : "";
        return ALLOWED_EXTENSIONS.contains(extension) ? name : "";
    }
}
