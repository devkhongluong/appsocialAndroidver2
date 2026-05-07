package com.example.appsocialver2.activity;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;

import androidx.appcompat.app.AppCompatActivity;

import com.example.appsocialver2.R;

import org.osmdroid.config.Configuration;
import org.osmdroid.util.GeoPoint;
import org.osmdroid.views.MapView;
import org.osmdroid.api.IGeoPoint;

public class MapPickerActivity extends AppCompatActivity {

    private MapView map;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Khởi tạo cấu hình OSMDroid trước khi setContentView
        Configuration.getInstance().load(this, getSharedPreferences("osmdroid", MODE_PRIVATE));
        Configuration.getInstance().setUserAgentValue("AppSocialAndroid/1.0");

        setContentView(R.layout.activity_map_picker);

        map = findViewById(R.id.map);
        map.setMultiTouchControls(true);

        // Nhận vị trí khởi đầu từ Intent (nếu có), mặc định là Hà Nội
        double lat = getIntent().getDoubleExtra("LAT", 21.028511);
        double lng = getIntent().getDoubleExtra("LNG", 105.804817);

        GeoPoint startPoint = new GeoPoint(lat, lng);
        map.getController().setZoom(16.0);
        map.getController().setCenter(startPoint);

        Button btnConfirmLocation = findViewById(R.id.btnConfirmLocation);
        btnConfirmLocation.setOnClickListener(v -> {
            IGeoPoint center = map.getMapCenter();
            Intent resultIntent = new Intent();
            resultIntent.putExtra("LAT", center.getLatitude());
            resultIntent.putExtra("LNG", center.getLongitude());
            setResult(RESULT_OK, resultIntent);
            finish();
        });
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (map != null) map.onResume();
    }

    @Override
    protected void onPause() {
        super.onPause();
        if (map != null) map.onPause();
    }
}
