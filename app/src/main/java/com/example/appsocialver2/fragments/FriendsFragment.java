package com.example.appsocialver2.fragments;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.appsocialver2.Models.User;
import com.example.appsocialver2.R;
import com.example.appsocialver2.activity.KetBan;
import com.example.appsocialver2.adapters.FriendAdapter;
import com.example.appsocialver2.adapters.RequestAdapter;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.ListenerRegistration;

import java.util.ArrayList;
import java.util.List;

/**
 * Fragment màn hình Bạn bè.
 * Thay thế BanBe Activity.
 */
public class FriendsFragment extends Fragment {

    private RecyclerView rcvRequest, listFriends;
    private FirebaseFirestore db;
    private Button btnSearch;

    private final List<User> requestList = new ArrayList<>();
    private final List<User> friendList  = new ArrayList<>();
    private RequestAdapter requestAdapter;
    private FriendAdapter  friendAdapter;

    private ListenerRegistration requestsListener;
    private ListenerRegistration friendsListener;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_friends, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        db          = FirebaseFirestore.getInstance();
        rcvRequest  = view.findViewById(R.id.rcvRequest);
        listFriends = view.findViewById(R.id.listFriends);
        btnSearch   = view.findViewById(R.id.btnSearch);

        rcvRequest.setLayoutManager(new LinearLayoutManager(requireContext()));
        listFriends.setLayoutManager(new LinearLayoutManager(requireContext()));

        requestAdapter = new RequestAdapter(requestList, requireContext());
        friendAdapter  = new FriendAdapter(friendList, requireContext());

        rcvRequest.setAdapter(requestAdapter);
        listFriends.setAdapter(friendAdapter);

        loadRequests();
        loadFriends();

        btnSearch.setOnClickListener(v -> {
            Intent intent = new Intent(requireContext(), KetBan.class);
            startActivity(intent);
        });
    }

    private void loadRequests() {
        String currentUserId = FirebaseAuth.getInstance().getCurrentUser().getUid();
        if (requestsListener != null) requestsListener.remove();

        requestsListener = db.collection("friend_requests")
                .whereEqualTo("toUserId", currentUserId)
                .addSnapshotListener((value, error) -> {
                    if (error != null || !isAdded()) return;
                    requestList.clear();
                    if (value == null || value.isEmpty()) {
                        requestAdapter.notifyDataSetChanged();
                        return;
                    }
                    for (DocumentSnapshot doc : value.getDocuments()) {
                        String fromId = doc.getString("fromUserId");
                        db.collection("users").document(fromId)
                                .get()
                                .addOnSuccessListener(userDoc -> {
                                    if (userDoc.exists() && isAdded()) {
                                        requestList.add(new User(fromId,
                                                userDoc.getString("email"),
                                                userDoc.getString("tendn"),
                                                userDoc.getString("avatar")));
                                        requestAdapter.notifyDataSetChanged();
                                    }
                                });
                    }
                });
    }

    private void loadFriends() {
        String currentUserId = FirebaseAuth.getInstance().getCurrentUser().getUid();
        if (friendsListener != null) friendsListener.remove();

        friendsListener = db.collection("friends")
                .document(currentUserId)
                .collection("list")
                .addSnapshotListener((value, error) -> {
                    if (error != null || !isAdded()) return;
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
                                    if (userDoc.exists() && isAdded()) {
                                        friendList.add(new User(friendId,
                                                userDoc.getString("email"),
                                                userDoc.getString("tendn"),
                                                userDoc.getString("avatar")));
                                        friendAdapter.notifyDataSetChanged();
                                    }
                                });
                    }
                });
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        if (requestsListener != null) { requestsListener.remove(); requestsListener = null; }
        if (friendsListener  != null) { friendsListener.remove();  friendsListener  = null; }
    }
}
