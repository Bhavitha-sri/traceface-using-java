package com.traceface.model;

/** One image result returned by Google Lens (exact or visual match). */
public class SearchItem {
    private String title;
    private String link;
    private String source;
    private String image;
    private String thumbnail;
    private boolean exactMatch;

    public SearchItem() { }

    public SearchItem(String title, String link, String source,
                      String image, String thumbnail, boolean exactMatch) {
        this.title = title;
        this.link = link;
        this.source = source;
        this.image = image;
        this.thumbnail = thumbnail;
        this.exactMatch = exactMatch;
    }

    public String getTitle()     { return title; }
    public String getLink()      { return link; }
    public String getSource()    { return source; }
    public String getImage()     { return image; }
    public String getThumbnail() { return thumbnail; }
    public boolean isExactMatch(){ return exactMatch; }
}
