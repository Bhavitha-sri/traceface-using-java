package com.traceface.service;

import com.traceface.config.AppConfig;
import com.traceface.model.SearchItem;
import com.traceface.model.SearchResults;
import com.traceface.model.WebMatch;
import org.opencv.core.Mat;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/** Downloads the images returned by Google Lens and compares their faces with the query face. */
public class WebFaceMatcher {

    private final FaceService faceService;
    private final ImageDownloader downloader;

    public WebFaceMatcher(FaceService faceService, ImageDownloader downloader) {
        this.faceService = faceService;
        this.downloader = downloader;
    }

    public List<WebMatch> findMatches(Mat queryFeature, SearchResults results) {
        List<WebMatch> candidates = new ArrayList<>();
        Set<String> seenUrls = new HashSet<>();

        // Exact matches first, then visual matches
        List<SearchItem> ordered = new ArrayList<>(results.getExactMatches());
        ordered.addAll(results.getVisualMatches());

        for (SearchItem item : ordered) {
            if (candidates.size() >= AppConfig.MAX_WEB_CANDIDATES) {
                break;
            }

            String pageUrl = item.getLink();
            String imageUrl = item.getImage();

            if (pageUrl == null || !isHttpUrl(pageUrl) || !seenUrls.add(pageUrl) || imageUrl == null) {
                continue;
            }

            Mat candidateImage = downloader.download(imageUrl);
            if (candidateImage == null) {
                continue;
            }

            try {
                Double similarity = faceService.compareQueryWithImage(queryFeature, candidateImage);
                if (similarity == null) {
                    continue;
                }

                candidates.add(new WebMatch(
                        String.format("WEB-%03d", candidates.size() + 1),
                        item.getTitle() != null ? item.getTitle() : "Public Web Result",
                        item.getSource() != null ? item.getSource() : "Web",
                        pageUrl,
                        imageUrl,
                        similarity,
                        item.isExactMatch()));
            } finally {
                candidateImage.release();
            }
        }

        // Highest face similarity first
        candidates.sort(Comparator.comparingDouble(WebMatch::getSimilarity).reversed());
        return candidates;
    }

    private static boolean isHttpUrl(String url) {
        String lower = url.toLowerCase();
        return lower.startsWith("http://") || lower.startsWith("https://");
    }
}
