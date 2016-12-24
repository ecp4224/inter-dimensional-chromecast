package com.boxtrotstudio.android.interdimensionalcable.core;


public class Video {
    private String thumbnailUrl;
    private String title;

    public Video(String title, String thumbnailUrl) {
        this.title = title;
        this.thumbnailUrl = thumbnailUrl;
    }

    public String getThumbnailUrl() {
        return thumbnailUrl;
    }

    public String getTitle() {
        return title;
    }
}
