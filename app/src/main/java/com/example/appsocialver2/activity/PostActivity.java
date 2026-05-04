package com.example.appsocialver2.activity;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorManager;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import android.view.WindowManager;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.camera.core.CameraSelector;
import androidx.camera.core.ImageCapture;
import androidx.camera.core.ImageCaptureException;
import androidx.camera.core.Preview;
import androidx.camera.core.resolutionselector.ResolutionSelector;
import androidx.camera.core.resolutionselector.ResolutionStrategy;
import androidx.camera.lifecycle.ProcessCameraProvider;
import androidx.camera.view.PreviewView;
import android.util.Size;
import androidx.core.content.ContextCompat;

import com.example.appsocialver2.R;
import com.example.appsocialver2.Models.Post;
import com.google.android.material.floatingactionbutton.ExtendedFloatingActionButton;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;

import java.io.File;
import java.util.concurrent.ExecutionException;
// thư viện dùng location
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.util.Base64;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;

public class PostActivity extends BaseSensorActivity {

    private FusedLocationProviderClient fusedLocationClient;
    private final ExecutorService executorService = Executors.newSingleThreadExecutor();
    // UI
    private PreviewView viewFinder;
    private ImageView imgDemo;
    private EditText editDescription;
    private TextView txtLocation, txtLightWarning;
    private View privacyOverlay, overlayHeader;
    private ImageButton btnCapture, btnGallery, btnCancel, btnPickLocation, btnSwitchCamera;
    private ExtendedFloatingActionButton btnPost;

    // Camera
    private ImageCapture imageCapture;

    // State
    private Uri finalImageUri;
    private boolean isFromGallery = false;
    private Sensor lightSensor;
    private int lensFacing = CameraSelector.LENS_FACING_BACK;

    // ── Runtime Permission ──────────────────────────────────────────────────
    private final ActivityResultLauncher<String[]> permissionLauncher =
            registerForActivityResult(new ActivityResultContracts.RequestMultiplePermissions(), result -> {
                // Kiểm tra xem quyền Camera có được cấp không
                Boolean cameraGranted = result.getOrDefault(Manifest.permission.CAMERA, false);
                if (cameraGranted != null && cameraGranted) {
                    startCamera();
                } else {
                    Toast.makeText(this, "Cần quyền Camera để sử dụng tính năng này", Toast.LENGTH_LONG).show();
                    finish();
                }
                // Quyền Vị trí: Nếu bị từ chối thì lát nữa hàm fetchLocation() tự báo "Không có quyền",
                // không ép người dùng văng ra ngoài.
            });

    // ── Gallery Picker ──────────────────────────────────────────────────────
    private final ActivityResultLauncher<String> mGetContent =
            registerForActivityResult(new ActivityResultContracts.GetContent(), uri -> {
                if (uri != null) {
                    isFromGallery = true;
                    showPreview(uri);
                }
            });

    private final ActivityResultLauncher<String> mediaPermissionLauncher =
            registerForActivityResult(new ActivityResultContracts.RequestPermission(), granted -> {
                if (granted) {
                    mGetContent.launch("image/*");
                } else {
                    Toast.makeText(this, "Cần quyền truy cập thư viện ảnh", Toast.LENGTH_SHORT).show();
                }
            });

