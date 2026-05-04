package com.example.appsocialver2.Models;
import com.google.firebase.firestore.ServerTimestamp;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

public class Post {
    private String postId;
    private String ownerUid;
    private String imageUrl;
    private String description;
    private String locationName; // Lưu tên địa danh từ GPS
    private List<String> likes = new ArrayList<>(); // Danh sách người thả tim

    @ServerTimestamp
    private Date timestamp; // Thời gian đăng bài tự động từ server [cite: 98]

    // Bắt buộc phải có Constructor trống để Firebase Firestore có thể chuyển đổi dữ liệu [cite: 49]
    public Post() {}

    public Post(String postId, String ownerUid, String imageUrl, String description, String locationName) {
        this.postId = postId;
        this.ownerUid = ownerUid;
        this.imageUrl = imageUrl;
        this.description = description;
        this.locationName = locationName;
    }

    // Getter và Setter
    public List<String> getLikes() { return likes; }
    public void setLikes(List<String> likes) { this.likes = likes; }

    public String getPostId() { return postId; }
    public void setPostId(String postId) { this.postId = postId; }
    public String getOwnerUid() { return ownerUid; }
    public void setOwnerUid(String ownerUid) { this.ownerUid = ownerUid; }
    public String getImageUrl() { return imageUrl; }
    public void setImageUrl(String imageUrl) { this.imageUrl = imageUrl; }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    public String getLocationName() { return locationName; }
    public void setLocationName(String locationName) { this.locationName = locationName; }
    public Date getTimestamp() { return timestamp; }
    public void setTimestamp(Date timestamp) { this.timestamp = timestamp; }
}
