
package com.example.appsocialver2.activity;

import android.os.Bundle;
import android.util.Log;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.appsocialver2.Models.User;
import com.example.appsocialver2.R;
import com.example.appsocialver2.adapters.KetBanAdapter;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.ArrayList;
import java.util.List;

public class KetBan extends AppCompatActivity {
    EditText etName;
    Button btnFind;
    RecyclerView recyclerView;
    FirebaseFirestore db;

    List<User> userList = new ArrayList<>();
    List<String> friendIds = new ArrayList<>();    // Danh sách ID bạn bè
    List<String> requestedIds = new ArrayList<>(); // Danh sách ID đã gửi lời mời

    KetBanAdapter adapter;
    String currentUid;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_search);

        db = FirebaseFirestore.getInstance();
        currentUid = FirebaseAuth.getInstance().getCurrentUser().getUid();

        etName = findViewById(R.id.etName);
        btnFind = findViewById(R.id.btnFindFriend);
        recyclerView = findViewById(R.id.recyclerFriends);

        recyclerView.setLayoutManager(new LinearLayoutManager(this));

        btnFind.setOnClickListener(v -> {
            String keyword = etName.getText().toString().trim();
            if (!keyword.isEmpty()) {
                loadRelationsAndSearch(keyword);
            } else {
                Toast.makeText(this, "Vui lòng nhập tên", Toast.LENGTH_SHORT).show();
            }
        });

        setupBottomNav();
    }

    private void setupBottomNav() {
        findViewById(R.id.home).setOnClickListener(v -> {
            startActivity(new android.content.Intent(this, MainActivity.class));
            finish();
        });
        findViewById(R.id.friend).setOnClickListener(v -> {
            startActivity(new android.content.Intent(this, BanBe.class));
            finish();
        });
        findViewById(R.id.camera).setOnClickListener(v -> {
            startActivity(new android.content.Intent(this, PostActivity.class));
            finish();
        });
        findViewById(R.id.chat).setOnClickListener(v -> {
            startActivity(new android.content.Intent(this, ListFriendsChatActivity.class));
            finish();
        });
        findViewById(R.id.my).setOnClickListener(v -> {
             startActivity(new android.content.Intent(this, profile.class));
        });
    }

    private void loadRelationsAndSearch(String keyword) {
        // 1. TRUY VẤN SUBCOLLECTION "list" ĐỂ LẤY BẠN BÈ
        // Đường dẫn: friends -> {currentUid} -> list -> {các document bạn bè}
        db.collection("friends")
                .document(currentUid)
                .collection("list")
                .get()
                .addOnSuccessListener(friendDocs -> {
                    friendIds.clear();
                    for (DocumentSnapshot doc : friendDocs) {
                        // Lấy ID của document - chính là mã 8SVog... của người bạn
                        friendIds.add(doc.getId().trim());
                    }

                    Log.d("DEBUG_KB", "Số lượng bạn bè tìm thấy: " + friendIds.size());

                    // 2. Lấy danh sách đã gửi lời mời (friend_requests)
                    db.collection("friend_requests")
                            .whereEqualTo("fromUserId", currentUid)
                            .whereEqualTo("status", "pending")
                            .get()
                            .addOnSuccessListener(reqDocs -> {
                                requestedIds.clear();
                                for (DocumentSnapshot doc : reqDocs) {
                                    String toId = doc.getString("toUserId");
                                    if (toId != null) requestedIds.add(toId.trim());
                                }

                                // 3. Sau khi có đủ 2 danh sách ID, tiến hành tìm User
                                performUserSearch(keyword);
                            });
                })
                .addOnFailureListener(e -> {
                    Log.e("DEBUG_KB", "Lỗi load friends: " + e.getMessage());
                    // Nếu lỗi (có thể do chưa có subcollection 'list'), vẫn cho tìm kiếm
                    performUserSearch(keyword);
                });
    }

    private void performUserSearch(String keyword) {
        db.collection("users")
                .get()
                .addOnSuccessListener(querySnapshot -> {
                    userList.clear();
                    for (DocumentSnapshot doc : querySnapshot) {
                        String tendn = doc.getString("tendn");
                        if (tendn != null && tendn.toLowerCase().contains(keyword.toLowerCase())) {
                            String userId = doc.getId(); // ID dài 8SVog...

                            if (userId.equals(currentUid)) continue;

                            String email = doc.getString("email");
                            String avatar = doc.getString("avatar");
                            userList.add(new User(userId, email, tendn, avatar));
                        }
                    }

                    // Cập nhật Adapter
                    adapter = new KetBanAdapter(userList, this, friendIds, requestedIds);
                    recyclerView.setAdapter(adapter);

                    if (userList.isEmpty()) {
                        Toast.makeText(this, "Không tìm thấy người dùng", Toast.LENGTH_SHORT).show();
                    }
                });
    }
}