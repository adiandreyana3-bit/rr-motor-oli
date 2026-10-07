package com.rrmotor.reminder;

import android.content.Context;
import android.os.Bundle;
import android.util.Base64;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.credentials.CredentialManager;
import androidx.credentials.GetCredentialRequest;
import androidx.credentials.GetCredentialResponse;
import androidx.credentials.exceptions.GetCredentialException;

import com.google.android.libraries.identity.googleid.GetGoogleIdOption;
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential;
import com.google.android.libraries.identity.googleid.GoogleIdTokenParsingException;

import java.security.SecureRandom;

public class LoginActivity extends AppCompatActivity {

    /*
     * ============================================================
     * GANTI DENGAN WEB CLIENT ID GOOGLE CLOUD
     * ============================================================
     *
     * Contoh:
     *
     * 123456789012-xxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxx.apps.googleusercontent.com
     *
     * JANGAN gunakan Android Client ID.
     */
    private static final String WEB_CLIENT_ID =
            "GANTI_DENGAN_WEB_CLIENT_ID_ANDA";

    private CredentialManager credentialManager;

    private TextView tvStatus;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        /*
         * Kalau sebelumnya sudah login,
         * langsung buka MainActivity.
         */
        if (isAlreadyLoggedIn()) {
            bukaMainActivity();
            return;
        }

        buatTampilan();