    // ───────────────────────────────────────────────────────────────────────
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView((int) R.layout.activity_post);
        initUI();
        setupNavigation();
        setupButtons();
        // Khởi tạo FusedLocationProviderClient
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this);
        lightSensor = sensorManager.getDefaultSensor(Sensor.TYPE_LIGHT);
        // Chuẩn bị danh sách quyền cần xin
        String[] requiredPermissions = new String[]{
                Manifest.permission.CAMERA,
                Manifest.permission.ACCESS_FINE_LOCATION,
                Manifest.permission.ACCESS_COARSE_LOCATION
        };
        // Kiểm tra quyền camera
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED) {
            startCamera();
            // Dù đã có quyền Camera, vẫn phải kiểm tra xem có quyền Location chưa.
            // Nếu chưa, kích hoạt xin quyền Location ngầm (không ảnh hưởng Camera đang chạy)
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                permissionLauncher.launch(requiredPermissions);
            }
        } else {
            // Xin cả mảng quyền từ đầu
            permissionLauncher.launch(requiredPermissions);
        }
    }

    private void initUI() {
        viewFinder      = findViewById(R.id.viewFinder);
        imgDemo         = findViewById(R.id.imgDemo);
        editDescription = findViewById(R.id.editDescription);
        txtLocation     = findViewById(R.id.txtLocation);
        txtLightWarning = findViewById(R.id.txtLightWarning);
        privacyOverlay  = findViewById(R.id.privacyOverlay);
        overlayHeader   = findViewById(R.id.overlayHeader);
        btnCapture      = findViewById(R.id.btnCapture);
        btnGallery      = findViewById(R.id.btnGallery);
        btnCancel       = findViewById(R.id.btnCancel);
        btnPost         = findViewById(R.id.btnPost);
        btnPickLocation = findViewById(R.id.btnPickLocation);
        btnSwitchCamera = findViewById(R.id.btnSwitchCamera);
    }

    private void setupButtons() {
        btnCapture.setOnClickListener(v -> takePhoto());
        btnGallery.setOnClickListener(v -> openGalleryWithPermission());
        btnCancel.setOnClickListener(v -> resetToCapture());
        btnPost.setOnClickListener(v -> uploadPost());
        btnPickLocation.setOnClickListener(v -> pickLocationManually());
        btnSwitchCamera.setOnClickListener(v -> toggleCamera());
    }

    private void toggleCamera() {
        if (lensFacing == CameraSelector.LENS_FACING_BACK) {
            lensFacing = CameraSelector.LENS_FACING_FRONT;
        } else {
            lensFacing = CameraSelector.LENS_FACING_BACK;
        }
        startCamera();
    }

    private void openGalleryWithPermission() {
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_MEDIA_IMAGES) == PackageManager.PERMISSION_GRANTED) {
                mGetContent.launch("image/*");
            } else {
                mediaPermissionLauncher.launch(Manifest.permission.READ_MEDIA_IMAGES);
            }
        } else {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED) {
                mGetContent.launch("image/*");
            } else {
                mediaPermissionLauncher.launch(Manifest.permission.READ_EXTERNAL_STORAGE);
            }
        }
    }

    private void pickLocationManually() {
        android.widget.EditText input = new android.widget.EditText(this);
        input.setHint("Nhập địa chỉ của bạn...");
        
        // Add some margin to the EditText
        android.widget.FrameLayout container = new android.widget.FrameLayout(this);
        android.widget.FrameLayout.LayoutParams params = new  android.widget.FrameLayout.LayoutParams(
                android.view.ViewGroup.LayoutParams.MATCH_PARENT,
                android.view.ViewGroup.LayoutParams.WRAP_CONTENT
        );
        params.leftMargin = 50;
        params.rightMargin = 50;
        input.setLayoutParams(params);
        container.addView(input);

        new android.app.AlertDialog.Builder(this)
                .setTitle("Nhập vị trí thủ công")
                .setView(container)
                .setPositiveButton("Xác nhận", (dialog, which) -> {
                    String manualLocation = input.getText().toString().trim();
                    if (!manualLocation.isEmpty()) {
                        txtLocation.setText(manualLocation);
                    }
                })
                .setNegativeButton("Hủy", null)
                .show();
    }

    /** Bottom nav điều hướng giống MainActivity */
    private void setupNavigation() {
        findViewById(R.id.home).setOnClickListener(v -> {
            startActivity(new Intent(this, MainActivity.class));
            finish();
        });
        findViewById(R.id.friend).setOnClickListener(v -> {
            startActivity(new Intent(this, BanBe.class));
            finish();
        });
        // btnNavCamera → trang hiện tại, không cần làm gì
        findViewById(R.id.btnNavChat).setOnClickListener(v -> {
            startActivity(new Intent(this, ListFriendsChatActivity.class));
            finish();
        });
        // btnNavProfile: chưa implement
    }

    // ── State: Camera Mode → Preview Mode ──────────────────────────────────
    private void showPreview(Uri uri) {
        finalImageUri = uri;

        // Ẩn camera view + nút camera/gallery
        viewFinder.setVisibility(View.GONE);
        btnCapture.setVisibility(View.GONE);
        btnGallery.setVisibility(View.GONE);
        if (btnSwitchCamera != null) btnSwitchCamera.setVisibility(View.GONE);

        // Hiện ảnh + các thành phần preview
        imgDemo.setVisibility(View.VISIBLE);
        imgDemo.setImageURI(uri);
        btnPost.setVisibility(View.VISIBLE);
        editDescription.setVisibility(View.VISIBLE);
        overlayHeader.setVisibility(View.VISIBLE);
        btnCancel.setVisibility(View.VISIBLE);

        // btnPickLocation chỉ hiện khi chọn từ thư viện
        btnPickLocation.setVisibility(isFromGallery ? View.VISIBLE : View.GONE);

        fetchLocation();
    }

    // ── State: Preview Mode → Camera Mode ──────────────────────────────────
    private void resetToCapture() {
        viewFinder.setVisibility(View.VISIBLE);
        btnCapture.setVisibility(View.VISIBLE);
        btnGallery.setVisibility(View.VISIBLE);

        imgDemo.setVisibility(View.GONE);
        btnPost.setVisibility(View.GONE);
        editDescription.setVisibility(View.GONE);
        overlayHeader.setVisibility(View.GONE);
        btnCancel.setVisibility(View.GONE);
        btnPickLocation.setVisibility(View.GONE);
        if (btnSwitchCamera != null) btnSwitchCamera.setVisibility(View.VISIBLE);

        editDescription.setText("");
        isFromGallery = false;
        finalImageUri = null;
        
        // Khôi phục lại trạng thái nút Đăng
        btnPost.setEnabled(true);
        btnPost.setText("ĐĂNG NGAY");
    }

    // ── Camera ──────────────────────────────────────────────────────────────
    private void startCamera() {
        // Giới hạn preview 720p — giảm CPU ~30-40% so với full resolution
        ResolutionSelector resolutionSelector = new ResolutionSelector.Builder()
                .setResolutionStrategy(new ResolutionStrategy(
                        new Size(1280, 720),
                        ResolutionStrategy.FALLBACK_RULE_CLOSEST_LOWER_THEN_HIGHER))
                .build();

        ListenableFuture<ProcessCameraProvider> future = ProcessCameraProvider.getInstance(this);
        future.addListener(() -> {
            try {
                ProcessCameraProvider cameraProvider = future.get();

                Preview preview = new Preview.Builder()
                        .setResolutionSelector(resolutionSelector)
                        .build();
                preview.setSurfaceProvider(viewFinder.getSurfaceProvider());

                // MINIMIZE_LATENCY: ưu tiên tốc độ chụp thay vì chất lượng tối đa
                imageCapture = new ImageCapture.Builder()
                        .setCaptureMode(ImageCapture.CAPTURE_MODE_MINIMIZE_LATENCY)
                        .setResolutionSelector(resolutionSelector)
                        .build();

                cameraProvider.unbindAll();
                cameraProvider.bindToLifecycle(this, new CameraSelector.Builder().requireLensFacing(lensFacing).build(), preview, imageCapture);
            } catch (ExecutionException | InterruptedException e) {
                e.printStackTrace();
            }
        }, ContextCompat.getMainExecutor(this));
    }

    private void takePhoto() {
        if (imageCapture == null) return;
        File photoFile = new File(getExternalCacheDir(), System.currentTimeMillis() + ".jpg");
        ImageCapture.OutputFileOptions options = new ImageCapture.OutputFileOptions.Builder(photoFile).build();
        imageCapture.takePicture(options, ContextCompat.getMainExecutor(this),
                new ImageCapture.OnImageSavedCallback() {
                    @Override
                    public void onImageSaved(@NonNull ImageCapture.OutputFileResults r) {
                        isFromGallery = false;
                        showPreview(Uri.fromFile(photoFile));
                    }
                    @Override
                    public void onError(@NonNull ImageCaptureException e) {
                        Toast.makeText(PostActivity.this, "Lỗi chụp ảnh: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                    }
                });
    }

    // ── Location ─────────────────────────────────────────────────────────────────
    private com.google.android.gms.location.LocationCallback locationCallback;

    private void fetchLocation() {
        txtLocation.setText("Đang lấy vị trí...");
        // Kiểm tra quyền Location
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED &&
                ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            txtLocation.setText("Vị trí không khả dụng (Thiếu quyền)");
            btnPickLocation.setVisibility(View.VISIBLE);
            return;
        }

        // BƯỚC 1: thử lấy vị trí đã cache sẵn (nhanh, thường là vị trí thực)
        fusedLocationClient.getLastLocation().addOnSuccessListener(this, cachedLoc -> {
            if (cachedLoc != null) {
                // Có cache → dùng luôn, không cần chờ GPS
                geocodeAddress(cachedLoc);
            } else {
                // Không có cache → yêu cầu GPS mới (mất vài giây)
                requestFreshLocation();
            }
        }).addOnFailureListener(e -> requestFreshLocation());
    }

    /** Yêu cầu GPS cập nhật mới khi không có cached location */
    @android.annotation.SuppressLint("MissingPermission")
    private void requestFreshLocation() {
        final boolean[] locationReceived = {false};

        com.google.android.gms.location.LocationRequest locationRequest =
                new com.google.android.gms.location.LocationRequest.Builder(
                        com.google.android.gms.location.Priority.PRIORITY_HIGH_ACCURACY, 2000)
                        .setMinUpdateIntervalMillis(1000)
                        .setMaxUpdates(1)
                        .build();

        locationCallback = new com.google.android.gms.location.LocationCallback() {
            @Override
            public void onLocationResult(@NonNull com.google.android.gms.location.LocationResult result) {
                if (locationReceived[0]) return;
                android.location.Location loc = result.getLastLocation();
                if (loc != null) {
                    locationReceived[0] = true;
                    stopLocationUpdates(locationCallback);
                    geocodeAddress(loc);
                }
            }
        };

        fusedLocationClient.requestLocationUpdates(locationRequest, locationCallback,
                android.os.Looper.getMainLooper());

        // Timeout 10 giây — nếu GPS vẫn không trả về thì hiện nút nhập thủ công
        com.google.android.gms.location.LocationCallback cb = locationCallback;
        new android.os.Handler(android.os.Looper.getMainLooper()).postDelayed(() -> {
            if (!locationReceived[0]) {
                stopLocationUpdates(cb);
                runOnUiThread(() -> {
                    txtLocation.setText("Không xác định được vị trí");
                    btnPickLocation.setVisibility(View.VISIBLE);
                });
            }
        }, 10000);
    }

    private void stopLocationUpdates(com.google.android.gms.location.LocationCallback cb) {
        if (cb != null) {
            fusedLocationClient.removeLocationUpdates(cb);
        }
    }

    private void geocodeAddress(android.location.Location location) {
        executorService.execute(() -> {
            try {
                // Sử dụng OpenStreetMap (Nominatim API) với tham số accept-language=vi để lấy Tiếng Việt
                String urlString = "https://nominatim.openstreetmap.org/reverse?format=json" +
                        "&lat=" + location.getLatitude() +
                        "&lon=" + location.getLongitude() +
                        "&accept-language=vi";

                java.net.URL url = new java.net.URL(urlString);
                java.net.HttpURLConnection conn = (java.net.HttpURLConnection) url.openConnection();
                conn.setRequestMethod("GET");
                // QUAN TRỌNG: Nominatim bắt buộc phải có User-Agent để nhận diện
                conn.setRequestProperty("User-Agent", "AppSocialAndroid/1.0");
                conn.setConnectTimeout(5000);
                conn.setReadTimeout(5000);

                if (conn.getResponseCode() == 200) {
                    java.io.BufferedReader in = new java.io.BufferedReader(new java.io.InputStreamReader(conn.getInputStream()));
                    String inputLine;
                    StringBuilder response = new StringBuilder();
                    while ((inputLine = in.readLine()) != null) {
                        response.append(inputLine);
                    }
                    in.close();

                    org.json.JSONObject jsonObject = new org.json.JSONObject(response.toString());
                    if (jsonObject.has("display_name")) {
                        String addressText = jsonObject.getString("display_name");
                        runOnUiThread(() -> txtLocation.setText(addressText));
                    } else {
                        runOnUiThread(() -> txtLocation.setText("Vị trí: " + location.getLatitude() + ", " + location.getLongitude()));
                    }
                } else {
                    int responseCode = conn.getResponseCode();
                    runOnUiThread(() -> txtLocation.setText("Lỗi lấy vị trí: " + responseCode));
                }
            } catch (Exception e) {
                e.printStackTrace();
                runOnUiThread(() -> txtLocation.setText("Lỗi mạng khi tải vị trí"));
            }
        });
    }

    private String encodeImageToBase64(Uri imageUri) {
        try {
            InputStream imageStream = getContentResolver().openInputStream(imageUri);
            Bitmap selectedImage = BitmapFactory.decodeStream(imageStream);

            // Giảm kích thước ảnh (Resize) để tránh vượt quá 1MB của Firestore
            int MAX_DIMENSION = 800; // Resize về tối đa 800px
            int width = selectedImage.getWidth();
            int height = selectedImage.getHeight();
            if (width > MAX_DIMENSION || height > MAX_DIMENSION) {
                float ratio = Math.min((float) MAX_DIMENSION / width, (float) MAX_DIMENSION / height);
                width = Math.round((float) ratio * width);
                height = Math.round((float) ratio * height);
                selectedImage = Bitmap.createScaledBitmap(selectedImage, width, height, false);
            }

            // Nén ảnh (Compress)
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            selectedImage.compress(Bitmap.CompressFormat.JPEG, 70, baos);
            byte[] imageBytes = baos.toByteArray();
            
            // Chuyển sang Base64
            return "data:image/jpeg;base64," + Base64.encodeToString(imageBytes, Base64.DEFAULT);
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    // ── Upload Post ──────────────────────────────────────────────────────
    private void uploadPost() {
        if (finalImageUri == null) return;
        
        String desc = editDescription.getText().toString().trim();
        String loc  = txtLocation.getText().toString().trim();
        String uid  = FirebaseAuth.getInstance().getUid();
        
        // Disable nút tránh double-tap
        btnPost.setEnabled(false);
        btnPost.setText("Đang xử lý ảnh...");
        
        // Xử lý nén và encode Base64 trên background thread để không block UI
        executorService.execute(() -> {
            String base64Image = encodeImageToBase64(finalImageUri);
            
            runOnUiThread(() -> {
                if (base64Image == null) {
                    btnPost.setEnabled(true);
                    btnPost.setText("ĐĂNG NGAY");
                    Toast.makeText(this, "Lỗi khi xử lý ảnh!", Toast.LENGTH_SHORT).show();
                    return;
                }
                
                btnPost.setText("Đang đăng bài...");
                
                // Lưu Post với chuỗi Base64
                Post newPost = new Post(null, uid, base64Image, desc, loc);
                FirebaseFirestore.getInstance().collection("Posts").add(newPost)
                    .addOnSuccessListener(ref -> {
                        Toast.makeText(this, "Đã đăng bài thành công!", Toast.LENGTH_SHORT).show();
                        resetToCapture();
                    })
                    .addOnFailureListener(e -> {
                        btnPost.setEnabled(true);
                        btnPost.setText("ĐĂNG NGAY");
                        Toast.makeText(this, "Lỗi lưu bài: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                    });
            });
        });
    }

    // ── Sensor ───────────────────────────────────────────────────────────
    @Override
    public void onSensorChanged(SensorEvent event) {
        super.onSensorChanged(event);
        if (event.sensor.getType() == Sensor.TYPE_LIGHT) {
            float lux = event.values[0];
            WindowManager.LayoutParams lp = getWindow().getAttributes();
            if (lux < 100) {
                lp.screenBrightness = 0.9f; // Sáng thấp -> tăng sáng màn hình
                if (txtLightWarning != null) {
                    txtLightWarning.setVisibility(View.VISIBLE);
                    txtLightWarning.setText("Ánh sáng yếu (" + lux + " lux)\nBạn hãy giữ chắc tay!");
                    txtLightWarning.setTextColor(android.graphics.Color.parseColor("#FFEB3B")); // Vàng
                }
            } else if (lux > 1000) {
                lp.screenBrightness = 0.2f; // Sáng cao -> giảm sáng màn hình
                if (txtLightWarning != null) {
                    txtLightWarning.setVisibility(View.VISIBLE);
                    txtLightWarning.setText("Chói quá (" + lux + " lux)\nGiảm độ sáng màn hình!");
                    txtLightWarning.setTextColor(android.graphics.Color.parseColor("#FF5252")); // Đỏ
                }
            } else {
                lp.screenBrightness = WindowManager.LayoutParams.BRIGHTNESS_OVERRIDE_NONE;
                if (txtLightWarning != null) {
                    txtLightWarning.setVisibility(View.GONE);
                }
            }
            getWindow().setAttributes(lp);
        }
    }

    @Override
    protected void onPrivacyTriggered(boolean isCovered) {
        privacyOverlay.setVisibility(isCovered ? View.VISIBLE : View.GONE);
        btnPost.setEnabled(!isCovered);
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (lightSensor != null) {
            // SENSOR_DELAY_NORMAL (~200ms) thay vì SENSOR_DELAY_UI (~16ms) — giảm tải CPU
            sensorManager.registerListener(this, lightSensor, SensorManager.SENSOR_DELAY_NORMAL);
        }
    }

    @Override
    protected void onPause() {
        // Chỉ unregister lightSensor trước khi gọi super để tránh double-unregister;
        // super.onPause() (BaseSensorActivity) sẽ unregisterListener(this) cho tất cả sensor
        // → không cần unregister lightSensor thêm một lần nữa sau super
        stopLocationUpdates(locationCallback);
        super.onPause(); // BaseSensorActivity.onPause() unregisterListener(this) cho tất cả sensor
    }

}