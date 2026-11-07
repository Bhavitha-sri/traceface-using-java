package com.traceface.service;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;
import com.traceface.config.AppConfig;
import com.traceface.model.BlockchainInfo;
import com.traceface.model.EvidencePayload;
import com.traceface.model.EvidenceRecord;
import com.traceface.model.WebMatch;

import java.io.IOException;
import java.lang.reflect.Type;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.UUID;

/** Creates SHA-256 evidence records and stores them in evidence_records.json. */
public class EvidenceService {

    private static final Type LIST_TYPE = new TypeToken<List<EvidenceRecord>>() { }.getType();

    private final Gson gson = new GsonBuilder().setPrettyPrinting().disableHtmlEscaping().create();
    private final Gson canonicalGson = new GsonBuilder().disableHtmlEscaping().create();

    // ------------------------------------------------------------------
    // Create evidence
    // ------------------------------------------------------------------

    public EvidenceRecord createEvidence(WebMatch bestMatch, String uploadedFilename) throws IOException {
        OffsetDateTime now = OffsetDateTime.now(ZoneOffset.UTC);

        String evidenceId = "TF-"
                + now.format(DateTimeFormatter.ofPattern("yyyyMMdd"))
                + "-"
                + UUID.randomUUID().toString().replace("-", "").substring(0, 8).toUpperCase();

        double similarity = Math.round(bestMatch.getSimilarity() * 1_000_000d) / 1_000_000d;

        EvidencePayload payload = new EvidencePayload(
                evidenceId,
                uploadedFilename,
                bestMatch.getId(),
                bestMatch.getTitle(),
                bestMatch.getSource(),
                bestMatch.getUrl(),
                similarity,
                now.toString());

        EvidenceRecord record = new EvidenceRecord(payload, calculateSha256(payload));
        saveRecord(record);
        return record;
    }

    // ------------------------------------------------------------------
    // Canonical JSON (sorted keys, no spaces) -> SHA-256
    // ------------------------------------------------------------------

    public String calculateSha256(EvidencePayload p) {
        Map<String, Object> canonical = new TreeMap<>();
        canonical.put("evidence_id", p.getEvidenceId());
        canonical.put("uploaded_file", p.getUploadedFile());
        canonical.put("matched_post_id", p.getMatchedPostId());
        canonical.put("title", p.getTitle());
        canonical.put("source", p.getSource());
        canonical.put("source_url", p.getSourceUrl());
        canonical.put("face_similarity", p.getFaceSimilarity());
        canonical.put("created_at", p.getCreatedAt());

        String json = canonicalGson.toJson(canonical);   // TreeMap => keys in sorted order

        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(json.getBytes(StandardCharsets.UTF_8));
            StringBuilder hex = new StringBuilder();
            for (byte b : hash) {
                hex.append(String.format("%02x", b));
            }
            return hex.toString();
        } catch (Exception e) {
            throw new IllegalStateException("SHA-256 not available", e);
        }
    }

    // ------------------------------------------------------------------
    // JSON file storage
    // ------------------------------------------------------------------

    public synchronized List<EvidenceRecord> loadRecords() {
        Path file = AppConfig.evidenceFile();
        if (!Files.exists(file)) {
            return new ArrayList<>();
        }
        try {
            String text = Files.readString(file, StandardCharsets.UTF_8);
            List<EvidenceRecord> records = gson.fromJson(text, LIST_TYPE);
            return records != null ? records : new ArrayList<>();
        } catch (Exception e) {
            return new ArrayList<>();
        }
    }

    private synchronized void saveRecord(EvidenceRecord record) throws IOException {
        List<EvidenceRecord> records = loadRecords();
        records.add(record);
        writeAll(records);
    }

    /** Attach the latest blockchain status to the stored record. */
    public synchronized void updateBlockchain(String evidenceId, BlockchainInfo info) {
        List<EvidenceRecord> records = loadRecords();
        for (EvidenceRecord record : records) {
            if (evidenceId.equals(record.getEvidence().getEvidenceId())) {
                record.setBlockchain(info);
                break;
            }
        }
        try {
            writeAll(records);
        } catch (IOException ignored) {
            // status update is best-effort
        }
    }

    private void writeAll(List<EvidenceRecord> records) throws IOException {
        Files.createDirectories(AppConfig.dataDir());
        Files.writeString(AppConfig.evidenceFile(), gson.toJson(records, LIST_TYPE), StandardCharsets.UTF_8);
    }
}
