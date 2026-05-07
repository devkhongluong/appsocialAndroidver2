package com.example.appsocialver2.fragments;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.appsocialver2.Models.Post;
import com.example.appsocialver2.R;
import com.example.appsocialver2.adapters.PostAdapter;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.ListenerRegistration;
import com.google.firebase.firestore.Query;

import java.util.ArrayList;
import java.util.List;

/**
 * Fragment hiển thị trang chủ (News Feed).
 * Thay thế toàn bộ logic feed từ MainActivity cũ.
 */
public class HomeFragment extends Fragment {

    private RecyclerView rvPosts;
    private List<Post> postList;
    private FirebaseFirestore db;
    private PostAdapter postAdapter;

    private ListenerRegistration friendsListener;
    private ListenerRegistration postsListener;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_home, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        db = FirebaseFirestore.getInstance();
        rvPosts = view.findViewById(R.id.rvPosts);

        postList = new ArrayList<>();
        postAdapter = new PostAdapter(postList, requireContext());
        rvPosts.setAdapter(postAdapter);
        rvPosts.setLayoutManager(new LinearLayoutManager(requireContext()));

        loadPosts();
    }

    private void loadPosts() {
        String currentUserId = FirebaseAuth.getInstance().getCurrentUser().getUid();

        db.collection("friends")
                .document(currentUserId)
                .collection("list")
                .get()
                .addOnSuccessListener(friendSnapshot -> {
                    if (!isAdded()) return;
                    List<String> friendIds = new ArrayList<>();
                    if (friendSnapshot != null) {
                        for (DocumentSnapshot doc : friendSnapshot.getDocuments()) {
                            friendIds.add(doc.getId());
                        }
                    }
                    listenToPosts(currentUserId, friendIds);
                })
                .addOnFailureListener(e -> {
                    if (!isAdded()) return;
                    listenToPosts(currentUserId, new ArrayList<>());
                });
    }

    private void listenToPosts(String currentUserId, List<String> friendIds) {
        if (postsListener != null) postsListener.remove();

        postsListener = db.collection("Posts")
                .orderBy("timestamp", Query.Direction.DESCENDING)
                .addSnapshotListener((value, error) -> {
                    if (error != null || !isAdded()) {
                        if (isAdded())
                            Toast.makeText(requireContext(), "Lỗi tải bài viết", Toast.LENGTH_SHORT).show();
                        return;
                    }
                    if (value != null) {
                        postList.clear();
                        for (DocumentSnapshot doc : value.getDocuments()) {
                            Post post = doc.toObject(Post.class);
                            if (post != null) {
                                post.setPostId(doc.getId());
                                if (post.getOwnerUid().equals(currentUserId)
                                        || friendIds.contains(post.getOwnerUid())) {
                                    postList.add(post);
                                }
                            }
                        }
                        postAdapter.notifyDataSetChanged();
                    }
                });
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        if (postsListener   != null) { postsListener.remove();   postsListener   = null; }
        if (friendsListener != null) { friendsListener.remove(); friendsListener = null; }
    }
}
