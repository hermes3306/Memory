package com.jason.memory;

import java.util.ArrayList;
import java.util.List;

public class MemoryItem {
    private long id;
    private String title;
    private String date;
    private String memoryText;
    private String userId;
    private long placeId;
    private String audio;
    private String hashtag;
    private int likes;
    private List<String> pictures;
    private List<String> comments;

    public MemoryItem(long id, String title, String date, String memoryText) {
        this.id = id;
        this.title = title;
        this.date = date;
        this.memoryText = memoryText;
        this.pictures = new ArrayList<>();
        this.comments = new ArrayList<>();
    }

    public MemoryItem(String title, String date, String memoryText) {
        this(-1, title, date, memoryText);
    }

    // Getters and setters
    public long getId() {
        return id;
    }

    public void setId(long id) {
        this.id = id;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getDate() {
        return date;
    }

    public void setDate(String date) {
        this.date = date;
    }

    public String getMemoryText() {
        return memoryText;
    }

    public void setMemoryText(String memoryText) {
        this.memoryText = memoryText;
    }

    public String getUserId() {
        return userId;
    }

    public void setUserId(String userId) {
        this.userId = userId;
    }

    public long getPlaceId() {
        return placeId;
    }

    public void setPlaceId(long placeId) {
        this.placeId = placeId;
    }

    public String getAudio() {
        return audio;
    }

    public void setAudio(String audio) {
        this.audio = audio;
    }

    public String getHashtag() {
        return hashtag;
    }

    public void setHashtag(String hashtag) {
        this.hashtag = hashtag;
    }

    public int getLikes() {
        return likes;
    }

    public void setLikes(int likes) {
        this.likes = likes;
    }

    public List<String> getPictures() {
        return pictures;
    }

    public void setPictures(List<String> pictures) {
        this.pictures = pictures;
    }

    public void addPicture(String pictureUrl) {
        if (this.pictures == null) {
            this.pictures = new ArrayList<>();
        }
        this.pictures.add(pictureUrl);
    }

    public List<String> getComments() {
        return comments;
    }

    public void setComments(List<String> comments) {
        this.comments = comments;
    }

    public void addComment(String comment) {
        if (this.comments == null) {
            this.comments = new ArrayList<>();
        }
        this.comments.add(comment);
    }
}