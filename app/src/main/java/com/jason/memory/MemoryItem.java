package com.jason.memory;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class MemoryItem {
    private long id;
    private String title;
    private long timestamp;
    private String memoryText;
    private String userId;
    private long placeId;
    private String audio;
    private String hashtag;
    private int likes;
    private List<String> pictures;
    private List<String> comments;
    private String userProfilePictureUrl;
    private String whoLikes; // New field to store who liked the memory

    public MemoryItem(long id, String title, long timestamp, String memoryText) {
        this.id = id;
        this.title = title;
        this.timestamp = timestamp;
        this.memoryText = memoryText;
        this.pictures = new ArrayList<>();
        this.comments = new ArrayList<>();
        this.whoLikes = "";
    }

    public MemoryItem(String title, long timestamp, String memoryText) {
        this(-1, title, timestamp, memoryText);
    }

    public boolean isLikedBy(String userId) {
        return whoLikes != null && Arrays.asList(whoLikes.split(",")).contains(userId);
    }

    public void addLike(String userId) {
        if (whoLikes == null || whoLikes.isEmpty()) {
            whoLikes = userId;
        } else if (!isLikedBy(userId)) {
            whoLikes += "," + userId;
        }
        likes = whoLikes.split(",").length;
    }

    // Add getter and setter
    public String getUserProfilePictureUrl() { return userProfilePictureUrl; }
    public void setUserProfilePictureUrl(String url) { this.userProfilePictureUrl = url; }

    public String getFormattedDate() {
        Date memoryDate = new Date(timestamp);
        Date currentDate = new Date();

        Calendar memoryCal = Calendar.getInstance();
        memoryCal.setTime(memoryDate);

        Calendar currentCal = Calendar.getInstance();
        currentCal.setTime(currentDate);

        SimpleDateFormat timeFormat = new SimpleDateFormat("HH:mm", Locale.getDefault());
        SimpleDateFormat dateFormat = new SimpleDateFormat("MM-dd", Locale.getDefault());
        SimpleDateFormat yearFormat = new SimpleDateFormat("yy-MM-dd", Locale.getDefault());

        if (memoryCal.get(Calendar.YEAR) == currentCal.get(Calendar.YEAR)) {
            if (memoryCal.get(Calendar.DAY_OF_YEAR) == currentCal.get(Calendar.DAY_OF_YEAR)) {
                // If it's today, return time
                return timeFormat.format(memoryDate);
            } else {
                // If it's this year but not today, return MM-dd
                return dateFormat.format(memoryDate);
            }
        } else {
            // If it's not this year, return yy-MM-dd
            return yearFormat.format(memoryDate);
        }
    }

    // Getters and setters
    public long getId() { return id; }
    public void setId(long id) { this.id = id; }
    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }
    public long getTimestamp() { return timestamp; }
    public void setTimestamp(long timestamp) { this.timestamp = timestamp; }
    public String getMemoryText() { return memoryText; }
    public void setMemoryText(String memoryText) { this.memoryText = memoryText; }
    public String getUserId() { return userId; }
    public void setUserId(String userId) { this.userId = userId; }
    public long getPlaceId() { return placeId; }
    public void setPlaceId(long placeId) { this.placeId = placeId; }
    public String getAudio() { return audio; }
    public void setAudio(String audio) { this.audio = audio; }
    public String getHashtag() { return hashtag; }
    public void setHashtag(String hashtag) { this.hashtag = hashtag; }
    public int getLikes() { return likes; }
    public void setLikes(int likes) { this.likes = likes; }
    public List<String> getPictures() { return pictures; }
    public void setPictures(List<String> pictures) { this.pictures = pictures; }
    public List<String> getComments() { return comments; }
    public void setComments(List<String> comments) { this.comments = comments; }
    public String getWhoLikes() { return whoLikes; }
    public void setWhoLikes(String whoLikes) { this.whoLikes = whoLikes; }

    public void addPicture(String pictureUrl) {
        if (this.pictures == null) {
            this.pictures = new ArrayList<>();
        }
        this.pictures.add(pictureUrl);
    }


    public void addComment(String comment) {
        if (this.comments == null) {
            this.comments = new ArrayList<>();
        }
        this.comments.add(comment);
    }
}