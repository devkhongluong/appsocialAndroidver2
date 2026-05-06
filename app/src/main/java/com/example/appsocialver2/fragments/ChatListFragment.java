package com.example.appsocialver2.fragments;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.appsocialver2.Models.User;
import com.example.appsocialver2.R;
import com.example.appsocialver2.activity.ChatActivity;
import com.example.appsocialver2.adapters.FriendChatAdapter;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.ArrayList;
import java.util.List;

/**
 * Fragment màn hình danh sách bạn bè để nhắn tin.
 * Thay thế ListFriendsChatActivity.
 */
public class ChatListFragment extends Fragment {

    private RecyclerView rvFriendsChat;
    private FirebaseFirestore db;
    private final List<User> friendList = new ArrayList<>();
    private FriendChatAdapter adapter;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_chat_list, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        db = FirebaseFirestore.getInstance();
        rvFriendsChat = view.findViewById(R.id.rvFriendsChat);
        rvFriendsChat.setLayoutManager(new LinearLayoutManager(requireContext()));

        // Click vào người bạn → mở ChatActivity (giữ Intent — màn hình detail)
        adapter = new FriendChatAdapter(friendList, user -> {
            Intent intent = new Intent(requireContext(), ChatActivity.class);
            intent.putExtra("userId",     user.userId);
            intent.putExtra("userName",   user.tendn);
            intent.putExtra("userAvatar", user.avatar);
            startActivity(intent);
        });
        rvFriendsChat.setAdapter(adapter);

        loadFriends();
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
                                    if (userDoc.exists() && isAdded()) {
                                        friendList.add(new User(friendId,
                                                userDoc.getString("email"),
                                                userDoc.getString("tendn"),
                                                userDoc.getString("avatar")));
                                        adapter.notifyDataSetChanged();
                                    }
                                });
                    }
                });
    }
}
