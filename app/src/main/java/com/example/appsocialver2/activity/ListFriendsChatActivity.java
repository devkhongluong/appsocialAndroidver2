package com.example.appsocialver2.activity;

import android.content.Intent;
import android.os.Bundle;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.example.appsocialver2.Models.RecentChat;
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
    private List<RecentChat> chatList = new ArrayList<>();
    private FriendChatAdapter adapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_list_friends_chat);

        rvFriendsChat = findViewById(R.id.rvFriendsChat);
        db = FirebaseFirestore.getInstance();

        rvFriendsChat.setLayoutManager(new LinearLayoutManager(this));
        adapter = new FriendChatAdapter(chatList, chat -> {
            Intent intent = new Intent(ListFriendsChatActivity.this, ChatActivity.class);
            intent.putExtra("userId", chat.getFriendId());
            intent.putExtra("userName", chat.getFriendName());
            intent.putExtra("userAvatar", chat.getFriendAvatar());
            startActivity(intent);
        });
        rvFriendsChat.setAdapter(adapter);

        setupBottomNav();
        loadChats();
    }

    private void setupBottomNav() {
        // Navigation đã chuyển sang MainActivity Fragment host — không còn dùng ở đây
    }

    private void loadChats() {
        String currentUserId = FirebaseAuth.getInstance().getCurrentUser().getUid();

        db.collection("users")
                .document(currentUserId)
                .collection("recent_chats")
                .addSnapshotListener((recentQuery, error1) -> {
                    if (error1 != null) return;
                    
                    db.collection("friends")
                            .document(currentUserId)
                            .collection("list")
                            .get()
                            .addOnSuccessListener(friendsQuery -> {
                                if (isDestroyed() || isFinishing()) return;
                                
                                List<RecentChat> tempChatList = new ArrayList<>();
                                java.util.Set<String> recentFriendIds = new java.util.HashSet<>();
                                
                                if (recentQuery != null) {
                                    for (DocumentSnapshot doc : recentQuery.getDocuments()) {
                                        RecentChat chat = doc.toObject(RecentChat.class);
                                        if (chat != null) {
                                            tempChatList.add(chat);
                                            recentFriendIds.add(chat.getFriendId());
                                        }
                                    }
                                }
                                
                                List<String> friendsToFetch = new ArrayList<>();
                                if (friendsQuery != null) {
                                    for (DocumentSnapshot doc : friendsQuery.getDocuments()) {
                                        String friendId = doc.getId();
                                        if (!recentFriendIds.contains(friendId)) {
                                            friendsToFetch.add(friendId);
                                        }
                                    }
                                }
                                
                                if (friendsToFetch.isEmpty()) {
                                    updateUIWithSortedChats(tempChatList);
                                    return;
                                }
                                
                                int[] pending = {friendsToFetch.size()};
                                for (String friendId : friendsToFetch) {
                                    db.collection("users").document(friendId).get()
                                        .addOnSuccessListener(userDoc -> {
                                            if (userDoc.exists()) {
                                                String name = userDoc.getString("tendn");
                                                String avatar = userDoc.getString("avatar");
                                                RecentChat dummyChat = new RecentChat(friendId, name, avatar, null, false);
                                                tempChatList.add(dummyChat);
                                            }
                                            checkPendingAndSort(tempChatList, pending);
                                        })
                                        .addOnFailureListener(e -> checkPendingAndSort(tempChatList, pending));
                                }
                            });
                });
    }

    private void checkPendingAndSort(List<RecentChat> tempChatList, int[] pending) {
        pending[0]--;
        if (pending[0] == 0 && !isDestroyed() && !isFinishing()) {
            updateUIWithSortedChats(tempChatList);
        }
    }

    private void updateUIWithSortedChats(List<RecentChat> list) {
        java.util.Collections.sort(list, (c1, c2) -> {
            if (c1.getTimestamp() == null && c2.getTimestamp() == null) return 0;
            if (c1.getTimestamp() == null) return 1;
            if (c2.getTimestamp() == null) return -1;
            return c2.getTimestamp().compareTo(c1.getTimestamp());
        });
        
        chatList.clear();
        chatList.addAll(list);
        adapter.notifyDataSetChanged();
    }
}