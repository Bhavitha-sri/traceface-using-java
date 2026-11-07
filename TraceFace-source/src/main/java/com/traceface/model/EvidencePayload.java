package com.traceface.model;

import com.google.gson.annotations.SerializedName;

/** The exact information protected by the SHA-256 fingerprint. */
public class EvidencePayload {
    @SerializedName("evidence_id")
    private String evidenceId;
    @SerializedName("uploaded_file")
    private String uploadedFile;
    @SerializedName("matched_post_id")
    private String matchedPostId;
    private String title;
    private String source;
    @SerializedName("source_url")
    private String sourceUrl;
    @SerializedName("face_similarity")
    private double faceSimilarity;
    @SerializedName("created_at")
    private String createdAt;

    public EvidencePayload(String evidenceId, String uploadedFile, String matchedPostId,
                           String title, String source, String sourceUrl,
                           double faceSimilarity, String createdAt) {
        this.evidenceId = evidenceId;
        this.uploadedFile = uploadedFile;
        this.matchedPostId = matchedPostId;
        this.title = title;
        this.source = source;
        this.sourceUrl = sourceUrl;
        this.faceSimilarity = faceSimilarity;
        this.createdAt = createdAt;
    }

    public String getEvidenceId()     { return evidenceId; }
    public String getUploadedFile()   { return uploadedFile; }
    public String getMatchedPostId()  { return matchedPostId; }
    public String getTitle()          { return title; }
    public String getSource()         { return source; }
    public String getSourceUrl()      { return sourceUrl; }
    public double getFaceSimilarity() { return faceSimilarity; }
    public String getCreatedAt()      { return createdAt; }
}
