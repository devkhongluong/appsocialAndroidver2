package com.example.appsocialver2.fragments;

import android.Manifest;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.net.ConnectivityManager;
import android.net.NetworkCapabilities;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.util.Base64;
import android.util.Size;

import com.example.appsocialver2.activity.MapPickerActivity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.camera.core.CameraSelector;
import androidx.camera.core.ImageCapture;
import androidx.camera.core.ImageCaptureException;
import androidx.camera.core.Preview;
import androidx.camera.core.resolutionselector.ResolutionSelector;
import androidx.camera.core.resolutionselector.ResolutionStrategy;
import androidx.camera.lifecycle.ProcessCameraProvider;
import androidx.camera.view.PreviewView;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;

import com.example.appsocialver2.Models.Post;
import com.example.appsocialver2.R;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;
import com.google.android.material.floatingactionbutton.ExtendedFloatingActionButton;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.InputStream;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Fragment màn hình Camera / Đăng bài.
 * Thay thế PostActivity. Tự quản lý SensorEventListener vì Fragment không kế thừa BaseSensorActivity.
 */
public class PostFragment extends Fragment implements SensorEventListener {

    // ── UI ──────────────────────────────────────────────────────────────────
    private PreviewView viewFinder;
    private ImageView   imgDemo;
    private EditText    editDescription;
    private TextView    txtLocation, txtLightWarning;
    private View        privacyOverlay, overlayHeader;
    private ImageButton btnCapture, btnGallery, btnCancel, btnPickLocation, btnSwitchCamera;
    private ExtendedFloatingActionButton btnPost;

    // ── Camera ──────────────────────────────────────────────────────────────
    private ImageCapture imageCapture;
    private int lensFacing = CameraSelector.LENS_FACING_BACK;

    // ── State ───────────────────────────────────────────────────────────────
    private Uri     finalImageUri;
    private boolean isFromGallery = false;

    // ── Sensor ──────────────────────────────────────────────────────────────
    private SensorManager sensorManager;
    private Sensor        lightSensor;
    private Sensor        proximitySensor;

    // ── Location ─────────────────────────────────────────────────────────────
    private FusedLocationProviderClient fusedLocationClient;
    private com.google.android.gms.location.LocationCallback locationCallback;
    private final ExecutorService executorService = Executors.newSingleThreadExecutor();

    // ── Permissions ──────────────────────────────────────────────────────────
    private final ActivityResultLauncher<String[]> permissionLauncher =
            registerForActivityResult(new ActivityResultContracts.RequestMultiplePermissions(), result -> {
                Boolean cameraGranted = result.getOrDefault(Manifest.permission.CAMERA, false);
                if (cameraGranted != null && cameraGranted) {
                    startCamera();
                } else {
                    if (isAdded())
                        Toast.makeText(requireContext(),
                                "Cần quyền Camera để sử dụng tính năng này", Toast.LENGTH_LONG).show();
                }
            });

    private final ActivityResultLauncher<String> mGetContent =
            registerForActivityResult(new ActivityResultContracts.GetContent(), uri -> {
                if (uri != null) { isFromGallery = true; showPreview(uri); }
            });

    private final ActivityResultLauncher<String> mediaPermissionLauncher =
            registerForActivityResult(new ActivityResultContracts.RequestPermission(), granted -> {
                if (granted) {
                    mGetContent.launch("image/*");
                } else {
                    if (isAdded())
                        Toast.makeText(requireContext(),
                                "Cần quyền truy cập thư viện ảnh", Toast.LENGTH_SHORT).show();
                }
            });

    // Nhận kết quả tọa độ từ MapPickerActivity, dịch ngược sang địa chỉ văn bản
    private final ActivityResultLauncher<Intent> mapPickerLauncher =
            registerForActivityResult(new ActivityResultContracts.StartActivityForResult(), result -> {
                if (result.getResultCode() == android.app.Activity.RESULT_OK
                        && result.getData() != null) {
                    double lat = result.getData().getDoubleExtra("LAT", 0);
                    double lng = result.getData().getDoubleExtra("LNG", 0);
                    if (isAdded()) txtLocation.setText("Đang tải địa chỉ...");
                    android.location.Location loc = new android.location.Location("");
                    loc.setLatitude(lat);
                    loc.setLongitude(lng);
                    geocodeAddress(loc);
                }
            });

