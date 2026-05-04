package com.example.appsocialver2.activity;

import static androidx.core.content.ContextCompat.getSystemService;

import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Bundle;
import androidx.appcompat.app.AppCompatActivity;

public abstract class BaseSensorActivity extends AppCompatActivity implements SensorEventListener {
    protected SensorManager sensorManager;
    protected Sensor proximitySensor;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        sensorManager = (SensorManager) getSystemService(SENSOR_SERVICE);
        if (sensorManager != null) {
            proximitySensor = sensorManager.getDefaultSensor(Sensor.TYPE_PROXIMITY);
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (proximitySensor != null) {
            sensorManager.registerListener(this, proximitySensor, SensorManager.SENSOR_DELAY_UI);
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        sensorManager.unregisterListener(this);
    }

    @Override
    public void onSensorChanged(SensorEvent event) {
        if (event.sensor.getType() == Sensor.TYPE_PROXIMITY) {
            // Nếu khoảng cách bé hơn range tối đa (bị che) [cite: 115, 116]
            boolean isCovered = event.values[0] < proximitySensor.getMaximumRange();
            onPrivacyTriggered(isCovered);
        }
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {}

    // Phương thức để các Activity con (như MainActivity) triển khai giao diện ẩn nội dung [cite: 151, 153]
    protected abstract void onPrivacyTriggered(boolean isCovered);
}