        credentialManager =
                CredentialManager.create(this);
    }

    // ============================================================
    // TAMPILAN LOGIN
    // ============================================================

    private void buatTampilan() {

        LinearLayout root =
                new LinearLayout(this);

        root.setOrientation(
                LinearLayout.VERTICAL
        );

        root.setPadding(
                50,
                80,
                50,
                50
        );

        root.setGravity(
                android.view.Gravity.CENTER
        );

        TextView title =
                new TextView(this);

        title.setText(
                "🏍️ RR MOTOR"
        );

        title.setTextSize(30);

        title.setTextColor(
                android.graphics.Color.rgb(
                        0,
                        150,
                        70
                )
        );

        title.setGravity(
                android.view.Gravity.CENTER
        );

        TextView subtitle =
                new TextView(this);

        subtitle.setText(
                "REMINDER GANTI OLI"
        );

        subtitle.setTextSize(18);

        subtitle.setGravity(
                android.view.Gravity.CENTER
        );

        subtitle.setPadding(
                0,
                10,
                0,
                40
        );

        tvStatus =
                new TextView(this);

        tvStatus.setText(
                "Silakan login dengan akun Google"
        );

        tvStatus.setTextSize(15);

        tvStatus.setGravity(
                android.view.Gravity.CENTER
        );

        tvStatus.setPadding(
                0,
                10,
                0,
                30
        );

        Button btnGoogle =
                new Button(this);

        btnGoogle.setText(
                "🔐  LOGIN DENGAN GOOGLE"
        );

        btnGoogle.setTextSize(16);

        btnGoogle.setTextColor(
                android.graphics.Color.WHITE
        );

        btnGoogle.setBackgroundColor(
                android.graphics.Color.rgb(
                        0,
                        150,
                        70
                )
        );

        btnGoogle.setAllCaps(false);

        btnGoogle.setOnClickListener(
                v -> loginGoogle()
        );

        root.addView(
                title,
                new LinearLayout.LayoutParams(
                        -1,
                        -2
                )
        );

        root.addView(
                subtitle,
                new LinearLayout.LayoutParams(
                        -1,
                        -2
                )
        );

        root.addView(
                tvStatus,
                new LinearLayout.LayoutParams(
                        -1,
                        -2
                )
        );

        LinearLayout.LayoutParams buttonParams =
                new LinearLayout.LayoutParams(
                        -1,
                        60
                );

        root.addView(
                btnGoogle,
                buttonParams
        );

        setContentView(root);
    }

    // ============================================================
    // LOGIN GOOGLE
    // ============================================================

    private void loginGoogle() {

        if (WEB_CLIENT_ID.startsWith("GANTI_")) {

            Toast.makeText(
                    this,
                    "Web Client ID Google belum diisi.",
                    Toast.LENGTH_LONG
            ).show();

            tvStatus.setText(
                    "Web Client ID Google belum diatur."
            );

            return;
        }

        tvStatus.setText(
                "Memilih akun Google..."
        );

        /*
         * Google Login menggunakan Credential Manager.
         *
         * filter false:
         * menampilkan akun Google yang tersedia.
         */

        GetGoogleIdOption googleIdOption =
                new GetGoogleIdOption.Builder()
                        .setFilterByAuthorizedAccounts(false)
                        .setServerClientId(
                                WEB_CLIENT_ID
                        )
                        .setAutoSelectEnabled(false)
                        .setNonce(
                                buatNonce()
                        )
                        .build();

        GetCredentialRequest request =
                new GetCredentialRequest.Builder()
                        .addCredentialOption(
                                googleIdOption
                        )
                        .build();

        credentialManager.getCredentialAsync(
                this,
                request,
                null,
                Runnable::run,
                new androidx.credentials.CredentialManagerCallback<
                        GetCredentialResponse,
                        GetCredentialException
                        >() {

                    @Override
                    public void onResult(
                            GetCredentialResponse result
                    ) {

                        prosesLoginGoogle(
                                result
                        );
                    }

                    @Override
                    public void onError(
                            GetCredentialException e
                    ) {

                        runOnUiThread(
                                () -> {

                                    tvStatus.setText(
                                            "Login Google dibatalkan/gagal."
                                    );

                                    Toast.makeText(
                                            LoginActivity.this,
                                            "Login Google gagal: "
                                                    + e.getMessage(),
                                            Toast.LENGTH_LONG
                                    ).show();
                                }
                        );
                    }
                }
        );
    }

    // ============================================================
    // PROSES HASIL LOGIN
    // ============================================================

    private void prosesLoginGoogle(
            GetCredentialResponse result
    ) {

        try {

            androidx.credentials.Credential credential =
                    result.getCredential();

            GoogleIdTokenCredential googleCredential =
                    GoogleIdTokenCredential
                            .createFrom(
                                    credential.getData()
                            );

            String nama =
                    googleCredential.getDisplayName();

            String email =
                    googleCredential.getId();

            /*
             * Simpan status login lokal.
             *
             * Kita TIDAK menggunakan Firebase Auth.
             */
            getSharedPreferences(
                    "RR_MOTOR_LOGIN",
                    MODE_PRIVATE
            )
                    .edit()
                    .putBoolean(
                            "sudah_login",
                            true
                    )
                    .putString(
                            "nama",
                            nama == null
                                    ? ""
                                    : nama
                    )
                    .putString(
                            "email",
                            email == null
                                    ? ""
                                    : email
                    )
                    .apply();

            runOnUiThread(
                    () -> {

                        tvStatus.setText(
                                "Login berhasil."
                        );

                        Toast.makeText(
                                LoginActivity.this,
                                "Login Google berhasil.",
                                Toast.LENGTH_SHORT
                        ).show();

                        bukaMainActivity();
                    }
            );

        } catch (
                GoogleIdTokenParsingException e
        ) {

            runOnUiThread(
                    () -> Toast.makeText(
                            LoginActivity.this,
                            "Data Login Google tidak dapat dibaca.",
                            Toast.LENGTH_LONG
                    ).show()
            );

        } catch (Exception e) {

            runOnUiThread(
                    () -> Toast.makeText(
                            LoginActivity.this,
                            "Login gagal: "
                                    + e.getMessage(),
                            Toast.LENGTH_LONG
                    ).show()
            );
        }
    }

    // ============================================================
    // CEK SUDAH LOGIN
    // ============================================================

    private boolean isAlreadyLoggedIn() {

        return getSharedPreferences(
                "RR_MOTOR_LOGIN",
                MODE_PRIVATE
        )
                .getBoolean(
                        "sudah_login",
                        false
                );
    }

    // ============================================================
    // BUKA MAIN ACTIVITY
    // ============================================================

    private void bukaMainActivity() {

        android.content.Intent intent =
                new android.content.Intent(
                        this,
                        MainActivity.class
                );

        startActivity(intent);

        finish();
    }

    // ============================================================
    // NONCE
    // ============================================================

    private String buatNonce() {

        byte[] bytes =
                new byte[32];

        new SecureRandom()
                .nextBytes(bytes);

        return Base64.encodeToString(
                bytes,
                Base64.NO_WRAP
                        | Base64.URL_SAFE
                        | Base64.NO_PADDING
        );
    }
}
