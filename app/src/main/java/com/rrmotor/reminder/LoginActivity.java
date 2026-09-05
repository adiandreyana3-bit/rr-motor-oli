package com.rrmotor.reminder;

import android.content.Intent;
import android.os.Bundle;
import android.text.InputType;
import android.view.Gravity;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.auth.FirebaseAuth;

public class LoginActivity extends AppCompatActivity {

    private FirebaseAuth auth;

    private EditText emailInput;
    private EditText passwordInput;

    private Button loginButton;
    private Button daftarButton;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        auth = FirebaseAuth.getInstance();

        // Kalau sudah login, langsung masuk ke aplikasi
        if (auth.getCurrentUser() != null) {
            bukaMainActivity();
            return;
        }

        buatTampilan();
    }

    private void buatTampilan() {

        LinearLayout utama = new LinearLayout(this);
        utama.setOrientation(LinearLayout.VERTICAL);
        utama.setPadding(40, 50, 40, 40);
        utama.setGravity(Gravity.CENTER_HORIZONTAL);

        TextView judul = new TextView(this);
        judul.setText("🏍️ RR MOTOR");
        judul.setTextSize(28);
        judul.setGravity(Gravity.CENTER);
        judul.setPadding(0, 0, 0, 10);

        TextView subjudul = new TextView(this);
        subjudul.setText("RR MOTOR REMINDER");
        subjudul.setTextSize(18);
        subjudul.setGravity(Gravity.CENTER);
        subjudul.setPadding(0, 0, 0, 40);

        emailInput = new EditText(this);
        emailInput.setHint("Email");
        emailInput.setSingleLine(true);
        emailInput.setInputType(
                InputType.TYPE_CLASS_TEXT |
                InputType.TYPE_TEXT_VARIATION_EMAIL_ADDRESS
        );

        LinearLayout.LayoutParams inputParams =
                new LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.MATCH_PARENT,
                        LinearLayout.LayoutParams.WRAP_CONTENT
                );

        inputParams.setMargins(0, 10, 0, 10);

        utama.addView(judul);
        utama.addView(subjudul);
        utama.addView(emailInput, inputParams);

        passwordInput = new EditText(this);
        passwordInput.setHint("Password");
        passwordInput.setSingleLine(true);
        passwordInput.setInputType(
                InputType.TYPE_CLASS_TEXT |
                InputType.TYPE_TEXT_VARIATION_PASSWORD
        );

        utama.addView(passwordInput, inputParams);

        loginButton = new Button(this);
        loginButton.setText("LOGIN");
        loginButton.setOnClickListener(v -> login());

        LinearLayout.LayoutParams buttonParams =
                new LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.MATCH_PARENT,
                        LinearLayout.LayoutParams.WRAP_CONTENT
                );

        buttonParams.setMargins(0, 25, 0, 10);

        utama.addView(loginButton, buttonParams);

        daftarButton = new Button(this);
        daftarButton.setText("DAFTAR AKUN BARU");
        daftarButton.setOnClickListener(v -> daftarAkun());

        utama.addView(daftarButton, buttonParams);

        setContentView(utama);
    }

    private void login() {

        String email = emailInput.getText().toString().trim();
        String password = passwordInput.getText().toString();

        if (email.isEmpty()) {
            emailInput.setError("Email wajib diisi");
            emailInput.requestFocus();
            return;
        }

        if (password.isEmpty()) {
            passwordInput.setError("Password wajib diisi");
            passwordInput.requestFocus();
            return;
        }

        loginButton.setEnabled(false);
        daftarButton.setEnabled(false);

        auth.signInWithEmailAndPassword(email, password)
                .addOnCompleteListener(this, task -> {

                    loginButton.setEnabled(true);
                    daftarButton.setEnabled(true);

                    if (task.isSuccessful()) {

                        Toast.makeText(
                                this,
                                "Login berhasil",
                                Toast.LENGTH_SHORT
                        ).show();

                        bukaMainActivity();

                    } else {

                        String pesan = "Login gagal";

                        if (task.getException() != null) {
                            pesan = task.getException().getMessage();
                        }

                        Toast.makeText(
                                this,
                                pesan,
                                Toast.LENGTH_LONG
                        ).show();
                    }
                });
    }

    private void daftarAkun() {

        String email = emailInput.getText().toString().trim();
        String password = passwordInput.getText().toString();

        if (email.isEmpty()) {
            emailInput.setError("Masukkan email");
            emailInput.requestFocus();
            return;
        }

        if (password.isEmpty()) {
            passwordInput.setError("Masukkan password");
            passwordInput.requestFocus();
            return;
        }

        if (password.length() < 6) {
            passwordInput.setError("Password minimal 6 karakter");
            passwordInput.requestFocus();
            return;
        }

        loginButton.setEnabled(false);
        daftarButton.setEnabled(false);

        auth.createUserWithEmailAndPassword(email, password)
                .addOnCompleteListener(this, task -> {

                    loginButton.setEnabled(true);
                    daftarButton.setEnabled(true);

                    if (task.isSuccessful()) {

                        Toast.makeText(
                                this,
                                "Akun berhasil dibuat",
                                Toast.LENGTH_LONG
                        ).show();

                        bukaMainActivity();

                    } else {

                        String pesan = "Pendaftaran gagal";

                        if (task.getException() != null) {
                            pesan = task.getException().getMessage();
                        }

                        Toast.makeText(
                                this,
                                pesan,
                                Toast.LENGTH_LONG
                        ).show();
                    }
                });
    }

    private void bukaMainActivity() {

        Intent intent = new Intent(
                LoginActivity.this,
                MainActivity.class
        );

        startActivity(intent);
        finish();
    }
}
