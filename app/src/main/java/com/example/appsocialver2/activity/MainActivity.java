package com.example.appsocialver2.activity;

import android.os.Bundle;
import android.view.View;
import android.widget.TextView;

import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentTransaction;

import com.example.appsocialver2.R;
import com.example.appsocialver2.fragments.ChatListFragment;
import com.example.appsocialver2.fragments.FriendsFragment;
import com.example.appsocialver2.fragments.HomeFragment;
import com.example.appsocialver2.fragments.PostFragment;
import com.example.appsocialver2.fragments.ProfileFragment;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QuerySnapshot;

/**
 * MainActivity chỉ còn vai trò là HOST chứa Fragment.
 * Toàn bộ logic (news feed, bạn bè, profile, camera...) đã chuyển sang Fragment tương ứng.
 * Không còn Intent để chuyển qua lại giữa các màn hình chính.
 */
public class MainActivity extends BaseSensorActivity {

    private View privacyOverlay;
    private TextView tvChatBadge;

    // Chỉ số tab hiện tại để tránh swap fragment không cần thiết
    private int currentTabIndex = -1;

    // Tab constants
    private static final int TAB_HOME     = 0;
    private static final int TAB_FRIENDS  = 1;
    private static final int TAB_POST     = 2;
    private static final int TAB_CHAT     = 3;
    private static final int TAB_PROFILE  = 4;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        privacyOverlay = findViewById(R.id.privacyOverlay);
        tvChatBadge = findViewById(R.id.tvChatBadge);

        // Cài đặt Bottom Navigation
        setupBottomNav();
        
        // Lắng nghe thông báo chat
        listenChatBadge();

        // Mở HomeFragment mặc định khi khởi động
        if (savedInstanceState == null) {
            switchTab(TAB_HOME);
        }
    }

    private void listenChatBadge() {
        if (FirebaseAuth.getInstance().getCurrentUser() == null) return;
        String currentUserId = FirebaseAuth.getInstance().getCurrentUser().getUid();
        FirebaseFirestore.getInstance().collection("users")
                .document(currentUserId)
                .collection("recent_chats")
                .whereEqualTo("hasUnread", true)
                .addSnapshotListener((query, error) -> {
                    if (error != null || query == null) return;
                    int unreadCount = query.size();
                    if (unreadCount > 0) {
                        tvChatBadge.setVisibility(View.VISIBLE);
                        tvChatBadge.setText(String.valueOf(unreadCount));
                    } else {
                        tvChatBadge.setVisibility(View.GONE);
                    }
                });
    }

    private void setupBottomNav() {
        findViewById(R.id.navHome).setOnClickListener(v -> switchTab(TAB_HOME));
        findViewById(R.id.navFriends).setOnClickListener(v -> switchTab(TAB_FRIENDS));
        findViewById(R.id.navCamera).setOnClickListener(v -> switchTab(TAB_POST));
        findViewById(R.id.navChat).setOnClickListener(v -> switchTab(TAB_CHAT));
        findViewById(R.id.navProfile).setOnClickListener(v -> switchTab(TAB_PROFILE));
    }

    /**
     * Chuyển sang tab tương ứng bằng FragmentTransaction.
     * Dùng hide/show thay vì replace để tránh recreate Fragment mỗi lần nhấn tab.
     */
    private void switchTab(int tabIndex) {
        if (tabIndex == currentTabIndex) return; // Đang ở tab này rồi → bỏ qua
        currentTabIndex = tabIndex;

        // Highlight tab đang active
        highlightTab(tabIndex);

        Fragment targetFragment = getOrCreateFragment(tabIndex);

        FragmentManager fm = getSupportFragmentManager();
        FragmentTransaction ft = fm.beginTransaction();

        // Ẩn tất cả Fragment hiện tại
        for (Fragment f : fm.getFragments()) {
            ft.hide(f);
        }

        // Nếu Fragment chưa được add → add vào; nếu có rồi → show lên
        if (!targetFragment.isAdded()) {
            ft.add(R.id.fragmentContainer, targetFragment, getTagForTab(tabIndex));
        } else {
            ft.show(targetFragment);
        }

        ft.commit();
    }

    /** Lấy Fragment đã tồn tại hoặc tạo mới */
    private Fragment getOrCreateFragment(int tabIndex) {
        FragmentManager fm = getSupportFragmentManager();
        String tag = getTagForTab(tabIndex);
        Fragment existing = fm.findFragmentByTag(tag);
        if (existing != null) return existing;

        switch (tabIndex) {
            case TAB_HOME:    return new HomeFragment();
            case TAB_FRIENDS: return new FriendsFragment();
            case TAB_POST:    return new PostFragment();
            case TAB_CHAT:    return new ChatListFragment();
            case TAB_PROFILE: return new ProfileFragment();
            default:          return new HomeFragment();
        }
    }

    private String getTagForTab(int tabIndex) {
        switch (tabIndex) {
            case TAB_HOME:    return "home";
            case TAB_FRIENDS: return "friends";
            case TAB_POST:    return "post";
            case TAB_CHAT:    return "chat";
            case TAB_PROFILE: return "profile";
            default:          return "home";
        }
    }

    /** Đổi màu icon tab đang được chọn */
    private void highlightTab(int tabIndex) {
        int[] ids = {
                R.id.imgNavHome,
                R.id.imgNavFriends,
                R.id.imgNavCamera,
                R.id.imgNavChat,
                R.id.imgNavProfile
        };
        for (int i = 0; i < ids.length; i++) {
            View v = findViewById(ids[i]);
            if (v != null) v.setAlpha(i == tabIndex ? 1.0f : 0.4f);
        }
    }

    @Override
    protected void onPrivacyTriggered(boolean isCovered) {
        // Overlay bảo mật khi cảm biến gần bị che
        if (privacyOverlay != null) {
            privacyOverlay.setVisibility(isCovered ? View.VISIBLE : View.GONE);
        }
    }
}