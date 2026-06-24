package com.example.digitalbinder;

public class Assignment {
    private String id;
    private String title;
    private String subject;
    private String imageBase64;
    private String extractedText;
    private String topicSummary;
    private int masteryLevel;
    private long nextReviewDate;
    private long createdAt;

    // 1. Mandatory Empty Constructor for Firestore
    public Assignment() {}

    // 2. Main Constructor
    public Assignment(String title, String subject, String imageBase64, String extractedText, String topicSummary) {
        this.title = title;
        this.subject = subject;
        this.imageBase64 = imageBase64;
        this.extractedText = extractedText;
        this.topicSummary = topicSummary;
        this.masteryLevel = 1;
        this.nextReviewDate = System.currentTimeMillis();
        this.createdAt = System.currentTimeMillis();
    }

    // 3. ALL Getters AND Setters (Firestore REQUIRES both to map data)
    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }

    public String getSubject() { return subject; }
    public void setSubject(String subject) { this.subject = subject; }

    public String getImageBase64() { return imageBase64; }
    public void setImageBase64(String imageBase64) { this.imageBase64 = imageBase64; }

    public String getExtractedText() { return extractedText; }
    public void setExtractedText(String extractedText) { this.extractedText = extractedText; }

    public String getTopicSummary() { return topicSummary; }
    public void setTopicSummary(String topicSummary) { this.topicSummary = topicSummary; }

    public int getMasteryLevel() { return masteryLevel; }
    public void setMasteryLevel(int masteryLevel) { this.masteryLevel = masteryLevel; }

    public long getNextReviewDate() { return nextReviewDate; }
    public void setNextReviewDate(long nextReviewDate) { this.nextReviewDate = nextReviewDate; }

    public long getCreatedAt() { return createdAt; }
    public void setCreatedAt(long createdAt) { this.createdAt = createdAt; }

    // Date Formatter Helper for the UI
    public String getFormattedDate() {
        java.text.SimpleDateFormat sdf = new java.text.SimpleDateFormat("MMM dd, yyyy", java.util.Locale.getDefault());
        return sdf.format(new java.util.Date(createdAt));
    }
}