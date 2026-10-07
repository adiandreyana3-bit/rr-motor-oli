package com.rrmotor.reminder;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.os.CancellationSignal;
import android.view.Gravity;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.credentials.Credential;
import androidx.credentials.CredentialManager;
import androidx.credentials.CredentialManagerCallback;
import androidx.credentials.GetCredentialRequest;
import androidx.credentials.GetCredentialResponse;
import androidx.credentials.exceptions.GetCredentialException;

import com.google.android.libraries.identity.googleid.GetGoogleIdOption;
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential;
import com.google.android.libraries.identity.googleid.GoogleIdTokenParsingException;

import java.util.concurrent.Executors;

public class LoginActivity extends AppCompatActivity {

    private CredentialManager credentialManager;

    private Button loginGoogleButton;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        credentialManager = CredentialManager.create(this);

        buatTampilan();
    }

    private void buatTampilan() {

        LinearLayout utama = new LinearLayout(this);
        utama.setOrientation(LinearLayout.VERTICAL);
        utama.setGravity(Gravity.CENTER_HORIZONTAL);
        utama.setPadding(40, 60, 40, 40);

        TextView judul = new TextView(this);
        judul.setText("🏍️ RR MOTOR");
        judul.setTextSize(28);
        judul.setGravity(Gravity.CENTER);
        judul.setPadding(0, 0, 0, 10);

        TextView subjudul = new TextView(this);
        subjudul.setText("RR MOTOR REMINDER");
        subjudul.setTextSize(18);
        subjudul.setGravity(Gravity.CENTER);
        subjudul.setPadding(0, 0, 0, 15);

        TextView keterangan = new TextView(this);
        keterangan.setText(
                "Login menggunakan akun Google.\n\n" +
                "Data reminder akan disimpan dan dicadangkan " +
                "ke Google Drive."
        );
        keterangan.setTextSize(15);
        keterangan.setGravity(Gravity.CENTER);
        keterangan.setPadding(10, 10, 10, 30);

        loginGoogleButton = new Button(this);
        loginGoogleButton.setText("🔵 LOGIN DENGAN GOOGLE");
        loginGoogleButton.setTextSize(16);

        loginGoogleButton.setOnClickListener(v -> loginDenganGoogle());

        LinearLayout.LayoutParams buttonParams =
                new LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.MATCH_PARENT,
                        LinearLayout.LayoutParams.WRAP_CONTENT
                );

        buttonParams.setMargins(0, 20, 0, 10);

        utama.addView(judul);
        utama.addView(subjudul);
        utama.addView(keterangan);
        utama.addView(loginGoogleButton, buttonParams);

        setContentView(utama);
    }

    private void loginDenganGoogle() {

        loginGoogleButton.setEnabled(false);

        String serverClientId = getString(
                R.string.default_web_client_id
        );

        GetGoogleIdOption googleIdOption =
                new GetGoogleIdOption.Builder()
                        .setServerClientId(serverClientId)
                        .setFilterByAuthorizedAccounts(false)
                        .setAutoSelectEnabled(false)
                        .build();

        GetCredentialRequest request =
                new GetCredentialRequest.Builder()
                        .addCredentialOption(googleIdOption)
                        .build();

        CancellationSignal cancellationSignal =
                new CancellationSignal();

        credentialManager.getCredentialAsync(
                this,
                request,
                cancellationSignal,
                Executors.newSingleThreadExecutor(),
                new CredentialManagerCallback<
                        GetCredentialResponse,
                        GetCredentialException>() {

                    @Override
                    public void onResult(
                            GetCredentialResponse result) {

                        runOnUiThread(() -> {

                            try {

                                Credential credential =
                                        result.getCredential();

                                if (credential == null) {
                                    loginGagal(
                                            "Data login Google tidak ditemukan."
                                    );
                                    return;
                                }

                                if (credential.getData() == null) {
                                    loginGagal(
                                            "Data akun Google tidak tersedia."
                                    );
                                    return;
                                }

                                GoogleIdTokenCredential googleCredential =
                                        GoogleIdTokenCredential
                                                .createFrom(
                                                        credential.getData()
                                                );

                                String nama =
                                        googleCredential.getDisplayName();

                                String email =
                                        googleCredential.getId();

                                if (nama == null ||
                                        nama.trim().isEmpty()) {
                                    nama = "Pengguna Google";
                                }

                                if (email == null ||
                                        email.trim().isEmpty()) {
                                    email = "";
                                }

                                simpanLoginGoogle(
                                        nama,
                                        email
                                );

                            } catch (
                                    GoogleIdTokenParsingException e) {

                                loginGagal(
                                        "Data akun Google tidak dapat dibaca."
                                );
                            } catch (Exception e) {

                                loginGagal(
                                        "Login Google gagal: "
                                                + e.getMessage()
                                );
                            }
                        });
                    }

                    @Override
                    public void onError(
                            @NonNull GetCredentialException e) {

                        runOnUiThread(() -> {

                            loginGoogleButton.setEnabled(true);

                            Toast.makeText(
                                    LoginActivity.this,
                                    "Login Google dibatalkan atau gagal.",
                                    Toast.LENGTH_LONG
                            ).show();
                        });
                    }
                }
        );
    }

    private void simpanLoginGoogle(
            String nama,
            String email) {

        getSharedPreferences(
                "RR_MOTOR_LOGIN",
                MODE_PRIVATE
        )
                .edit()
                .putBoolean("sudah_login", true)
                .putString("nama_google", nama)
                .putString("email_google", email)
                .apply();

        Toast.makeText(
                this,
                "Login Google berhasil.",
                Toast.LENGTH_SHORT
        ).show();

        bukaMainActivity();
    }

    private void loginGagal(String pesan) {

        loginGoogleButton.setEnabled(true);

        Toast.makeText(
                this,
                pesan,
                Toast.LENGTH_LONG
        ).show();
    }

    private void bukaMainActivity() {

        Intent intent =
                new Intent(
                        LoginActivity.this,
                        MainActivity.class
                );

        intent.addFlags(
                Intent.FLAG_ACTIVITY_CLEAR_TOP |
                Intent.FLAG_ACTIVITY_NEW_TASK |
                Intent.FLAG_ACTIVITY_CLEAR_TASK
        );

        startActivity(intent);
        finish();
    }
}
