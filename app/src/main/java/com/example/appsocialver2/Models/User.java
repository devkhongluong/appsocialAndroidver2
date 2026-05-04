package com.example.appsocialver2.Models;

public class User {
    public String userId;
    public String email;
    public String tendn;
    public String avatar;

    public User(String userId, String email, String tendn, String avatar) {
        this.userId = userId;
        this.email = email;
        this.tendn = tendn;
        this.avatar = avatar;
    }
}
