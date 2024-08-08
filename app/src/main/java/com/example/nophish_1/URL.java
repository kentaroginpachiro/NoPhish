package com.example.nophish_1;

public class URL {
    private String url;

    public URL() {
        // Default constructor required for calls to DataSnapshot.getValue(URL.class)
    }

    public URL(String url) {
        this.url = url;
    }

    public String getUrl() {
        return url;
    }

    public void setUrl(String url) {
        this.url = url;
    }
}
