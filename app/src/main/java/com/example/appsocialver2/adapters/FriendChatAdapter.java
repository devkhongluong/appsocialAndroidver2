package com.example.appsocialver2.adapters;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.bumptech.glide.Glide;
import com.example.appsocialver2.Models.User;
import com.example.appsocialver2.R;
import java.util.List;

public class FriendChatAdapter extends RecyclerView.Adapter<FriendChatAdapter.ViewHolder> {
    private List<User> friendList;
    private OnFriendClickListener listener;

    public interface OnFriendClickListener {
        void onFriendClick(User user);
    }

    public FriendChatAdapter(List<User> friendList, OnFriendClickListener listener) {
        this.friendList = friendList;
        this.listener = listener;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_friend_chat, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        User user = friendList.get(position);
        holder.tvFriendName.setText(user.tendn);
        if (user.avatar != null && !user.avatar.isEmpty()) {
            Glide.with(holder.itemView.getContext()).load(user.avatar).into(holder.imgAvatar);
        } else {
            holder.imgAvatar.setImageResource(R.drawable.account);
        }

        holder.itemView.setOnClickListener(v -> listener.onFriendClick(user));
    }

    @Override
    public int getItemCount() {
        return friendList.size();
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        ImageView imgAvatar;
        TextView tvFriendName, tvLastMessage;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            imgAvatar = itemView.findViewById(R.id.imgAvatar);
            tvFriendName = itemView.findViewById(R.id.tvFriendName);
            tvLastMessage = itemView.findViewById(R.id.tvLastMessage);
        }
    }
}