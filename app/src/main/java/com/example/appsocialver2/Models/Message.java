package com.example.appsocialver2.Models;
import com.google.firebase.firestore.ServerTimestamp;
import java.util.Date;
public class Message {
    private String messageId;
    private String senderId;    // ID người gửi
    private String receiverId;  // ID người nhận
    private String text;        // Nội dung tin nhắn

    @ServerTimestamp
    private Date timestamp;     // Thời gian nhắn (tự động lấy từ Server Firebase)

    // Constructor trống cho Firebase
    public Message() {}

    public Message(String messageId, String senderId, String receiverId, String text) {
        this.messageId = messageId;
        this.senderId = senderId;
        this.receiverId = receiverId;
        this.text = text;
    }

    // Getter và Setter
    public String getMessageId() { return messageId; }
    public void setMessageId(String messageId) { this.messageId = messageId; }

    public String getSenderId() { return senderId; }
    public void setSenderId(String senderId) { this.senderId = senderId; }

    public String getReceiverId() { return receiverId; }
    public void setReceiverId(String receiverId) { this.receiverId = receiverId; }

    public String getText() { return text; }
    public void setText(String text) { this.text = text; }

    public Date getTimestamp() { return timestamp; }
    public void setTimestamp(Date timestamp) { this.timestamp = timestamp; }
}
