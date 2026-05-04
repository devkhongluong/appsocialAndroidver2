package com.example.appsocialver2.activity;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import android.widget.LinearLayout;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.appsocialver2.Models.User;
import com.example.appsocialver2.R;
import com.example.appsocialver2.adapters.FriendAdapter;
import com.example.appsocialver2.adapters.RequestAdapter;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.ArrayList;
import java.util.List;

public class BanBe extends AppCompatActivity {
    RecyclerView rcvRequest, listFriends;
    FirebaseFirestore db;
    LinearLayout home, friend, camera, chat, my;
    Button btnSearch;
    List<User> requestList = new ArrayList<>();
    List<User> friendList = new ArrayList<>();

    RequestAdapter requestAdapter;
    FriendAdapter friendAdapter;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_banbe);

        home = findViewById(R.id.home);
        friend = findViewById(R.id.friend);
        camera = findViewById(R.id.camera);
        chat = findViewById(R.id.chat);
        my = findViewById(R.id.my);
        rcvRequest = findViewById(R.id.rcvRequest);
        listFriends = findViewById(R.id.listFriends);
        db = FirebaseFirestore.getInstance();
        btnSearch = findViewById(R.id.btnSearch);
        rcvRequest.setLayoutManager(new LinearLayoutManager(this));
        listFriends.setLayoutManager(new LinearLayoutManager(this));

        requestAdapter = new RequestAdapter(requestList, this);
        friendAdapter = new FriendAdapter(friendList, this);

        rcvRequest.setAdapter(requestAdapter);
        listFriends.setAdapter(friendAdapter);

        loadRequests();
        loadFriends();

        btnSearch.setOnClickListener(v -> {
            Intent intent = new Intent(this, KetBan.class);
            startActivity(intent);
        });
        home.setOnClickListener(v -> {
            Intent intent = new Intent(this, MainActivity.class);
            startActivity(intent);
        });
        camera.setOnClickListener(v -> {
            Intent intent = new Intent(this, PostActivity.class);
            startActivity(intent);
        });
        chat.setOnClickListener(v -> {
            Intent intent = new Intent(this, ListFriendsChatActivity.class);
            startActivity(intent);
        });
        my.setOnClickListener(v -> {
             Intent intent = new Intent(this, profile.class);
             startActivity(intent);
        });
    }
    private void loadRequests() {
        String currentUserId = FirebaseAuth.getInstance().getCurrentUser().getUid();

        db.collection("friend_requests")
                .whereEqualTo("toUserId", currentUserId)
                .addSnapshotListener((value, error) -> {
                    if (error != null) {
                        return;
                    }

                    requestList.clear();

                    // Nếu không có yêu cầu kết bạn nào
                    if (value == null || value.isEmpty()) {
                        requestAdapter.notifyDataSetChanged();
                        return;
                    }

                    // Duyệt qua danh sách các yêu cầu
                    for (DocumentSnapshot doc : value.getDocuments()) {
                        String fromId = doc.getString("fromUserId");

                        // Lấy thông tin chi tiết của người gửi từ collection "users"
                        db.collection("users").document(fromId)
                                .get()
                                .addOnSuccessListener(userDoc -> {
                                    if (userDoc.exists()) {
                                        String email = userDoc.getString("email");
                                        String tendn = userDoc.getString("tendn");
                                        String avatar = userDoc.getString("avatar");

                                        requestList.add(new User(fromId, email, tendn, avatar));
                                        requestAdapter.notifyDataSetChanged();
                                    }
                                });
                    }
                });
    }
    private void loadFriends() {

        String currentUserId = FirebaseAuth.getInstance().getCurrentUser().getUid();
        db.collection("friends")
                .document(currentUserId)
                .collection("list")
                .addSnapshotListener((value, error)  -> {
                    if (error != null) return;
                    friendList.clear();
                    if (value == null || value.isEmpty()) {
                        friendAdapter.notifyDataSetChanged();
                        return;
                    }
                    for (DocumentSnapshot doc : value.getDocuments()) {
                        String friendId = doc.getId();

                        db.collection("users").document(friendId)
                                .get()
                                .addOnSuccessListener(userDoc -> {
                                    if (userDoc.exists()) {

                                        String email = userDoc.getString("email");
                                        String tendn = userDoc.getString("tendn");
                                        String avatar = userDoc.getString("avatar");
                                        friendList.add(new User(friendId, email, tendn, avatar));
                                        friendAdapter.notifyDataSetChanged();
                                    }
                                });
                    }
                });
    }
}
