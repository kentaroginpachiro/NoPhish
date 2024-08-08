package com.example.nophish_1;

public class FetchedEmail {
    private String from;
    private String subject;
    private String content;
    private String truncatedContent;
    private long timestamp; // To store the time the email was fetched
    private String context;
    private String urlStatus;
    private String normalizedContent;

    public FetchedEmail() {
        // Default constructor required for calls to DataSnapshot.getValue(FetchedEmail.class)
    }

    public FetchedEmail(String from, String subject, String content, String truncatedContent, long timestamp, String context, String urlStatus, String normalizedContent) {
        this.from = from;
        this.subject = subject;
        this.content = content;
        this.truncatedContent = truncatedContent;
        this.timestamp = timestamp;
        this.context = context;
        this.urlStatus = urlStatus;
        this.normalizedContent = normalizedContent;
    }

    public String getFrom() {
        return from;
    }

    public void setFrom(String from) {
        this.from = from;
    }

    public String getSubject() {
        return subject;
    }

    public void setSubject(String subject) {
        this.subject = subject;
    }

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }

    public String getTruncatedContent() {
        return truncatedContent;
    }

    public void setTruncatedContent(String truncatedContent) {
        this.truncatedContent = truncatedContent;
    }

    public long getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(long timestamp) {
        this.timestamp = timestamp;
    }

    public String getContext() {
        return context;
    }

    public void setContext(String context) {
        this.context = context;
    }

    public String getUrlStatus() {
        return urlStatus;
    }

    public void setUrlStatus(String urlStatus) {
        this.urlStatus = urlStatus;
    }

    public String getNormalizedContent() {
        return normalizedContent;
    }

    public void setNormalizedContent(String normalizedContent) {
        this.normalizedContent = normalizedContent;
    }
}
