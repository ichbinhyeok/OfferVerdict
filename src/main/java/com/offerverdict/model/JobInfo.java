package com.offerverdict.model;

public class JobInfo {
    private String title;
    private String slug;
    private String category;
    private boolean major = false; // Default false

    public JobInfo() {
    }

    public JobInfo(String title, String slug, String category) {
        this.title = title;
        this.slug = slug;
        this.category = category;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getSlug() {
        return slug;
    }

    public void setSlug(String slug) {
        this.slug = slug;
    }

    public String getCategory() {
        return category;
    }

    public void setCategory(String category) {
        this.category = category;
    }

    public boolean isMajor() {
        return major;
    }

    public void setMajor(boolean major) {
        this.major = major;
    }
}
