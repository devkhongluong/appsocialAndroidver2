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
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.HashMap;
import java.util.List;

public class RequestAdapter extends RecyclerView.Adapter<RequestAdapter.ViewHolder>{
    List<User> list;
    Context context;
    FirebaseFirestore db;

    public RequestAdapter(List<User> list, Context context) {
        this.list = list;
        this.context = context;
        db = FirebaseFirestore.getInstance();
    }
    public class ViewHolder extends RecyclerView.ViewHolder {
        TextView tvName;
        ImageView imgAvatar;
        Button btnDongY, btnTuChoi;

        public ViewHolder(View itemView) {
            super(itemView);
            tvName = itemView.findViewById(R.id.tvRequestName);
            btnDongY = itemView.findViewById(R.id.btnDongY);
            btnTuChoi = itemView.findViewById(R.id.btnTuChoi);
            imgAvatar = itemView.findViewById(R.id.imgAvatar);
        }
    }

    @Override
    public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context)
                .inflate(R.layout.item_request, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(ViewHolder holder, int position) {

        User user = list.get(position);
        holder.tvName.setText(user.tendn);

        String currentUserId = FirebaseAuth.getInstance().getCurrentUser().getUid();
        if (user.avatar != null && !user.avatar.isEmpty()) {
            Glide.with(context)
                    .load(user.avatar)
                    .placeholder(R.drawable.account)
                    .error(R.drawable.account)
                    .into(holder.imgAvatar);
        } else {
            holder.imgAvatar.setImageResource(R.drawable.account);
        }
        holder.btnDongY.setOnClickListener(v -> {

            int pos = holder.getAdapterPosition();
            if (pos == RecyclerView.NO_POSITION) return;

            db.collection("friends")
                    .document(currentUserId)
                    .collection("list")
                    .document(user.userId)
                    .set(new HashMap<>());

            db.collection("friends")
                    .document(user.userId)
                    .collection("list")
                    .document(currentUserId)
                    .set(new HashMap<>());

            db.collection("friend_requests")
                    .whereEqualTo("fromUserId", user.userId)
                    .whereEqualTo("toUserId", currentUserId)
                    .get()
                    .addOnSuccessListener(query -> {
                        for (DocumentSnapshot doc : query) {
                            doc.getReference().delete();
                        }

                        list.remove(pos);
                        notifyItemRemoved(pos);
                    });

            Toast.makeText(context, "Đã kết bạn", Toast.LENGTH_SHORT).show();
        });

        holder.btnTuChoi.setOnClickListener(v -> {

            int pos = holder.getAdapterPosition();
            if (pos == RecyclerView.NO_POSITION) return;

            db.collection("friend_requests")
                    .whereEqualTo("fromUserId", user.userId)
                    .whereEqualTo("toUserId", currentUserId)
                    .get()
                    .addOnSuccessListener(query -> {
                        for (DocumentSnapshot doc : query) {
                            doc.getReference().delete();
                        }

                        list.remove(pos);
                        notifyItemRemoved(pos);
                    });

            Toast.makeText(context, "Đã từ chối", Toast.LENGTH_SHORT).show();
        });
    }

    @Override
    public int getItemCount() {
        return list.size();
    }
}
