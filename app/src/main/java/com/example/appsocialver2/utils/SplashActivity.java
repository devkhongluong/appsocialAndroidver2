package com.example.appsocialver2.utils;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;

import androidx.appcompat.app.AppCompatActivity;

import com.example.appsocialver2.R;
import com.example.appsocialver2.activity.DangNhapActivity;
import com.example.appsocialver2.activity.MainActivity;
import com.google.firebase.auth.FirebaseAuth;

public class SplashActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.logo);

        new Handler().postDelayed(() -> {

            if (FirebaseAuth.getInstance().getCurrentUser() != null) {
                startActivity(new Intent(this, MainActivity.class));
            } else {
                startActivity(new Intent(this, DangNhapActivity.class));
            }

            finish();

        }, 2000);
    }
}