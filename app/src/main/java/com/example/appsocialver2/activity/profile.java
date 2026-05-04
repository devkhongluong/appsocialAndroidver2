package com.example.appsocialver2.activity;

import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Bundle;
import android.util.Base64;
import android.view.View;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.bumptech.glide.Glide;
import com.example.appsocialver2.Models.Post;
import com.example.appsocialver2.R;
import com.example.appsocialver2.adapters.GridPostAdapter;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class profile extends BaseSensorActivity {


    // 1. Khai báo các View (Giao diện)
    private ImageView imgProfileAvatar;
    private TextView tvProfileName, tvPostCount, tvFriendCount;
    private ImageButton btnEditAvatar, btnLogout;
    private RecyclerView rvProfileGrid;
    // 2. Khai báo Database, Adapter
    private FirebaseFirestore db;
    private String currentUserId;
    private List<Post> myPostList;
    private GridPostAdapter gridPostAdapter;

    // Xử lý background thread (cho việc encode ảnh)
    private final ExecutorService executorService = Executors.newSingleThreadExecutor();
    // 3. Công cụ chọn ảnh từ thư viện
    private final ActivityResultLauncher<String> mGetContent =
            registerForActivityResult(new ActivityResultContracts.GetContent(), uri -> {
                if (uri != null) {
                    // Khi chọn ảnh xong, gọi hàm lưu ảnh lên Firebase
                    uploadAvatar(uri);
                }
            });
    @Override
    protected void onPrivacyTriggered(boolean isCovered) {

    }
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_profile);
        // Khởi tạo các biến
        db = FirebaseFirestore.getInstance();
        currentUserId = FirebaseAuth.getInstance().getCurrentUser().getUid();
        // Ánh xạ View
        initUI();
        // Cài đặt thanh điều hướng ở dưới
        setupNavigation();
        // Cài đặt RecyclerView (dạng lưới 3 cột)
        myPostList = new ArrayList<>();
        gridPostAdapter = new GridPostAdapter(myPostList, this);
        rvProfileGrid.setLayoutManager(new GridLayoutManager(this, 3));
        rvProfileGrid.setAdapter(gridPostAdapter);
        // Bắt đầu gọi dữ liệu từ Firebase
        loadUserInfo();
        loadUserStatsAndPosts();
    }
    private void initUI() {
        imgProfileAvatar = findViewById(R.id.imgProfileAvatar);
        tvProfileName = findViewById(R.id.tvProfileName);
        tvPostCount = findViewById(R.id.tvPostCount);
        tvFriendCount = findViewById(R.id.tvFriendCount);
        btnEditAvatar = findViewById(R.id.btnEditAvatar);
        btnLogout = findViewById(R.id.btnLogout);
        rvProfileGrid = findViewById(R.id.rvProfileGrid);
        // Bấm nút sửa Avatar thì mở thư viện ảnh
        btnEditAvatar.setOnClickListener(v -> mGetContent.launch("image/*"));
        
        // Sự kiện Đăng xuất
        btnLogout.setOnClickListener(v -> {
            new android.app.AlertDialog.Builder(this)
                .setTitle("Đăng xuất")
                .setMessage("Bạn có chắc chắn muốn đăng xuất không?")
                .setPositiveButton("Đồng ý", (dialog, which) -> {
                    FirebaseAuth.getInstance().signOut();
                    Intent intent = new Intent(this, DangNhapActivity.class);
                    intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                    startActivity(intent);
                    finish();
                })
                .setNegativeButton("Hủy", null)
                .show();
        });
    }
    // ── HÀM LOAD THÔNG TIN USER ──
    private void loadUserInfo() {
        db.collection("users").document(currentUserId)
                .addSnapshotListener((documentSnapshot, error) -> {
                    if (error != null) return;
                    if (documentSnapshot != null && documentSnapshot.exists()) {
                        String name = documentSnapshot.getString("tendn");
                        String avatarStr = documentSnapshot.getString("avatar");
                        tvProfileName.setText(name);
                        // Dùng Glide load avatar (có thể là URL hoặc Base64)
                        if (avatarStr != null && !avatarStr.isEmpty()) {
                            Glide.with(this)
                                    .load(avatarStr)
                                    .placeholder(R.drawable.account)
                                    .circleCrop() // làm tròn ảnh avatar
                                    .into(imgProfileAvatar);
                        }
                    }
                });
    }
    // ── HÀM LOAD BẠN BÈ VÀ BÀI VIẾT ──
    private void loadUserStatsAndPosts() {
        // Đếm số bạn bè
        db.collection("friends").document(currentUserId).collection("list")
                .addSnapshotListener((value, error) -> {
                    if (error != null) return;
                    if (value != null) {
                        tvFriendCount.setText(String.valueOf(value.size())); // size() là số lượng bạn bè
                    }
                });
        // Load bài viết của TÔI
        db.collection("Posts")
                .whereEqualTo("ownerUid", currentUserId) // Lọc bài viết có chủ sở hữu là người dùng hiện tại
                .orderBy("timestamp", Query.Direction.DESCENDING) // Xếp mới nhất lên đầu
                .addSnapshotListener((value, error) -> {
                    if (error != null) return;
                    if (value != null) {
                        myPostList.clear();
                        for (DocumentSnapshot doc : value.getDocuments()) {
                            Post post = doc.toObject(Post.class);
                            if (post != null) {
                                post.setPostId(doc.getId());
                                myPostList.add(post);
                            }
                        }
                        // Cập nhật số lượng bài viết
                        tvPostCount.setText(String.valueOf(myPostList.size()));
                        // Báo Adapter vẽ lại giao diện lưới
                        gridPostAdapter.notifyDataSetChanged();
                    }
                });
    }
    // ── HÀM XỬ LÝ ẢNH AVATAR VÀ ĐẨY LÊN FIREBASE ──
    private void uploadAvatar(Uri imageUri) {
        Toast.makeText(this, "Đang xử lý ảnh...", Toast.LENGTH_SHORT).show();
        // Xử lý nén và encode Base64 trên luồng nền (tránh đơ app)
        executorService.execute(() -> {
            try {
                InputStream imageStream = getContentResolver().openInputStream(imageUri);
                Bitmap selectedImage = BitmapFactory.decodeStream(imageStream);
                // Resize ảnh xuống tối đa 500px (vì Avatar không cần lớn, giảm kích thước dung lượng)
                int MAX_DIMENSION = 500;
                int width = selectedImage.getWidth();
                int height = selectedImage.getHeight();
                if (width > MAX_DIMENSION || height > MAX_DIMENSION) {
                    float ratio = Math.min((float) MAX_DIMENSION / width, (float) MAX_DIMENSION / height);
                    width = Math.round((float) ratio * width);
                    height = Math.round((float) ratio * height);
                    selectedImage = Bitmap.createScaledBitmap(selectedImage, width, height, false);
                }
                // Nén (Compress)
                ByteArrayOutputStream baos = new ByteArrayOutputStream();
                selectedImage.compress(Bitmap.CompressFormat.JPEG, 70, baos);
                byte[] imageBytes = baos.toByteArray();

                // Chuyển sang chuỗi Base64
                String base64Image = "data:image/jpeg;base64," + Base64.encodeToString(imageBytes, Base64.DEFAULT);
                // Sau khi có chuỗi base64, cập nhật lên Firestore (chạy lại trên luồng chính UI)
                runOnUiThread(() -> {
                    db.collection("users").document(currentUserId)
                            .update("avatar", base64Image)
                            .addOnSuccessListener(aVoid -> Toast.makeText(profile.this, "Cập nhật Avatar thành công!", Toast.LENGTH_SHORT).show())
                            .addOnFailureListener(e -> Toast.makeText(profile.this, "Lỗi cập nhật Avatar", Toast.LENGTH_SHORT).show());
                });
            } catch (Exception e) {
                e.printStackTrace();
                runOnUiThread(() -> Toast.makeText(this, "Lỗi khi xử lý ảnh", Toast.LENGTH_SHORT).show());
            }
        });
    }
    // Cài đặt thanh điều hướng chuyển Activity
    private void setupNavigation() {
        findViewById(R.id.home).setOnClickListener(v -> startActivity(new Intent(this, MainActivity.class)));
        findViewById(R.id.friend).setOnClickListener(v -> startActivity(new Intent(this, BanBe.class)));
        findViewById(R.id.btnNavCamera).setOnClickListener(v -> startActivity(new Intent(this, PostActivity.class)));
        findViewById(R.id.btnNavChat).setOnClickListener(v -> startActivity(new Intent(this, ListFriendsChatActivity.class)));

    }
}