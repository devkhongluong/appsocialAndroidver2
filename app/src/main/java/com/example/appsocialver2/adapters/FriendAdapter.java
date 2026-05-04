package com.example.appsocialver2.adapters;

import android.content.Context;
import android.content.Intent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.example.appsocialver2.Models.User;
import com.example.appsocialver2.R;

import java.util.List;

public class FriendAdapter extends RecyclerView.Adapter<FriendAdapter.ViewHolder> {

    List<User> list;
    Context context;

    public FriendAdapter(List<User> list, Context context) {
        this.list = list;
        this.context = context;
    }

    public class ViewHolder extends RecyclerView.ViewHolder {
        TextView tvName;
        ImageView imgAvatar;
        Button btnNhanTin;

        public ViewHolder(View itemView) {
            super(itemView);

            tvName = itemView.findViewById(R.id.tvName);
            btnNhanTin = itemView.findViewById(R.id.btnNhanTin);
            imgAvatar = itemView.findViewById(R.id.imgAvatar);
        }
    }

    @Override
    public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context)
                .inflate(R.layout.item_banbe, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(ViewHolder holder, int position) {

        User user = list.get(position);

        holder.tvName.setText(user.tendn);
        // lấy ảnh từ firebase
        if (user.avatar != null && !user.avatar.isEmpty()) {
            Glide.with(context)
                    .load(user.avatar)
                    .placeholder(R.drawable.account)
                    .error(R.drawable.account)
                    .into(holder.imgAvatar);
        } else {
            holder.imgAvatar.setImageResource(R.drawable.account);
        }

        holder.btnNhanTin.setOnClickListener(v -> {
            Intent intent = new Intent(context, com.example.appsocialver2.activity.ChatActivity.class);
            intent.putExtra("userId", user.userId);
            intent.putExtra("userName", user.tendn);
            intent.putExtra("userAvatar", user.avatar);
            context.startActivity(intent);
        });
    }

    @Override
    public int getItemCount() {
        return list.size();
    }
}
