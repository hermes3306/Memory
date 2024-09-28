package com.jason.memory;

import java.util.ArrayList;
import java.util.List;

public class MemoryItem {
    private long id;
    private String title;
    private String date;
    private String memoryText;
    private String[] pictures;
    private String place;

    public MemoryItem(long id, String title, String date, String memoryText, String[] pictures, String place) {
        this.id = id;
        this.title = title;
        this.date = date;
        this.memoryText = memoryText;
        this.pictures = pictures != null ? pictures : new String[9];
        this.place = place;
    }

    public MemoryItem(String title, String date, String memoryText, List<String> pictureUrls, String place) {
        this(-1, title, date, memoryText, convertListToArray(pictureUrls), place);
    }

    private static String[] convertListToArray(List<String> list) {
        String[] array = new String[9];
        if (list != null) {
            for (int i = 0; i < Math.min(list.size(), 9); i++) {
                array[i] = list.get(i);
            }
        }
        return array;
    }

    // Add getter and setter for place
    public String getPlace() {
        return place;
    }

    public void setPlace(String place) {
        this.place = place;
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

    public String[] getPictures() {
        return pictures;
    }

    public void setPictures(String[] pictures) {
        this.pictures = pictures;
    }


    public void addPicture(String pictureUrl, int index) {
        if (index >= 0 && index < 9) {
            pictures[index] = pictureUrl;
        }
    }
}
