package com.jason.memory;

import java.util.ArrayList;
import java.util.List;

public class MemoryItem {
    private String title;
    private String date;
    private String memoryText;
    private List<String> pictureUrls;
    private String audioUrl;

    public MemoryItem(String title, String date, String memoryText) {
        this.title = title;
        this.date = date;
        this.memoryText = memoryText;
        this.pictureUrls = new ArrayList<>();
    }

    // Getters and setters
    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }

    public String getDate() { return date; }
    public void setDate(String date) { this.date = date; }

    public String getMemoryText() { return memoryText; }
    public void setMemoryText(String memoryText) { this.memoryText = memoryText; }

    public List<String> getPictureUrls() { return pictureUrls; }
    public void addPictureUrl(String url) { this.pictureUrls.add(url); }

    public String getAudioUrl() { return audioUrl; }
    public void setAudioUrl(String audioUrl) { this.audioUrl = audioUrl; }
}
