package com.example.appsocialver2.adapters;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.example.appsocialver2.Models.User;
import com.example.appsocialver2.R;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.HashMap;
import java.util.List;

public class KetBanAdapter extends RecyclerView.Adapter<KetBanAdapter.ViewHolder> {
    List<User> list;
    Context context;
    FirebaseFirestore db;
    List<String> friendIds;
    List<String> requestedIds;

    public KetBanAdapter(List<User> list, Context context,
                         List<String> friendIds,
                         List<String> requestedIds) {
        this.list = list;
        this.context = context;
        this.friendIds = friendIds;
        this.requestedIds = requestedIds;
        db = FirebaseFirestore.getInstance();
    }

    public class ViewHolder extends RecyclerView.ViewHolder {
        TextView tvName;
        ImageView imgAvatar;
        Button btnAdd;

        public ViewHolder(View itemView) {
            super(itemView);
            tvName = itemView.findViewById(R.id.tvEmail);
            imgAvatar = itemView.findViewById(R.id.imgAvatar);
            btnAdd = itemView.findViewById(R.id.btnAdd);
        }
    }

    @Override
    public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_search, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(ViewHolder holder, int position) {
        User user = list.get(position);
        String currentUserId = FirebaseAuth.getInstance().getCurrentUser().getUid();

        holder.tvName.setText(user.tendn);

        // Sự kiện Click vào Avatar hoặc Item để xem trang cá nhân
        View.OnClickListener openProfile = v -> {
            if (context instanceof com.example.appsocialver2.activity.MainActivity) {
                ((com.example.appsocialver2.activity.MainActivity) context).showUserDetail(user.userId);
            }
        };

        holder.itemView.setOnClickListener(openProfile);
        holder.imgAvatar.setOnClickListener(openProfile);

        Glide.with(context)
                .load(user.avatar != null && !user.avatar.isEmpty() ? user.avatar : R.drawable.account)
                .placeholder(R.drawable.account)
                .circleCrop()
                .into(holder.imgAvatar);

        holder.btnAdd.setVisibility(View.VISIBLE);
        holder.btnAdd.setEnabled(true);
        holder.btnAdd.setText("Kết bạn");
        if (user.userId.equals(currentUserId)) {
            holder.btnAdd.setVisibility(View.GONE);
            return;
        }
        // Kiểm tra nếu đã là bạn bè (So sánh ID chuỗi dài)
        if (friendIds != null && friendIds.contains(user.userId)) {
            holder.btnAdd.setText("Bạn bè");
            holder.btnAdd.setEnabled(false);

            return;
        }
        // Kiểm tra nếu đã gửi lời mời (Đang chờ - Pending)
        if (requestedIds != null && requestedIds.contains(user.userId)) {
            holder.btnAdd.setText("Đã gửi");
            holder.btnAdd.setEnabled(false);
            return;
        }

        // Mặc định: Có thể kết bạn
        holder.btnAdd.setOnClickListener(v -> {
            guiLoiMoi(user, holder, position, currentUserId);
        });
    }

    private void guiLoiMoi(User user, ViewHolder holder, int position, String currentUserId) {
        holder.btnAdd.setEnabled(false);
        holder.btnAdd.setText("...");

        HashMap<String, Object> map = new HashMap<>();
        map.put("fromUserId", currentUserId);
        map.put("toUserId", user.userId);
        map.put("status", "pending");
        map.put("timestamp", System.currentTimeMillis());

        db.collection("friend_requests")
                .add(map)
                .addOnSuccessListener(doc -> {
                    // Cập nhật list local ngay lập tức để UI đổi chữ "Đã gửi"
                    if (!requestedIds.contains(user.userId)) {
                        requestedIds.add(user.userId);
                    }
                    notifyItemChanged(position);
                    Toast.makeText(context, "Đã gửi lời mời", Toast.LENGTH_SHORT).show();
                })
                .addOnFailureListener(e -> {
                    holder.btnAdd.setEnabled(true);
                    holder.btnAdd.setText("Kết bạn");
                });
    }

    @Override
    public int getItemCount() {
        return list != null ? list.size() : 0;
    }
}