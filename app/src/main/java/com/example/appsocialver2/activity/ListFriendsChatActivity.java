package com.example.appsocialver2.activity;

import android.content.Intent;
import android.os.Bundle;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.example.appsocialver2.Models.User;
import com.example.appsocialver2.R;
import com.example.appsocialver2.adapters.FriendChatAdapter;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import java.util.ArrayList;
import java.util.List;

public class ListFriendsChatActivity extends AppCompatActivity {
    private RecyclerView rvFriendsChat;
    private FirebaseFirestore db;
    private List<User> friendList = new ArrayList<>();
    private FriendChatAdapter adapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_list_friends_chat);

        rvFriendsChat = findViewById(R.id.rvFriendsChat);
        db = FirebaseFirestore.getInstance();

        rvFriendsChat.setLayoutManager(new LinearLayoutManager(this));
        adapter = new FriendChatAdapter(friendList, user -> {
            Intent intent = new Intent(ListFriendsChatActivity.this, ChatActivity.class);
            intent.putExtra("userId", user.userId);
            intent.putExtra("userName", user.tendn);
            intent.putExtra("userAvatar", user.avatar);
            startActivity(intent);
        });
        rvFriendsChat.setAdapter(adapter);

        setupBottomNav();
        loadFriends();
    }

    private void setupBottomNav() {
        findViewById(R.id.home).setOnClickListener(v -> {
            startActivity(new Intent(this, MainActivity.class));
            finish();
        });
        findViewById(R.id.friend).setOnClickListener(v -> {
            startActivity(new Intent(this, BanBe.class));
            finish();
        });
        findViewById(R.id.btnNavCamera).setOnClickListener(v -> {
            startActivity(new Intent(this, PostActivity.class));
            finish();
        });
        findViewById(R.id.btnNavChat).setOnClickListener(v -> {
            // Already here
        });
        findViewById(R.id.btnNavProfile).setOnClickListener(v -> {
             startActivity(new Intent(this, profile.class));
        });
    }

    private void loadFriends() {
        String currentUserId = FirebaseAuth.getInstance().getCurrentUser().getUid();
        db.collection("friends")
                .document(currentUserId)
                .collection("list")
                .get()
                .addOnSuccessListener(query -> {
                    friendList.clear();
                    for (DocumentSnapshot doc : query) {
                        String friendId = doc.getId();
                        db.collection("users").document(friendId)
                                .get()
                                .addOnSuccessListener(userDoc -> {
                                    if (userDoc.exists()) {
                                        String email = userDoc.getString("email");
                                        String tendn = userDoc.getString("tendn");
                                        String avatar = userDoc.getString("avatar");
                                        friendList.add(new User(friendId, email, tendn, avatar));
                                        adapter.notifyDataSetChanged();
                                    }
                                });
                    }
                });
    }
}