    // ────────────────────────────────────────────────────────────────────────
    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_post, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        sensorManager   = (SensorManager) requireContext().getSystemService(Context.SENSOR_SERVICE);
        lightSensor      = sensorManager.getDefaultSensor(Sensor.TYPE_LIGHT);
        proximitySensor  = sensorManager.getDefaultSensor(Sensor.TYPE_PROXIMITY);

        fusedLocationClient = LocationServices.getFusedLocationProviderClient(requireActivity());

        initUI(view);
        setupButtons();

        String[] requiredPermissions = {
                Manifest.permission.CAMERA,
                Manifest.permission.ACCESS_FINE_LOCATION,
                Manifest.permission.ACCESS_COARSE_LOCATION
        };

        if (ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.CAMERA)
                == PackageManager.PERMISSION_GRANTED) {
            startCamera();
            if (ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.ACCESS_FINE_LOCATION)
                    != PackageManager.PERMISSION_GRANTED) {
                permissionLauncher.launch(requiredPermissions);
            }
        } else {
            permissionLauncher.launch(requiredPermissions);
        }
    }

    private void initUI(View view) {
        viewFinder      = view.findViewById(R.id.viewFinder);
        imgDemo         = view.findViewById(R.id.imgDemo);
        editDescription = view.findViewById(R.id.editDescription);
        txtLocation     = view.findViewById(R.id.txtLocation);
        txtLightWarning = view.findViewById(R.id.txtLightWarning);
        privacyOverlay  = view.findViewById(R.id.privacyOverlay);
        overlayHeader   = view.findViewById(R.id.overlayHeader);
        btnCapture      = view.findViewById(R.id.btnCapture);
        btnGallery      = view.findViewById(R.id.btnGallery);
        btnCancel       = view.findViewById(R.id.btnCancel);
        btnPost         = view.findViewById(R.id.btnPost);
        btnPickLocation = view.findViewById(R.id.btnPickLocation);
        btnSwitchCamera = view.findViewById(R.id.btnSwitchCamera);
    }

    private void setupButtons() {
        btnCapture.setOnClickListener(v -> takePhoto());
        btnGallery.setOnClickListener(v -> openGalleryWithPermission());
        btnCancel.setOnClickListener(v -> resetToCapture());
        btnPost.setOnClickListener(v -> uploadPost());
        btnPickLocation.setOnClickListener(v -> pickLocationManually());
        btnSwitchCamera.setOnClickListener(v -> toggleCamera());
    }

    // ── Camera ──────────────────────────────────────────────────────────────
    private void toggleCamera() {
        lensFacing = (lensFacing == CameraSelector.LENS_FACING_BACK)
                ? CameraSelector.LENS_FACING_FRONT : CameraSelector.LENS_FACING_BACK;
        startCamera();
    }

    private void startCamera() {
        if (!isAdded()) return;
        ResolutionSelector rs = new ResolutionSelector.Builder()
                .setResolutionStrategy(new ResolutionStrategy(
                        new Size(1280, 720),
                        ResolutionStrategy.FALLBACK_RULE_CLOSEST_LOWER_THEN_HIGHER))
                .build();
        ListenableFuture<ProcessCameraProvider> future =
                ProcessCameraProvider.getInstance(requireContext());
        future.addListener(() -> {
            try {
                ProcessCameraProvider cp = future.get();
                Preview preview = new Preview.Builder().setResolutionSelector(rs).build();
                preview.setSurfaceProvider(viewFinder.getSurfaceProvider());
                imageCapture = new ImageCapture.Builder()
                        .setCaptureMode(ImageCapture.CAPTURE_MODE_MINIMIZE_LATENCY)
                        .setResolutionSelector(rs).build();
                cp.unbindAll();
                cp.bindToLifecycle(requireActivity(),
                        new CameraSelector.Builder().requireLensFacing(lensFacing).build(),
                        preview, imageCapture);
            } catch (ExecutionException | InterruptedException e) {
                e.printStackTrace();
            }
        }, ContextCompat.getMainExecutor(requireContext()));
    }

    private void takePhoto() {
        if (imageCapture == null) return;
        File photoFile = new File(requireContext().getExternalCacheDir(),
                System.currentTimeMillis() + ".jpg");
        ImageCapture.OutputFileOptions options =
                new ImageCapture.OutputFileOptions.Builder(photoFile).build();
        imageCapture.takePicture(options,
                ContextCompat.getMainExecutor(requireContext()),
                new ImageCapture.OnImageSavedCallback() {
                    @Override
                    public void onImageSaved(@NonNull ImageCapture.OutputFileResults r) {
                        isFromGallery = false;
                        showPreview(Uri.fromFile(photoFile));
                        if (!isNetworkAvailable())
                            Toast.makeText(requireContext(),
                                    "Không có kết nối internet!", Toast.LENGTH_LONG).show();
                    }
                    @Override
                    public void onError(@NonNull ImageCaptureException e) {
                        if (isAdded())
                            Toast.makeText(requireContext(),
                                    "Lỗi chụp ảnh: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                    }
                });
    }

    // ── Gallery ──────────────────────────────────────────────────────────────
    private void openGalleryWithPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(requireContext(),
                    Manifest.permission.READ_MEDIA_IMAGES) == PackageManager.PERMISSION_GRANTED) {
                mGetContent.launch("image/*");
            } else {
                mediaPermissionLauncher.launch(Manifest.permission.READ_MEDIA_IMAGES);
            }
        } else {
            if (ContextCompat.checkSelfPermission(requireContext(),
                    Manifest.permission.READ_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED) {
                mGetContent.launch("image/*");
            } else {
                mediaPermissionLauncher.launch(Manifest.permission.READ_EXTERNAL_STORAGE);
            }
        }
    }

    // ── Preview ──────────────────────────────────────────────────────────────
    private void showPreview(Uri uri) {
        finalImageUri = uri;
        viewFinder.setVisibility(View.GONE);
        btnCapture.setVisibility(View.GONE);
        btnGallery.setVisibility(View.GONE);
        if (btnSwitchCamera != null) btnSwitchCamera.setVisibility(View.GONE);
        imgDemo.setVisibility(View.VISIBLE);
        imgDemo.setImageURI(uri);
        btnPost.setVisibility(View.VISIBLE);
        editDescription.setVisibility(View.VISIBLE);
        overlayHeader.setVisibility(View.VISIBLE);
        btnCancel.setVisibility(View.VISIBLE);
        // Luôn hiện nút bản đồ cho cả ảnh chụp và ảnh thư viện
        btnPickLocation.setVisibility(View.VISIBLE);
        fetchLocation();
    }

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
        btnPost.setEnabled(true);
        btnPost.setText("ĐĂNG NGAY");
    }

    // ── Location ─────────────────────────────────────────────────────────────
    private void pickLocationManually() {
        // Mở màn hình bản đồ OSMDroid để người dùng chọn vị trí trực quan
        Intent intent = new Intent(requireContext(), MapPickerActivity.class);
        mapPickerLauncher.launch(intent);
    }

    private void fetchLocation() {
        txtLocation.setText("Đang lấy vị trí...");
        if (ContextCompat.checkSelfPermission(requireContext(),
                Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED &&
            ContextCompat.checkSelfPermission(requireContext(),
                Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            txtLocation.setText("Vị trí không khả dụng (Thiếu quyền)");
            btnPickLocation.setVisibility(View.VISIBLE);
            return;
        }
        fusedLocationClient.getLastLocation()
                .addOnSuccessListener(requireActivity(), loc -> {
                    if (loc != null) geocodeAddress(loc);
                    else requestFreshLocation();
                })
                .addOnFailureListener(e -> requestFreshLocation());
    }

    @android.annotation.SuppressLint("MissingPermission")
    private void requestFreshLocation() {
        final boolean[] received = {false};
        com.google.android.gms.location.LocationRequest req =
                new com.google.android.gms.location.LocationRequest.Builder(
                        com.google.android.gms.location.Priority.PRIORITY_HIGH_ACCURACY, 2000)
                        .setMinUpdateIntervalMillis(1000).setMaxUpdates(1).build();
        locationCallback = new com.google.android.gms.location.LocationCallback() {
            @Override
            public void onLocationResult(@NonNull com.google.android.gms.location.LocationResult r) {
                if (received[0]) return;
                android.location.Location loc = r.getLastLocation();
                if (loc != null) {
                    received[0] = true;
                    stopLocationUpdates(locationCallback);
                    geocodeAddress(loc);
                }
            }
        };
        fusedLocationClient.requestLocationUpdates(req, locationCallback,
                android.os.Looper.getMainLooper());
        com.google.android.gms.location.LocationCallback cb = locationCallback;
        new android.os.Handler(android.os.Looper.getMainLooper()).postDelayed(() -> {
            if (!received[0]) {
                stopLocationUpdates(cb);
                if (isAdded()) requireActivity().runOnUiThread(() -> {
                    txtLocation.setText("Không xác định được vị trí");
                    btnPickLocation.setVisibility(View.VISIBLE);
                });
            }
        }, 10000);
    }

    private void stopLocationUpdates(com.google.android.gms.location.LocationCallback cb) {
        if (cb != null) fusedLocationClient.removeLocationUpdates(cb);
    }

    private void geocodeAddress(android.location.Location location) {
        executorService.execute(() -> {
            try {
                String url = "https://nominatim.openstreetmap.org/reverse?format=json"
                        + "&lat=" + location.getLatitude()
                        + "&lon=" + location.getLongitude()
                        + "&accept-language=vi";
                java.net.HttpURLConnection conn =
                        (java.net.HttpURLConnection) new java.net.URL(url).openConnection();
                conn.setRequestProperty("User-Agent", "AppSocialAndroid/1.0");
                conn.setConnectTimeout(5000);
                conn.setReadTimeout(5000);
                if (conn.getResponseCode() == 200) {
                    java.io.BufferedReader in = new java.io.BufferedReader(
                            new java.io.InputStreamReader(conn.getInputStream()));
                    StringBuilder sb = new StringBuilder();
                    String line;
                    while ((line = in.readLine()) != null) sb.append(line);
                    in.close();
                    org.json.JSONObject json = new org.json.JSONObject(sb.toString());
                    String addr = json.has("display_name") ? json.getString("display_name")
                            : "Vị trí: " + location.getLatitude() + ", " + location.getLongitude();
                    if (isAdded()) requireActivity().runOnUiThread(() -> txtLocation.setText(addr));
                }
            } catch (Exception e) {
                e.printStackTrace();
                if (isAdded())
                    requireActivity().runOnUiThread(() -> txtLocation.setText("Lỗi mạng khi tải vị trí"));
            }
        });
    }

    // ── Network + Upload ─────────────────────────────────────────────────────
    private boolean isNetworkAvailable() {
        ConnectivityManager cm =
                (ConnectivityManager) requireContext().getSystemService(Context.CONNECTIVITY_SERVICE);
        if (cm == null) return false;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            android.net.Network net = cm.getActiveNetwork();
            if (net == null) return false;
            NetworkCapabilities cap = cm.getNetworkCapabilities(net);
            return cap != null && (cap.hasTransport(NetworkCapabilities.TRANSPORT_WIFI)
                    || cap.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR)
                    || cap.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET));
        } else {
            android.net.NetworkInfo info = cm.getActiveNetworkInfo();
            return info != null && info.isConnected();
        }
    }

    private String encodeImageToBase64(Uri imageUri) {
        try {
            InputStream is = requireContext().getContentResolver().openInputStream(imageUri);
            Bitmap bmp = BitmapFactory.decodeStream(is);
            int MAX = 800, w = bmp.getWidth(), h = bmp.getHeight();
            if (w > MAX || h > MAX) {
                float r = Math.min((float) MAX / w, (float) MAX / h);
                bmp = Bitmap.createScaledBitmap(bmp, Math.round(r * w), Math.round(r * h), false);
            }
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            bmp.compress(Bitmap.CompressFormat.JPEG, 70, baos);
            return "data:image/jpeg;base64,"
                    + Base64.encodeToString(baos.toByteArray(), Base64.DEFAULT);
        } catch (Exception e) { e.printStackTrace(); return null; }
    }

    private void uploadPost() {
        if (finalImageUri == null) return;
        String desc = editDescription.getText().toString().trim();
        String loc  = txtLocation.getText().toString().trim();
        String uid  = FirebaseAuth.getInstance().getUid();

        btnPost.setEnabled(false);
        btnPost.setText("Đang kiểm tra kết nối...");

        executorService.execute(() -> {
            boolean hasInternet;
            try {
                java.net.Socket socket = new java.net.Socket();
                socket.connect(new java.net.InetSocketAddress("8.8.8.8", 53), 1500);
                socket.close();
                hasInternet = true;
            } catch (Exception e) { hasInternet = false; }

            final boolean connected = hasInternet;
            if (!isAdded()) return;
            requireActivity().runOnUiThread(() -> {
                if (!connected) {
                    btnPost.setEnabled(true);
                    btnPost.setText("ĐĂNG NGAY");
                    Toast.makeText(requireContext(),
                            "Không có kết nối internet! Vui lòng kiểm tra lại.",
                            Toast.LENGTH_LONG).show();
                    return;
                }
                btnPost.setText("Đang xử lý ảnh...");
                executorService.execute(() -> {
                    String base64 = encodeImageToBase64(finalImageUri);
                    if (!isAdded()) return;
                    requireActivity().runOnUiThread(() -> {
                        if (base64 == null) {
                            btnPost.setEnabled(true);
                            btnPost.setText("ĐĂNG NGAY");
                            Toast.makeText(requireContext(),
                                    "Lỗi khi xử lý ảnh!", Toast.LENGTH_SHORT).show();
                            return;
                        }
                        btnPost.setText("Đang đăng bài...");
                        Post newPost = new Post(null, uid, base64, desc, loc);
                        FirebaseFirestore.getInstance().collection("Posts").add(newPost)
                                .addOnSuccessListener(ref -> {
                                    Toast.makeText(requireContext(),
                                            "Đã đăng bài thành công!", Toast.LENGTH_SHORT).show();
                                    resetToCapture();
                                })
                                .addOnFailureListener(e -> {
                                    btnPost.setEnabled(true);
                                    btnPost.setText("ĐĂNG NGAY");
                                    Toast.makeText(requireContext(),
                                            "Lỗi lưu bài: " + e.getMessage(),
                                            Toast.LENGTH_SHORT).show();
                                });
                    });
                });
            });
        });
    }

    // ── Sensor ───────────────────────────────────────────────────────────────
    @Override
    public void onSensorChanged(SensorEvent event) {
        if (event.sensor.getType() == Sensor.TYPE_LIGHT && isAdded()) {
            float lux = event.values[0];
            WindowManager.LayoutParams lp = requireActivity().getWindow().getAttributes();
            lp.screenBrightness = lux < 100 ? 0.9f
                    : lux > 1000 ? 0.2f
                    : WindowManager.LayoutParams.BRIGHTNESS_OVERRIDE_NONE;
            requireActivity().getWindow().setAttributes(lp);
        }
        if (event.sensor.getType() == Sensor.TYPE_PROXIMITY && isAdded()) {
            boolean covered = proximitySensor != null
                    && event.values[0] < proximitySensor.getMaximumRange();
            if (privacyOverlay != null)
                privacyOverlay.setVisibility(covered ? View.VISIBLE : View.GONE);
            if (btnPost != null) btnPost.setEnabled(!covered);
        }
    }

    @Override public void onAccuracyChanged(Sensor sensor, int accuracy) {}

    @Override
    public void onResume() {
        super.onResume();
        if (sensorManager != null) {
            if (proximitySensor != null)
                sensorManager.registerListener(this, proximitySensor, SensorManager.SENSOR_DELAY_UI);
            if (lightSensor != null)
                sensorManager.registerListener(this, lightSensor, SensorManager.SENSOR_DELAY_NORMAL);
        }
    }

    @Override
    public void onPause() {
        super.onPause();
        stopLocationUpdates(locationCallback);
        if (sensorManager != null) sensorManager.unregisterListener(this);
    }
}
