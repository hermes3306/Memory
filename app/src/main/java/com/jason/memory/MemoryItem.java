package com.jason.memory;

import java.util.ArrayList;
import java.util.List;

public class MemoryItem {
    private long id;
    private String title;
    private String date;
    private String memoryText;

    public MemoryItem(long id, String title, String date, String memoryText) {
        this.id = id;
        this.title = title;
        this.date = date;
        this.memoryText = memoryText;
    }

    public MemoryItem(String title, String date, String memoryText) {
        this(-1, title, date, memoryText);
    }

    public long getId() {
        return id;
    }

    public void setId(long id) {
        this.id = id;
    }
    // Getters and setters
    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }

    public String getDate() { return date; }
    public void setDate(String date) { this.date = date; }

    public String getMemoryText() { return memoryText; }
    public void setMemoryText(String memoryText) { this.memoryText = memoryText; }

}
