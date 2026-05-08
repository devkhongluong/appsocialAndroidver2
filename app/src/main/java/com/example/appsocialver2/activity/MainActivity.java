package com.example.appsocialver2.activity;

import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentTransaction;

import com.example.appsocialver2.R;
import com.example.appsocialver2.fragments.ChatListFragment;
import com.example.appsocialver2.fragments.FriendsFragment;
import com.example.appsocialver2.fragments.HomeFragment;
import com.example.appsocialver2.fragments.PostFragment;
import com.example.appsocialver2.fragments.ProfileFragment;
import com.example.appsocialver2.fragments.UserDetailFragment;
import com.example.appsocialver2.utils.NotificationHelper;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentChange;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QuerySnapshot;

/**
 * MainActivity chỉ còn vai trò là HOST chứa Fragment.
 * Toàn bộ logic (news feed, bạn bè, profile, camera...) đã chuyển sang Fragment tương ứng.
 * Không còn Intent để chuyển qua lại giữa các màn hình chính.
 */
public class MainActivity extends BaseSensorActivity {

    private View privacyOverlay;
    private TextView tvChatBadge, tvFriendsBadge;

    // Thời điểm ứng dụng bắt đầu để lọc thông báo mới
    private long appStartTime = System.currentTimeMillis();

    // Chỉ số tab hiện tại để tránh swap fragment không cần thiết
    private int currentTabIndex = -1;

    // Tab constants
    private static final int TAB_HOME     = 0;
    private static final int TAB_FRIENDS  = 1;
    private static final int TAB_POST     = 2;
    private static final int TAB_CHAT     = 3;
    private static final int TAB_PROFILE  = 4;
    public static final int TAB_SEARCH   = 5;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        privacyOverlay = findViewById(R.id.privacyOverlay);
        tvChatBadge = findViewById(R.id.tvChatBadge);
        tvFriendsBadge = findViewById(R.id.tvFriendsBadge);
        // Xin quyền thông báo cho Android 13+
        requestNotificationPermission();
        // Cài đặt Bottom Navigation
        setupBottomNav();
        listenChatBadge();
        listenFriendRequests(); 

        // Mở HomeFragment mặc định khi khởi động
        if (savedInstanceState == null) {
            if (getIntent().hasExtra("OPEN_TAB")) {
                switchTab(TAB_FRIENDS);
            }else {
                switchTab(TAB_HOME);
            }
        }

    }

    private void requestNotificationPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, android.Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(this, new String[]{android.Manifest.permission.POST_NOTIFICATIONS}, 101);
            }
        }
    }

    private void listenFriendRequests() {
        if (FirebaseAuth.getInstance().getCurrentUser() == null) return;
        String currentUserId = FirebaseAuth.getInstance().getCurrentUser().getUid();

        FirebaseFirestore.getInstance()
                .collection("friend_requests")
                .whereEqualTo("toUserId", currentUserId)
                .whereEqualTo("status", "pending")
                .addSnapshotListener((value, error) -> {
                    if (error != null || value == null) return;

                    int requestCount = value.size();
                    if (requestCount > 0) {
                        tvFriendsBadge.setVisibility(View.VISIBLE);
                        tvFriendsBadge.setText(String.valueOf(requestCount));
                    } else {
                        tvFriendsBadge.setVisibility(View.GONE);
                    }

                    for (DocumentChange dc : value.getDocumentChanges()) {
                        if (dc.getType() == DocumentChange.Type.ADDED) {
                            Long timestamp = dc.getDocument().getLong("timestamp");
                            if (timestamp != null && timestamp > appStartTime) {
                                String fromUserId = dc.getDocument().getString("fromUserId");
                                loadUserAndShowNotification(fromUserId);
                            }
                        }
                    }
                });
    }

    private void loadUserAndShowNotification(String fromUserId) {
        FirebaseFirestore.getInstance().collection("users").document(fromUserId)
                .get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (documentSnapshot.exists()) {
                        String name = documentSnapshot.getString("tendn");
                        NotificationHelper.showNotification(this, "Lời mời kết bạn mới",
                                name + " vừa gửi cho bạn một lời mời kết bạn.");
                    }
                });
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
        if (tabIndex == currentTabIndex) {
            // Nếu bấm lại vào tab đang đứng, ta pop hết backstack để về gốc của tab đó
            getSupportFragmentManager().popBackStack(null, FragmentManager.POP_BACK_STACK_INCLUSIVE);
            return;
        }
        currentTabIndex = tabIndex;
        getSupportFragmentManager().popBackStack(null, FragmentManager.POP_BACK_STACK_INCLUSIVE);

        highlightTab(tabIndex);

        Fragment targetFragment = getOrCreateFragment(tabIndex);
        FragmentManager fm = getSupportFragmentManager();
        FragmentTransaction ft = fm.beginTransaction();

        // Ẩn tất cả Fragment hiện tại đang có trong Manager
        for (Fragment f : fm.getFragments()) {
            ft.hide(f);
        }

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
            case TAB_SEARCH:  return new com.example.appsocialver2.fragments.SearchFragment();
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
            case TAB_SEARCH:  return "search";
            default:          return "home";
        }
    }

    public void switchTabPublic(int tabIndex) {
        switchTab(tabIndex);
    }
    public void showUserDetail(String userId) {
        if (userId == null) return;
        FragmentManager fm = getSupportFragmentManager();
        // Tìm Fragment hiện tại đang hiển thị
        Fragment currentFragment = null;
        for (Fragment f : fm.getFragments()) {
            if (f.isVisible()) {
                currentFragment = f;
                break;
            }
        }
        // Nếu đã đang ở chính trang đó thì không làm gì cả
        if (currentFragment instanceof UserDetailFragment) {
            Bundle args = currentFragment.getArguments();
            if (args != null && userId.equals(args.getString("userId"))) {
                return;
            }
        }
        UserDetailFragment detailFragment = UserDetailFragment.newInstance(userId);
        FragmentTransaction ft = fm.beginTransaction();
        // Ẩn fragment hiện tại đi thay vì replace (để giữ trạng thái)
        if (currentFragment != null) {
            ft.hide(currentFragment);
        }
        // Thêm UserDetail lên trên
        ft.add(R.id.fragmentContainer, detailFragment, "user_detail_" + userId);
        ft.addToBackStack("user_detail");
        ft.commit();
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
    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        if (intent != null && intent.hasExtra("OPEN_TAB")) {
            if ("FRIENDS".equals(intent.getStringExtra("OPEN_TAB"))) {
                switchTab(TAB_FRIENDS);
            }
        }
    }
}