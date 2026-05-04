package com.example.appsocialver2.activity;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.example.appsocialver2.R;
import com.google.firebase.auth.FirebaseAuth;

public class DangNhapActivity extends AppCompatActivity {
    EditText edtEmail, edtPass;
    Button btnDangNhap, btnDangKy;
    TextView errorEmail, errorPass;

    FirebaseAuth mAuth;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_dangnhap);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });
        edtEmail = findViewById(R.id.edtEmail);
        edtPass = findViewById(R.id.edtPass);
        btnDangNhap = findViewById(R.id.btnDangNhap);
        errorEmail = findViewById(R.id.errorEmail);
        errorPass = findViewById(R.id.errorPass);
        btnDangKy = findViewById(R.id.btnDangKy);

        errorEmail.setVisibility(View.GONE);
        errorPass.setVisibility(View.GONE);

        mAuth = FirebaseAuth.getInstance();

        btnDangNhap.setOnClickListener(v -> {
            loginUser();
        });
        btnDangKy.setOnClickListener(v -> {
            Intent intent = new Intent(this, DangKi.class);
            startActivity(intent);
        });
    }
    private void loginUser() {

        if (!validateInput()) return;

        String email = edtEmail.getText().toString().trim();
        String pass = edtPass.getText().toString().trim();

        mAuth.signInWithEmailAndPassword(email, pass)
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {

                        Toast.makeText(this, "Đăng nhập thành công", Toast.LENGTH_SHORT).show();
                        Intent intent = new Intent(this, MainActivity.class);
                        startActivity(intent);

                    } else {
                        showError(errorPass, "Email hoặc mật khẩu không đúng");
                    }
                });
    }
    private boolean validateInput() {

        String email = edtEmail.getText().toString().trim();
        String pass = edtPass.getText().toString().trim();

        hideAllErrors();

        if (email.isEmpty() || pass.isEmpty()) {
            showError(errorEmail, "Vui lòng nhập đầy đủ thông tin");
            return false;
        }

        if (!android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            showError(errorEmail, "Email không hợp lệ");
            return false;
        }

        return true;
    }

    private void showError(TextView tv, String message) {
        tv.setText(message);
        tv.setVisibility(View.VISIBLE);
    }

    private void hideAllErrors() {
        errorEmail.setVisibility(View.GONE);
        errorPass.setVisibility(View.GONE);
    }
}