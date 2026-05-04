package com.example.appsocialver2.activity;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Toast;

import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.appsocialver2.Models.Post;
import com.example.appsocialver2.R;
import com.example.appsocialver2.adapters.PostAdapter;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;
import com.google.firebase.auth.FirebaseAuth;
import java.util.ArrayList;
import java.util.List;

public class MainActivity extends BaseSensorActivity {

    private RecyclerView rvPosts;
    private List<Post> postList;
    private FirebaseFirestore db;
    private PostAdapter postAdapter;
    private View privacyOverlay;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // Khởi tạo UI và Firebase
        db = FirebaseFirestore.getInstance();
        rvPosts = findViewById(R.id.rvPosts);
        privacyOverlay = findViewById(R.id.privacyOverlay);

        postList = new ArrayList<>();
        postAdapter = new PostAdapter(postList, this);
        rvPosts.setAdapter(postAdapter);
        rvPosts.setLayoutManager(new LinearLayoutManager(this));

        // Load bài viết từ Firestore theo thời gian thực
        loadPosts();

        // Nút Camera điều hướng sang PostActivity
        findViewById(R.id.btnNavCamera).setOnClickListener(v -> {
            startActivity(new Intent(MainActivity.this, PostActivity.class));
        });
        findViewById(R.id.home).setOnClickListener(v -> {
            startActivity(new Intent(MainActivity.this, MainActivity.class));
        });
        findViewById(R.id.friend).setOnClickListener(v -> {
            startActivity(new Intent(MainActivity.this, BanBe.class));
        });
        findViewById(R.id.btnNavChat).setOnClickListener(v -> {
            startActivity(new Intent(MainActivity.this, ListFriendsChatActivity.class));
        });
        findViewById(R.id.btnNavProfile).setOnClickListener(v -> {
            startActivity(new Intent(MainActivity.this, profile.class));
        });
    }

    private void loadPosts() {
        String currentUserId = FirebaseAuth.getInstance().getCurrentUser().getUid();

        // Lấy danh sách bạn bè trước
        db.collection("friends")
                .document(currentUserId)
                .collection("list")
                .addSnapshotListener((friendValue, friendError) -> {
                    if (friendError != null) return;
                    
                    List<String> friendIds = new ArrayList<>();
                    if (friendValue != null) {
                        for (DocumentSnapshot doc : friendValue.getDocuments()) {
                            friendIds.add(doc.getId());
                        }
                    }
                    
                    // Sau khi có danh sách bạn bè, bắt đầu lấy bài viết
                    db.collection("Posts")
                            .orderBy("timestamp", Query.Direction.DESCENDING)
                            .addSnapshotListener((value, error) -> {
                                if (error != null) {
                                    Toast.makeText(this, "Lỗi tải bài viết", Toast.LENGTH_SHORT).show();
                                    return;
                                }
                                if (value != null) {
                                    postList.clear();
                                    for (DocumentSnapshot doc : value.getDocuments()) {
                                        Post post = doc.toObject(Post.class);
                                        if (post != null) {
                                            post.setPostId(doc.getId());
                                            
                                            // Chỉ thêm bài viết của mình hoặc của người trong danh sách bạn bè
                                            if (post.getOwnerUid().equals(currentUserId) || friendIds.contains(post.getOwnerUid())) {
                                                postList.add(post);
                                            }
                                        }
                                    }
                                    postAdapter.notifyDataSetChanged();
                                }
                            });
                });
    }

    @Override
    protected void onPrivacyTriggered(boolean isCovered) {
        if (isCovered) {
            privacyOverlay.setVisibility(View.VISIBLE); // Phủ màn hình đen [cite: 91, 151]
            rvPosts.setAlpha(0.0f); // Ẩn hoàn toàn nội dung bài viết
        } else {
            privacyOverlay.setVisibility(View.GONE);
            rvPosts.setAlpha(1.0f);
        }
    }
}