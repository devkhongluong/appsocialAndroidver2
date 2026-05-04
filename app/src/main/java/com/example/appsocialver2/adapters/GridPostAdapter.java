package com.example.appsocialver2.adapters;

import android.content.Context;
import android.content.Intent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.bumptech.glide.Glide;
import com.example.appsocialver2.Models.Post;
import com.example.appsocialver2.R;
import com.example.appsocialver2.activity.SinglePostActivity;

import java.util.List;

public class GridPostAdapter extends RecyclerView.Adapter<GridPostAdapter.GridViewHolder> {

    private List<Post> postList;
    private Context context;

    public GridPostAdapter(List<Post> postList, Context context) {
        this.postList = postList;
        this.context = context;
    }

    @NonNull
    @Override
    public GridViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        // Nạp giao diện item_grid_post.xml
        View view = LayoutInflater.from(context).inflate(R.layout.item_grid_post, parent, false);
        return new GridViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull GridViewHolder holder, int position) {
        Post post = postList.get(position);

        // Dùng Glide để load ảnh từ Base64 hoặc URL
        Glide.with(context)
                .load(post.getImageUrl())
                .placeholder(R.drawable.bg_photo) // ảnh tạm khi đang tải
                .into(holder.imgGridPost);

        // Bắt sự kiện khi user bấm vào ảnh
        holder.itemView.setOnClickListener(v -> {
            // Chuyển sang màn hình SinglePostActivity và gửi kèm postId
            Intent intent = new Intent(context, SinglePostActivity.class);
            intent.putExtra("POST_ID", post.getPostId());
            context.startActivity(intent);
        });
    }

    @Override
    public int getItemCount() {
        return postList.size();
    }

    public static class GridViewHolder extends RecyclerView.ViewHolder {
        ImageView imgGridPost;

        public GridViewHolder(@NonNull View itemView) {
            super(itemView);
            imgGridPost = itemView.findViewById(R.id.imgGridPost);
        }
    }
}
