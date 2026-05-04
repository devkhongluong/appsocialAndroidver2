package com.example.appsocialver2.activity;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.example.appsocialver2.R;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.HashMap;

public class DangKi extends AppCompatActivity {
    EditText edtEmail, edtOTP, edtPass, edtPass2, edtTen;
    Button btnDangKy, btnOTP;
    TextView errorEmail, errorPass, errorPass2;
    FirebaseAuth mAuth;
    FirebaseFirestore db;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_dangki);

        edtEmail = findViewById(R.id.edtEmail);
        edtOTP = findViewById(R.id.ed_OTP);
        edtPass = findViewById(R.id.edtPass);
        edtPass2 = findViewById(R.id.ed_pass2);
        btnDangKy = findViewById(R.id.btnDangKy);
        btnOTP = findViewById(R.id.btnOTP);
        errorEmail = findViewById(R.id.errorEmail);
        errorPass = findViewById(R.id.errorPass);
        errorPass2 = findViewById(R.id.errorPass2);
        edtTen = findViewById(R.id.edtTen);
        //ẩn đi cái lỗi
        errorEmail.setVisibility(View.GONE);
        errorPass.setVisibility(View.GONE);
        errorPass2.setVisibility(View.GONE);
        mAuth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();

        btnDangKy.setOnClickListener(v -> {
            registerUser();
        });
        btnOTP.setOnClickListener(v -> {
            sendOTP();
        });

    }
    private void registerUser() {
        if (!validateInput()) return;
        String email = edtEmail.getText().toString().trim();
        String otp = edtOTP.getText().toString().trim();
        String pass = edtPass.getText().toString().trim();
        db.collection("otp")
                .document(email)
                .get()
                .addOnSuccessListener(documentSnapshot -> {

                    if (documentSnapshot.exists()) {
                        String otpServer = documentSnapshot.getString("code");
                        if (otpServer == null) {
                            showError(errorEmail, "Lỗi OTP, vui lòng gửi lại mã");
                            return;
                        }
                        if (!otp.equals(otpServer)) {
                            showError(errorEmail, "Mã xác thực OTP không đúng. Vui lòng nhập lại");
                            return;
                        }
                        createAccount(email, pass);
                    } else {
                        showError(errorEmail, "Vui lòng gửi mã OTP trước");
                    }
                });
    }
    private void createAccount(String email, String pass) {

        String tendn = edtTen.getText().toString().trim();
        mAuth.createUserWithEmailAndPassword(email, pass)
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {

                        String userId = mAuth.getCurrentUser().getUid();

                        HashMap<String, Object> userMap = new HashMap<>();
                        userMap.put("email", email);
                        userMap.put("role", "user");
                        userMap.put("tendn", tendn);

                        db.collection("users").document(userId).set(userMap)
                                .addOnSuccessListener(unused -> {
                                    db.collection("otp").document(email).delete();
                                    Toast.makeText(this, "Đăng ký thành công", Toast.LENGTH_SHORT).show();
                                    Intent intent = new Intent(this, DangNhapActivity.class);
                                    startActivity(intent);
                                    finish();
                                })
                                .addOnFailureListener(e -> {
                                    Toast.makeText(this, "Lưu thông tin thất bại: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                                });
                    } else {
                        Exception e = task.getException();
                        Toast.makeText(this, "Lỗi: " + e.getMessage(), Toast.LENGTH_LONG).show();
                    }
                });
    }
    private boolean validateInput() {

        String email = edtEmail.getText().toString().trim();
        String otp = edtOTP.getText().toString().trim();
        String pass = edtPass.getText().toString().trim();
        String pass2 = edtPass2.getText().toString().trim();
        String tendn = edtTen.getText().toString().trim();

        hideAllErrors();

        if (email.isEmpty() || otp.isEmpty() || pass.isEmpty() || pass2.isEmpty() || tendn.isEmpty()) {
            Toast.makeText(this, "Vui lòng nhập đầy đủ thông tin", Toast.LENGTH_LONG).show();
            return false;
        }

        if (!android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            showError(errorEmail, "Email không hợp lệ");
            return false;
        }

        if (!pass.equals(pass2)) {
            showError(errorPass2, "Mật khẩu không khớp");
            return false;
        }
        if (pass.length() < 6) {
            showError(errorPass, "Mật khẩu phải lớn hơn 6 ký tự");
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
        errorPass2.setVisibility(View.GONE);
    }
    private void sendOTP() {

        String email = edtEmail.getText().toString().trim();

        hideAllErrors();

        if (email.isEmpty()) {
            showError(errorEmail, "Vui lòng nhập email");
            return;
        }

        if (!android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            showError(errorEmail, "Email không hợp lệ");
            return;
        }

        // email đã tồn tại chưa (Firebase Auth)
        mAuth.fetchSignInMethodsForEmail(email)
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful() && task.getResult() != null) {
                        boolean isNewUser = task.getResult().getSignInMethods().isEmpty();
                        if (!isNewUser) {
                            showError(errorEmail, "Email đã tồn tại");
                        } else {
                            generateAndSaveOTP(email);
                        }
                    }
                })
                .addOnFailureListener(e -> {
            Toast.makeText(this, "Lỗi: " + e.getMessage(), Toast.LENGTH_LONG).show();
        });
    }
    private void generateAndSaveOTP(String email) {

        String otp = String.valueOf((int)(Math.random() * 900000) + 100000);

        HashMap<String, Object> otpMap = new HashMap<>();
        otpMap.put("code", otp);
        otpMap.put("time", System.currentTimeMillis());

        db.collection("otp")
                .document(email)
                .set(otpMap)
                .addOnSuccessListener(unused -> {
                    Toast.makeText(this, "Mã OTP: " + otp, Toast.LENGTH_LONG).show();
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(this, "Lỗi gửi OTP", Toast.LENGTH_SHORT).show();
                });
    }
}
