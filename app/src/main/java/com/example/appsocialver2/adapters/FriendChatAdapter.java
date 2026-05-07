package com.example.appsocialver2.adapters;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import android.graphics.Typeface;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.bumptech.glide.Glide;
import com.example.appsocialver2.Models.RecentChat;
import com.example.appsocialver2.R;
import java.util.List;

public class FriendChatAdapter extends RecyclerView.Adapter<FriendChatAdapter.ViewHolder> {
    private List<RecentChat> chatList;
    private OnChatClickListener listener;

    public interface OnChatClickListener {
        void onChatClick(RecentChat chat);
    }

    public FriendChatAdapter(List<RecentChat> chatList, OnChatClickListener listener) {
        this.chatList = chatList;
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
        RecentChat chat = chatList.get(position);
        holder.tvFriendName.setText(chat.getFriendName());
        if (chat.getFriendAvatar() != null && !chat.getFriendAvatar().isEmpty()) {
            Glide.with(holder.itemView.getContext()).load(chat.getFriendAvatar()).into(holder.imgAvatar);
        } else {
            holder.imgAvatar.setImageResource(R.drawable.account);
        }

        if (chat.getLastMessage() != null) {
            holder.tvLastMessage.setText(chat.getLastMessage());
        } else {
            holder.tvLastMessage.setText("Bắt đầu trò chuyện...");
        }

        if (chat.isHasUnread()) {
            holder.tvLastMessage.setTypeface(null, Typeface.BOLD);
            holder.tvLastMessage.setTextColor(holder.itemView.getContext().getResources().getColor(R.color.black));
        } else {
            holder.tvLastMessage.setTypeface(null, Typeface.NORMAL);
            holder.tvLastMessage.setTextColor(holder.itemView.getContext().getResources().getColor(R.color.grey));
        }

        holder.itemView.setOnClickListener(v -> listener.onChatClick(chat));
    }

    @Override
    public int getItemCount() {
        return chatList.size();
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