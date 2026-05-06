package com.example.appsocialver2.Models;

import com.google.firebase.firestore.ServerTimestamp;

import java.util.Date;

public class RecentChat {
    private String friendId;
    private String friendName;
    private String friendAvatar;
    private String lastMessage;
    private boolean hasUnread;

    @ServerTimestamp
    private Date timestamp;

    public RecentChat() {
    }

    public RecentChat(String friendId, String friendName, String friendAvatar, String lastMessage, boolean hasUnread) {
        this.friendId = friendId;
        this.friendName = friendName;
        this.friendAvatar = friendAvatar;
        this.lastMessage = lastMessage;
        this.hasUnread = hasUnread;
    }

    public String getFriendId() {
        return friendId;
    }

    public void setFriendId(String friendId) {
        this.friendId = friendId;
    }

    public String getFriendName() {
        return friendName;
    }

    public void setFriendName(String friendName) {
        this.friendName = friendName;
    }

    public String getFriendAvatar() {
        return friendAvatar;
    }

    public void setFriendAvatar(String friendAvatar) {
        this.friendAvatar = friendAvatar;
    }

    public String getLastMessage() {
        return lastMessage;
    }

    public void setLastMessage(String lastMessage) {
        this.lastMessage = lastMessage;
    }

    public boolean isHasUnread() {
        return hasUnread;
    }

    public void setHasUnread(boolean hasUnread) {
        this.hasUnread = hasUnread;
    }

    public Date getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(Date timestamp) {
        this.timestamp = timestamp;
    }
}
