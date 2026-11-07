package com.traceface.model;

import com.traceface.config.AppConfig;

/** A web image whose face was compared with the uploaded face. */
public class WebMatch {
    private String id;
    private String title;
    private String source;
    private String url;
    private String image;
    private double similarity;
    private boolean exactMatch;

    public WebMatch(String id, String title, String source, String url,
                    String image, double similarity, boolean exactMatch) {
        this.id = id;
        this.title = title;
        this.source = source;
        this.url = url;
        this.image = image;
        this.similarity = similarity;
        this.exactMatch = exactMatch;
    }

    public String getId()          { return id; }
    public String getTitle()       { return title; }
    public String getSource()      { return source; }
    public String getUrl()         { return url; }
    public String getImage()       { return image; }
    public double getSimilarity()  { return similarity; }
    public boolean isExactMatch()  { return exactMatch; }

    /** True when the similarity reaches the SFace threshold (shows the MATCH badge). */
    public boolean isMatch() { return similarity >= AppConfig.MATCH_THRESHOLD; }

    /** Similarity as a percentage, used by the JSP pages. */
    public double getSimilarityPercent() { return similarity * 100.0; }
}
