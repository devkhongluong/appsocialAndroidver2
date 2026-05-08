package com.example.appsocialver2.fragments;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.widget.AppCompatButton;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.example.appsocialver2.Models.Post;
import com.example.appsocialver2.R;
import com.example.appsocialver2.activity.ChatActivity;
import com.example.appsocialver2.adapters.GridPostAdapter;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.*;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class UserDetailFragment extends Fragment {

    private ImageView imgAvatarProfile, btnBack;
    private TextView tvProfileName, tvCountPosts, tvCountFriends, tvCountLikes;
    private AppCompatButton btnSingleAction, btnMessage, btnUnfriend;
    private LinearLayout layoutFriendActions;
    private RecyclerView rvMyPosts;

    private FirebaseFirestore db;
    private String currentUserId, targetUserId;

    private List<Post> postList;
    private GridPostAdapter adapter;

    private ListenerRegistration friendStatusListener;
    private ListenerRegistration requestListener;
    private ListenerRegistration userListener;
    private ListenerRegistration postListener;
    private ListenerRegistration friendsCountListener;

    private String requestIdCache = null;
    private String requestFromCache = null;

    public static UserDetailFragment newInstance(String userId) {
        UserDetailFragment f = new UserDetailFragment();
        Bundle b = new Bundle();
        b.putString("userId", userId);
        f.setArguments(b);
        return f;
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {

        View view = inflater.inflate(R.layout.fragment_user_detail, container, false);

        if (getArguments() != null) {
            targetUserId = getArguments().getString("userId");
        }

        currentUserId = FirebaseAuth.getInstance().getCurrentUser().getUid();
        db = FirebaseFirestore.getInstance();

        initUI(view);

        if (targetUserId != null && targetUserId.equals(currentUserId)) {
            btnSingleAction.setVisibility(View.GONE);
            layoutFriendActions.setVisibility(View.GONE);
        } else {
            checkFriendshipStatus();
        }

        loadUserInfo();
        loadUserPosts();

        return view;
    }

    private void initUI(View view) {

        imgAvatarProfile = view.findViewById(R.id.imgAvatarProfile);
        btnBack = view.findViewById(R.id.btnBack);

        tvProfileName = view.findViewById(R.id.tvProfileName);
        tvCountPosts = view.findViewById(R.id.tvCountPosts);
        tvCountFriends = view.findViewById(R.id.tvCountFriends);
        tvCountLikes = view.findViewById(R.id.tvCountLikes);

        btnSingleAction = view.findViewById(R.id.btnSingleAction);
        btnMessage = view.findViewById(R.id.btnMessage);
        btnUnfriend = view.findViewById(R.id.btnUnfriend);

        layoutFriendActions = view.findViewById(R.id.layoutFriendActions);

        rvMyPosts = view.findViewById(R.id.rvMyPosts);
        postList = new ArrayList<>();
        adapter = new GridPostAdapter(postList, getContext());
        rvMyPosts.setLayoutManager(new GridLayoutManager(getContext(), 3));
        rvMyPosts.setAdapter(adapter);

        btnBack.setOnClickListener(v ->
                getActivity().getSupportFragmentManager().popBackStack()
        );

        btnMessage.setOnClickListener(v -> {
            Intent i = new Intent(getContext(), ChatActivity.class);
            i.putExtra("userId", targetUserId);
            i.putExtra("userName", tvProfileName.getText().toString());
            startActivity(i);
        });
    }

    // ================= FRIEND LOGIC (REAL-TIME) =================

    private void checkFriendshipStatus() {
        if (targetUserId == null) return;

        if (friendStatusListener != null) friendStatusListener.remove();

        friendStatusListener = db.collection("friends")
                .document(currentUserId)
                .collection("list")
                .document(targetUserId)
                .addSnapshotListener((doc, e) -> {
                    if (!isAdded() || doc == null) return;
                    if (doc.exists()) {
                        showFriendState();
                        if (requestListener != null) {
                            requestListener.remove();
                            requestListener = null;
                        }
                    } else {
                        listenFriendRequest();
                    }
                });
    }

    private void listenFriendRequest() {
        if (requestListener != null) return;

        requestListener = db.collection("friend_requests")
                .whereIn("fromUserId", Arrays.asList(currentUserId, targetUserId))
                .whereEqualTo("status", "pending")
                .addSnapshotListener((query, e) -> {
                    if (!isAdded() || query == null) return;

                    requestIdCache = null;
                    requestFromCache = null;
                    boolean found = false;

                    for (DocumentSnapshot d : query.getDocuments()) {
                        String from = d.getString("fromUserId");
                        String to = d.getString("toUserId");

                        if ((from.equals(currentUserId) && to.equals(targetUserId)) ||
                                (from.equals(targetUserId) && to.equals(currentUserId))) {

                            found = true;
                            requestIdCache = d.getId();
                            requestFromCache = from;

                            if (from.equals(currentUserId)) {
                                showPendingSentState(requestIdCache);
                            } else {
                                showPendingReceivedState(requestIdCache);
                            }
                            break;
                        }
                    }

                    if (!found) {
                        showNormalState();
                    }
                });
    }

    private void showFriendState() {
        btnSingleAction.setVisibility(View.GONE);
        layoutFriendActions.setVisibility(View.VISIBLE);

        btnMessage.setVisibility(View.VISIBLE);
        btnMessage.setText("Nhắn tin");
        btnMessage.setOnClickListener(v -> {
            Intent i = new Intent(getContext(), ChatActivity.class);
            i.putExtra("userId", targetUserId);
            i.putExtra("userName", tvProfileName.getText().toString());
            startActivity(i);
        });

        btnUnfriend.setVisibility(View.VISIBLE);
        btnUnfriend.setText("Hủy bạn");
        btnUnfriend.setOnClickListener(v -> {
            new android.app.AlertDialog.Builder(getContext())
                    .setTitle("Hủy kết bạn")
                    .setMessage("Bạn chắc chắn muốn hủy kết bạn?")
                    .setPositiveButton("Xác nhận", (d, w) -> {
                        db.collection("friends").document(currentUserId)
                                .collection("list").document(targetUserId).delete();
                        db.collection("friends").document(targetUserId)
                                .collection("list").document(currentUserId).delete();
                        Toast.makeText(getContext(), "Đã hủy kết bạn", Toast.LENGTH_SHORT).show();
                    })
                    .setNegativeButton("Hủy", null)
                    .show();
        });
    }

    private void showPendingSentState(String requestId) {
        btnSingleAction.setVisibility(View.VISIBLE);
        layoutFriendActions.setVisibility(View.GONE);

        btnSingleAction.setText("Hủy yêu cầu");
        btnSingleAction.setEnabled(true);
        btnSingleAction.setAlpha(1.0f);
        btnSingleAction.setOnClickListener(v -> cancelFriendRequest(requestId));
    }

    private void showPendingReceivedState(String requestId) {
        btnSingleAction.setVisibility(View.GONE);
        layoutFriendActions.setVisibility(View.VISIBLE);

        btnMessage.setVisibility(View.VISIBLE);
        btnMessage.setText("Chấp nhận");
        btnMessage.setOnClickListener(v -> acceptFriendRequest(requestId));

        btnUnfriend.setVisibility(View.VISIBLE);
        btnUnfriend.setText("Từ chối");
        btnUnfriend.setOnClickListener(v -> rejectFriendRequest(requestId));
    }

    private void showNormalState() {
        btnSingleAction.setVisibility(View.VISIBLE);
        layoutFriendActions.setVisibility(View.GONE);
        btnUnfriend.setVisibility(View.GONE);

        btnSingleAction.setText("Kết bạn");
        btnSingleAction.setEnabled(true);
        btnSingleAction.setAlpha(1.0f);
        btnSingleAction.setOnClickListener(v -> sendFriendRequest());
    }

    private void sendFriendRequest() {
        java.util.HashMap<String, Object> map = new java.util.HashMap<>();
        map.put("fromUserId", currentUserId);
        map.put("toUserId", targetUserId);
        map.put("status", "pending");
        map.put("timestamp", System.currentTimeMillis());

        db.collection("friend_requests").add(map)
                .addOnSuccessListener(a -> Toast.makeText(getContext(), "Đã gửi lời mời", Toast.LENGTH_SHORT).show());
    }

    private void acceptFriendRequest(String requestId) {
        db.collection("friend_requests").document(requestId)
                .update("status", "accepted")
                .addOnSuccessListener(a -> {
                    addFriend();
                    Toast.makeText(getContext(), "Đã trở thành bạn bè", Toast.LENGTH_SHORT).show();
                });
    }

    private void rejectFriendRequest(String requestId) {
        db.collection("friend_requests").document(requestId).delete()
                .addOnSuccessListener(a -> {
                    Toast.makeText(getContext(), "Đã từ chối lời mời", Toast.LENGTH_SHORT).show();
                });
    }

    private void cancelFriendRequest(String requestId) {
        db.collection("friend_requests").document(requestId).delete()
                .addOnSuccessListener(a -> Toast.makeText(getContext(), "Đã hủy yêu cầu", Toast.LENGTH_SHORT).show());
    }

    private void addFriend() {
        db.collection("friends").document(currentUserId)
                .collection("list").document(targetUserId).set(new java.util.HashMap<>());
        db.collection("friends").document(targetUserId)
                .collection("list").document(currentUserId).set(new java.util.HashMap<>());
    }
    private void loadUserInfo() {
        userListener = db.collection("users")
                .document(targetUserId)
                .addSnapshotListener((doc, e) -> {
                    if (!isAdded() || doc == null) return;
                    tvProfileName.setText(doc.getString("tendn"));
                    String avatar = doc.getString("avatar");
                    if (avatar != null) {
                        Glide.with(imgAvatarProfile.getContext())
                                .load(avatar)
                                .circleCrop()
                                .into(imgAvatarProfile);
                    }
                });

        friendsCountListener = db.collection("friends")
                .document(targetUserId)
                .collection("list")
                .addSnapshotListener((q, e) -> {
                    if (q != null) {
                        tvCountFriends.setText(String.valueOf(q.size()));
                    }
                });
    }
    private void loadUserPosts() {

        postListener = db.collection("Posts")
                .whereEqualTo("ownerUid", targetUserId)
                .orderBy("timestamp", Query.Direction.DESCENDING)
                .addSnapshotListener((value, e) -> {

                    if (!isAdded() || value == null) return;

                    postList.clear();
                    int likes = 0;

                    for (DocumentSnapshot d : value.getDocuments()) {

                        Post p = d.toObject(Post.class);
                        if (p != null) {
                            p.setPostId(d.getId());
                            postList.add(p);

                            if (p.getLikes() != null)
                                likes += p.getLikes().size();
                        }
                    }

                    tvCountPosts.setText(String.valueOf(postList.size()));
                    tvCountLikes.setText(String.valueOf(likes));
                    adapter.notifyDataSetChanged();
                });
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();

        if (friendStatusListener != null) friendStatusListener.remove();
        if (requestListener != null) requestListener.remove();
        if (userListener != null) userListener.remove();
        if (postListener != null) postListener.remove();
        if (friendsCountListener != null) friendsCountListener.remove();
    }

    // helper model
    static class FriendRequest {
        String fromUserId;
        String toUserId;
        String status = "pending";
        long timestamp = System.currentTimeMillis();

        FriendRequest(String from, String to) {
            this.fromUserId = from;
            this.toUserId = to;
        }

        java.util.Map<String, Object> toMap() {
            java.util.HashMap<String, Object> m = new java.util.HashMap<>();
            m.put("fromUserId", fromUserId);
            m.put("toUserId", toUserId);
            m.put("status", status);
            m.put("timestamp", timestamp);
            return m;
        }
    }
}