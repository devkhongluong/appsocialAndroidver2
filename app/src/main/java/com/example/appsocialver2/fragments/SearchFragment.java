package com.example.appsocialver2.fragments;

import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.appsocialver2.Models.User;
import com.example.appsocialver2.R;
import com.example.appsocialver2.adapters.KetBanAdapter;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.ArrayList;
import java.util.List;

public class SearchFragment extends Fragment {

    private EditText etName;
    private Button btnFind;
    private RecyclerView recyclerView;
    private FirebaseFirestore db;

    private List<User> userList = new ArrayList<>();
    private List<String> friendIds = new ArrayList<>();
    private List<String> requestedIds = new ArrayList<>();

    private KetBanAdapter adapter;
    private String currentUid;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_search, container, false);

        db = FirebaseFirestore.getInstance();
        currentUid = FirebaseAuth.getInstance().getCurrentUser().getUid();

        etName = view.findViewById(R.id.etName);
        btnFind = view.findViewById(R.id.btnFindFriend);
        recyclerView = view.findViewById(R.id.recyclerFriends);

        recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));

        btnFind.setOnClickListener(v -> {
            String keyword = etName.getText().toString().trim();
            if (!keyword.isEmpty()) {
                loadRelationsAndSearch(keyword);
            } else {
                Toast.makeText(getContext(), "Vui lòng nhập tên", Toast.LENGTH_SHORT).show();
            }
        });

        return view;
    }

    private void loadRelationsAndSearch(String keyword) {
        db.collection("friends")
                .document(currentUid)
                .collection("list")
                .get()
                .addOnSuccessListener(friendDocs -> {
                    friendIds.clear();
                    for (DocumentSnapshot doc : friendDocs) {
                        friendIds.add(doc.getId().trim());
                    }

                    db.collection("friend_requests")
                            .whereEqualTo("fromUserId", currentUid)
                            .whereEqualTo("status", "pending")
                            .get()
                            .addOnSuccessListener(reqDocs -> {
                                requestedIds.clear();
                                for (DocumentSnapshot doc : reqDocs) {
                                    String toId = doc.getString("toUserId");
                                    if (toId != null) requestedIds.add(toId.trim());
                                }

                                performUserSearch(keyword);
                            });
                })
                .addOnFailureListener(e -> {
                    performUserSearch(keyword);
                });
    }

    private void performUserSearch(String keyword) {
        db.collection("users")
                .get()
                .addOnSuccessListener(querySnapshot -> {
                    userList.clear();
                    for (DocumentSnapshot doc : querySnapshot) {
                        String tendn = doc.getString("tendn");
                        if (tendn != null && tendn.toLowerCase().contains(keyword.toLowerCase())) {
                            String userId = doc.getId();

                            if (userId.equals(currentUid)) continue;

                            String email = doc.getString("email");
                            String avatar = doc.getString("avatar");
                            userList.add(new User(userId, email, tendn, avatar));
                        }
                    }

                    adapter = new KetBanAdapter(userList, getContext(), friendIds, requestedIds);
                    recyclerView.setAdapter(adapter);

                    if (userList.isEmpty()) {
                        Toast.makeText(getContext(), "Không tìm thấy người dùng", Toast.LENGTH_SHORT).show();
                    }
                });
    }
}