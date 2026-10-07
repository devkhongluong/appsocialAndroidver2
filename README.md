# 📱 AppSocial Android v2

Ứng dụng mạng xã hội trên Android được xây dựng bằng Java và Firebase. Cho phép người dùng đăng bài ảnh, kết bạn, nhắn tin realtime và chia sẻ vị trí.

---

## 🚀 Tính năng chính

| Tính năng | Mô tả |
|-----------|-------|
| 🔐 Xác thực | Đăng nhập / Đăng ký qua Firebase Authentication |
| 🏠 News Feed | Xem bài viết của bản thân và bạn bè theo thời gian thực |
| 📷 Đăng bài | Chụp ảnh bằng CameraX và đăng kèm mô tả, vị trí GPS |
| 👥 Kết bạn | Gửi / nhận / xác nhận lời mời kết bạn |
| 💬 Nhắn tin | Chat 1-1 realtime với bạn bè |
| 🔍 Tìm kiếm | Tìm kiếm người dùng theo tên |
| 👤 Trang cá nhân | Xem và chỉnh sửa hồ sơ cá nhân |
| 🗺️ Bản đồ | Chọn vị trí địa lý khi đăng bài bằng OpenStreetMap |
| 🔔 Thông báo | Thông báo realtime khi có tin nhắn hoặc lời mời kết bạn |
| 🛡️ Privacy Screen | Tự động ẩn nội dung khi cảm biến gần bị che (proximity sensor) |

---

## 🛠️ Công nghệ sử dụng

- **Ngôn ngữ:** Java
- **Min SDK:** 24 (Android 7.0)  
- **Target SDK:** 36

### Thư viện chính

| Thư viện | Phiên bản | Mục đích |
|----------|-----------|----------|
| Firebase BOM | 33.9.0 | Quản lý phiên bản Firebase |
| Firebase Authentication | - | Xác thực người dùng |
| Firebase Firestore | - | Cơ sở dữ liệu NoSQL realtime |
| Firebase Analytics | - | Phân tích người dùng |
| CameraX | 1.3.1 | Camera native |
| Glide | 4.16.0 | Tải và cache ảnh |
| Google Play Services Location | 21.2.0 | Định vị GPS |
| OSMDroid | 6.1.18 | Bản đồ OpenStreetMap |
| Navigation Component | - | Điều hướng Fragment |

---

## 🏗️ Kiến trúc dự án

Dự án theo mô hình **Single Activity – Multi Fragment**, sử dụng `MainActivity` làm host chứa các Fragment.

```
app/src/main/java/com/example/appsocialver2/
│
├── activity/
│   ├── BaseSensorActivity.java       # Base Activity tích hợp cảm biến proximity
│   ├── MainActivity.java             # Host Activity, điều hướng bottom nav
│   ├── DangNhapActivity.java         # Màn hình đăng nhập
│   ├── DangKi.java                   # Màn hình đăng ký
│   ├── ChatActivity.java             # Màn hình chat 1-1
│   ├── ListFriendsChatActivity.java  # Danh sách bạn bè để chat
│   ├── KetBan.java                   # Màn hình quản lý kết bạn
│   ├── MapPickerActivity.java        # Chọn vị trí trên bản đồ
│   └── SinglePostActivity.java       # Xem chi tiết bài đăng
│
├── fragments/
│   ├── HomeFragment.java             # News Feed
│   ├── FriendsFragment.java          # Danh sách bạn bè & lời mời
│   ├── PostFragment.java             # Đăng bài mới (Camera)
│   ├── ProfileFragment.java          # Trang cá nhân
│   ├── ChatListFragment.java         # Danh sách cuộc trò chuyện
│   ├── SearchFragment.java           # Tìm kiếm người dùng
│   └── UserDetailFragment.java       # Xem hồ sơ người khác
│
├── adapters/
│   ├── PostAdapter.java              # Hiển thị bài viết trong RecyclerView
│   ├── MessageAdapter.java           # Hiển thị tin nhắn chat
│   ├── FriendAdapter.java            # Hiển thị danh sách bạn bè
│   ├── FriendChatAdapter.java        # Hiển thị danh sách chat
│   ├── KetBanAdapter.java            # Hiển thị gợi ý kết bạn
│   ├── RequestAdapter.java           # Hiển thị lời mời kết bạn
│   └── GridPostAdapter.java          # Hiển thị ảnh dạng lưới
│
├── Models/
│   ├── User.java                     # Model người dùng
│   ├── Post.java                     # Model bài viết
│   ├── Message.java                  # Model tin nhắn
│   └── RecentChat.java               # Model cuộc trò chuyện gần đây
│
└── utils/
    ├── NotificationHelper.java       # Helper hiển thị thông báo hệ thống
    ├── SplashActivity.java           # Màn hình khởi động
    ├── FirebaseHelper.java           # Helper Firebase (placeholder)
    └── SensorHelper.java             # Helper cảm biến (placeholder)
```

