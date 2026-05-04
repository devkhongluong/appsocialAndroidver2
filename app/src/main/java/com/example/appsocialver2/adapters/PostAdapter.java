package com.example.appsocialver2.adapters;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.EditText;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.example.appsocialver2.Models.Post;
import com.example.appsocialver2.R;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.auth.FirebaseAuth;

import java.util.List;
import java.util.ArrayList;

public class PostAdapter extends RecyclerView.Adapter<PostAdapter.PostViewHolder> {

    private List<Post> postList;
    private Context context;
    private FirebaseFirestore db;

    public PostAdapter(List<Post> postList, Context context) {
        this.postList = postList;
        this.context = context;
        this.db = FirebaseFirestore.getInstance();
    }

    @NonNull
    @Override
    public PostViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_posts, parent, false);
        return new PostViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull PostViewHolder holder, int position) {
        Post post = postList.get(position);
        String location = post.getLocationName();
        //Mặc định
        if (location == null || location.isEmpty()) {
            location = "Hà Nội, Việt Nam";
        }
        holder.tvDescription.setText(post.getDescription());
        holder.tvLocation.setText(post.getLocationName());
        Glide.with(context)
                .load(post.getImageUrl())
                .placeholder(R.drawable.bg_photo)
                .into(holder.imgPost);

        db.collection("users").document(post.getOwnerUid())
                .get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (documentSnapshot.exists()) {
                        String name = documentSnapshot.getString("tendn");
                        String avatar = documentSnapshot.getString("avatar");

                        holder.tvUserName.setText(name);
                        Glide.with(context)
                                .load(avatar)
                                .placeholder(R.drawable.account)
                                .into(holder.imgAvatar);
                    }
                });

        // Xử lý hiển thị Like
        List<String> likes = post.getLikes();
        if (likes == null) {
            likes = new ArrayList<>();
            post.setLikes(likes);
        }
        
        holder.tvLikeCount.setText(String.valueOf(likes.size()));
        
        String currentUserId = FirebaseAuth.getInstance().getCurrentUser().getUid();
        if (likes.contains(currentUserId)) {
            holder.btnLike.setColorFilter(android.graphics.Color.RED);
        } else {
            holder.btnLike.clearColorFilter();
        }

        // Xử lý nút Like/Comment đơn giản
        List<String> finalLikes = likes;
        holder.btnLike.setOnClickListener(v -> {
            if (finalLikes.contains(currentUserId)) {
                finalLikes.remove(currentUserId); // Bỏ like
            } else {
                finalLikes.add(currentUserId); // Thêm like
            }
            db.collection("Posts").document(post.getPostId())
                    .update("likes", finalLikes);
        });

        // Xử lý gửi tin nhắn nhanh
        holder.btnSendQuickMessage.setOnClickListener(v -> {
            String text = holder.edtQuickMessage.getText().toString().trim();
            if (text.isEmpty()) {
                return;
            }

            String receiverId = post.getOwnerUid();
            
            if (currentUserId.equals(receiverId)) {
                Toast.makeText(context, "Bạn không thể tự nhắn cho chính mình", Toast.LENGTH_SHORT).show();
                return;
            }

            String messageId = db.collection("chats").document().getId();
            com.example.appsocialver2.Models.Message message = new com.example.appsocialver2.Models.Message(messageId, currentUserId, receiverId, text);

            db.collection("chats").document(messageId).set(message)
                    .addOnSuccessListener(aVoid -> {
                        holder.edtQuickMessage.setText("");
                        Toast.makeText(context, "Đã gửi tin nhắn cho tác giả", Toast.LENGTH_SHORT).show();
                    })
                    .addOnFailureListener(e -> {
                        Toast.makeText(context, "Lỗi khi gửi tin nhắn", Toast.LENGTH_SHORT).show();
                    });
        });
    }

    @Override
    public int getItemCount() {
        return postList.size();
    }

    public static class PostViewHolder extends RecyclerView.ViewHolder {
        ImageView imgAvatar, imgPost, btnLike, btnSendQuickMessage, btnMore;
        TextView tvUserName, tvLocation, tvLikeCount, tvDescription;
        EditText edtQuickMessage;

        public PostViewHolder(@NonNull View itemView) {
            super(itemView);
            imgAvatar = itemView.findViewById(R.id.imgAvatar);
            imgPost = itemView.findViewById(R.id.imgPost);
            btnLike = itemView.findViewById(R.id.btnLike);
            btnSendQuickMessage = itemView.findViewById(R.id.btnSendQuickMessage);
            btnMore = itemView.findViewById(R.id.btnMore);
            tvUserName = itemView.findViewById(R.id.tvUserName);
            tvLocation = itemView.findViewById(R.id.tvLocation);
            tvLikeCount = itemView.findViewById(R.id.tvLikeCount);
            edtQuickMessage = itemView.findViewById(R.id.edtQuickMessage);
            tvDescription = itemView.findViewById(R.id.tvDescription);
        }
    }
}
