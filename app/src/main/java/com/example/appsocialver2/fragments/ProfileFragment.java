package com.example.appsocialver2.fragments;

import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Bundle;
import android.util.Base64;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.example.appsocialver2.Models.Post;
import com.example.appsocialver2.R;
import com.example.appsocialver2.activity.DangNhapActivity;
import com.example.appsocialver2.adapters.GridPostAdapter;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.ListenerRegistration;
import com.google.firebase.firestore.Query;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Fragment màn hình Trang cá nhân (Profile).
 * Thay thế profile Activity.
 */
public class ProfileFragment extends Fragment {

    private ImageView   imgProfileAvatar;
    private TextView    tvProfileName, tvPostCount, tvFriendCount;
    private ImageButton btnEditAvatar, btnLogout;
    private RecyclerView rvProfileGrid;

    private FirebaseFirestore db;
    private String currentUserId;
    private final List<Post> myPostList = new ArrayList<>();
    private GridPostAdapter gridPostAdapter;
    private final ExecutorService executorService = Executors.newSingleThreadExecutor();

    private ListenerRegistration userInfoListener;
    private ListenerRegistration friendsCountListener;
    private ListenerRegistration postsListener;

    private final ActivityResultLauncher<String> mGetContent =
            registerForActivityResult(new ActivityResultContracts.GetContent(), uri -> {
                if (uri != null) uploadAvatar(uri);
            });

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_profile, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        db            = FirebaseFirestore.getInstance();
        currentUserId = FirebaseAuth.getInstance().getCurrentUser().getUid();

        imgProfileAvatar = view.findViewById(R.id.imgProfileAvatar);
        tvProfileName    = view.findViewById(R.id.tvProfileName);
        tvPostCount      = view.findViewById(R.id.tvPostCount);
        tvFriendCount    = view.findViewById(R.id.tvFriendCount);
        btnEditAvatar    = view.findViewById(R.id.btnEditAvatar);
        btnLogout        = view.findViewById(R.id.btnLogout);
        rvProfileGrid    = view.findViewById(R.id.rvProfileGrid);

        btnEditAvatar.setOnClickListener(v -> mGetContent.launch("image/*"));

        btnLogout.setOnClickListener(v ->
                new android.app.AlertDialog.Builder(requireContext())
                        .setTitle("Đăng xuất")
                        .setMessage("Bạn có chắc chắn muốn đăng xuất không?")
                        .setPositiveButton("Đồng ý", (dialog, which) -> {
                            FirebaseAuth.getInstance().signOut();
                            Intent intent = new Intent(requireContext(), DangNhapActivity.class);
                            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                            startActivity(intent);
                        })
                        .setNegativeButton("Hủy", null)
                        .show());

        gridPostAdapter = new GridPostAdapter(myPostList, requireContext());
        rvProfileGrid.setLayoutManager(new GridLayoutManager(requireContext(), 3));
        rvProfileGrid.setAdapter(gridPostAdapter);

        loadUserInfo();
        loadUserStatsAndPosts();
    }

    private void loadUserInfo() {
        if (userInfoListener != null) userInfoListener.remove();
        userInfoListener = db.collection("users").document(currentUserId)
                .addSnapshotListener((snapshot, error) -> {
                    if (error != null || !isAdded()) return;
                    if (snapshot != null && snapshot.exists()) {
                        tvProfileName.setText(snapshot.getString("tendn"));
                        String avatarStr = snapshot.getString("avatar");
                        if (avatarStr != null && !avatarStr.isEmpty()) {
                            Glide.with(this)
                                    .load(avatarStr)
                                    .placeholder(R.drawable.account)
                                    .circleCrop()
                                    .into(imgProfileAvatar);
                        }
                    }
                });
    }

    private void loadUserStatsAndPosts() {
        if (friendsCountListener != null) friendsCountListener.remove();
        friendsCountListener = db.collection("friends")
                .document(currentUserId).collection("list")
                .addSnapshotListener((value, error) -> {
                    if (error != null || !isAdded()) return;
                    if (value != null) tvFriendCount.setText(String.valueOf(value.size()));
                });

        if (postsListener != null) postsListener.remove();
        postsListener = db.collection("Posts")
                .whereEqualTo("ownerUid", currentUserId)
                .orderBy("timestamp", Query.Direction.DESCENDING)
                .addSnapshotListener((value, error) -> {
                    if (error != null || !isAdded()) return;
                    if (value != null) {
                        myPostList.clear();
                        for (DocumentSnapshot doc : value.getDocuments()) {
                            Post post = doc.toObject(Post.class);
                            if (post != null) {
                                post.setPostId(doc.getId());
                                myPostList.add(post);
                            }
                        }
                        tvPostCount.setText(String.valueOf(myPostList.size()));
                        gridPostAdapter.notifyDataSetChanged();
                    }
                });
    }

    private void uploadAvatar(Uri imageUri) {
        Toast.makeText(requireContext(), "Đang xử lý ảnh...", Toast.LENGTH_SHORT).show();
        executorService.execute(() -> {
            try {
                InputStream imageStream =
                        requireContext().getContentResolver().openInputStream(imageUri);
                Bitmap bmp = BitmapFactory.decodeStream(imageStream);
                int MAX = 500, w = bmp.getWidth(), h = bmp.getHeight();
                if (w > MAX || h > MAX) {
                    float ratio = Math.min((float) MAX / w, (float) MAX / h);
                    bmp = Bitmap.createScaledBitmap(bmp,
                            Math.round(ratio * w), Math.round(ratio * h), false);
                }
                ByteArrayOutputStream baos = new ByteArrayOutputStream();
                bmp.compress(Bitmap.CompressFormat.JPEG, 70, baos);
                String base64 = "data:image/jpeg;base64,"
                        + Base64.encodeToString(baos.toByteArray(), Base64.DEFAULT);

                if (!isAdded()) return;
                requireActivity().runOnUiThread(() ->
                        db.collection("users").document(currentUserId)
                                .update("avatar", base64)
                                .addOnSuccessListener(v ->
                                        Toast.makeText(requireContext(),
                                                "Cập nhật Avatar thành công!", Toast.LENGTH_SHORT).show())
                                .addOnFailureListener(e ->
                                        Toast.makeText(requireContext(),
                                                "Lỗi cập nhật Avatar", Toast.LENGTH_SHORT).show()));
            } catch (Exception e) {
                e.printStackTrace();
                if (isAdded())
                    requireActivity().runOnUiThread(() ->
                            Toast.makeText(requireContext(),
                                    "Lỗi khi xử lý ảnh", Toast.LENGTH_SHORT).show());
            }
        });
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        if (userInfoListener     != null) { userInfoListener.remove();     userInfoListener     = null; }
        if (friendsCountListener != null) { friendsCountListener.remove(); friendsCountListener = null; }
        if (postsListener        != null) { postsListener.remove();        postsListener        = null; }
    }
}