---

## 🗄️ Cấu trúc Firebase Firestore

```
├── users/{userId}
│   ├── tendn         (String)   - Tên hiển thị
│   ├── email         (String)   - Email
│   ├── avatar        (String)   - URL ảnh đại diện
│   └── recent_chats/ (Collection)
│       └── {otherUserId}
│           ├── hasUnread   (Boolean)
│           └── lastMessage (String)
│
├── Posts/{postId}
│   ├── ownerUid      (String)   - ID người đăng
│   ├── imageUrl      (String)   - URL ảnh bài viết
│   ├── description   (String)   - Mô tả bài viết
│   ├── locationName  (String)   - Tên địa điểm
│   ├── likes         (Array)    - Danh sách userId đã thả tim
│   └── timestamp     (Date)     - Thời gian đăng (Server Timestamp)
│
├── friends/{userId}/list/{friendId}
│   └── (document xác nhận là bạn bè)
│
├── friend_requests/{requestId}
│   ├── fromUserId    (String)
│   ├── toUserId      (String)
│   ├── status        (String)   - "pending" | "accepted"
│   └── timestamp     (Long)
│
└── messages/{chatId}/chat/{messageId}
    ├── senderId      (String)
    ├── receiverId    (String)
    ├── text          (String)
    └── timestamp     (Date)
```

---

## ⚙️ Cài đặt & Chạy dự án

### Yêu cầu
- Android Studio **Hedgehog** trở lên
- JDK 11
- Thiết bị / Emulator Android API 24+

### Các bước

1. **Clone repo**
   ```bash
   git clone https://github.com/devkhongluong/appsocialAndroidver2.git
   cd appsocialAndroidver2
   ```

2. **Cấu hình Firebase**
   - Tạo project trên [Firebase Console](https://console.firebase.google.com/)
   - Thêm ứng dụng Android với package name: `com.example.appsocialver2`
   - Tải file `google-services.json` và đặt vào thư mục `app/`
   - Bật **Authentication** (Email/Password)
   - Bật **Firestore Database**

3. **Mở trong Android Studio**
   - File → Open → chọn thư mục dự án
   - Chờ Gradle sync hoàn tất

4. **Chạy ứng dụng**
   - Nhấn **Run ▶** hoặc `Shift + F10`

---

## 📸 Luồng hoạt động

```
Khởi động
    └── SplashActivity
            ├── Chưa đăng nhập → DangNhapActivity → DangKi.java
            └── Đã đăng nhập   → MainActivity
                                    ├── HomeFragment       (News Feed)
                                    ├── FriendsFragment    (Bạn bè)
                                    ├── PostFragment       (Đăng bài)
                                    ├── ChatListFragment   (Tin nhắn)
                                    └── ProfileFragment    (Cá nhân)
```

---

## 🔒 Tính năng Privacy Screen

Khi người dùng đưa điện thoại lên tai (cảm biến gần bị che), màn hình sẽ tự động hiển thị lớp phủ đen để bảo vệ nội dung. Tính năng này được triển khai qua `BaseSensorActivity` và hook `onPrivacyTriggered()`.

---

## 📄 License

Dự án này được phát triển cho mục đích học tập.
