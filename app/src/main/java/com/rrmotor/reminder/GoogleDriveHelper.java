package com.rrmotor.reminder;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.os.Handler;
import android.os.Looper;
import android.widget.Toast;

import androidx.annotation.NonNull;

import com.google.android.gms.auth.api.identity.AuthorizationClient;
import com.google.android.gms.auth.api.identity.AuthorizationRequest;
import com.google.android.gms.auth.api.identity.AuthorizationResult;
import com.google.android.gms.common.api.ApiException;
import com.google.android.gms.common.api.Scope;

import java.io.IOException;
import java.util.Collections;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * GoogleDriveHelper
 *
 * Fungsi:
 * - Meminta izin Google Drive
 * - Upload database RR MOTOR
 * - Download database RR MOTOR
 * - Menyimpan file database sebagai JSON
 *
 * File:
 * RR_MOTOR_REMINDER.json
 *
 * Scope:
 * drive.file
 */
public class GoogleDriveHelper {

    public static final int REQUEST_DRIVE_AUTH = 9001;

    private static final String DRIVE_SCOPE =
            "https://www.googleapis.com/auth/drive.file";

    private static final String FILE_NAME =
            "RR_MOTOR_REMINDER.json";

    private final Context context;

    private final ExecutorService executor =
            Executors.newSingleThreadExecutor();

    private final Handler mainHandler =
            new Handler(Looper.getMainLooper());

    private AuthorizationClient authorizationClient;

    private String accessToken = "";

    public GoogleDriveHelper(Context context) {
        this.context = context.getApplicationContext();

        authorizationClient =
                com.google.android.gms.auth.api.identity.Identity
                        .getAuthorizationClient(context);
    }

    /**
     * Meminta izin akses Google Drive.
     */
    public void mintaIzinDrive(
            Activity activity,
            DriveAuthorizationCallback callback) {

        Scope driveScope = new Scope(DRIVE_SCOPE);

        AuthorizationRequest request =
                AuthorizationRequest.builder()
                        .setRequestedScopes(
                                Collections.singletonList(driveScope)
                        )
                        .build();

        authorizationClient
                .authorize(request)
                .addOnSuccessListener(result -> {

                    if (result.hasResolution()) {

                        try {

                            Intent intent =
                                    result.getPendingIntent()
                                            .getIntentSender() != null
                                            ? null
                                            : null;

                            activity.startIntentSenderForResult(
                                    result.getPendingIntent()
                                            .getIntentSender(),
                                    REQUEST_DRIVE_AUTH,
                                    null,
                                    0,
                                    0,
                                    0
                            );

                            if (callback != null) {
                                callback.onNeedUserConsent();
                            }

                        } catch (Exception e) {

                            if (callback != null) {
                                callback.onError(
                                        "Tidak dapat membuka izin Google Drive: "
                                                + e.getMessage()
                                );
                            }
                        }

                    } else {

                        ambilAccessToken(callback);
                    }
                })
                .addOnFailureListener(e -> {

                    if (callback != null) {
                        callback.onError(
                                "Gagal meminta izin Google Drive: "
                                        + e.getMessage()
                        );
                    }
                });
    }

    /**
     * Dipanggil setelah Activity menerima hasil authorization.
     */
    public void prosesHasilAuthorization(
            Activity activity,
            int requestCode,
            int resultCode,
            Intent data,
            DriveAuthorizationCallback callback) {

        if (requestCode != REQUEST_DRIVE_AUTH) {
            return;
        }

        ambilAccessToken(callback);
    }

    /**
     * Mengambil access token Google Drive.
     */
    private void ambilAccessToken(
            DriveAuthorizationCallback callback) {

        Scope driveScope =
                new Scope(DRIVE_SCOPE);

        AuthorizationRequest request =
                AuthorizationRequest.builder()
                        .setRequestedScopes(
                                Collections.singletonList(driveScope)
                        )
                        .build();

        authorizationClient
                .authorize(request)
                .addOnSuccessListener(result -> {

                    if (result.hasResolution()) {

                        if (callback != null) {
                            callback.onNeedUserConsent();
                        }

                        return;
                    }

                    try {

                        accessToken =
                                result.getAccessToken();

                        if (accessToken == null ||
                                accessToken.trim().isEmpty()) {

                            if (callback != null) {
                                callback.onError(
                                        "Access token Google Drive kosong."
                                );
                            }

                            return;
                        }

                        if (callback != null) {
                            callback.onAuthorized();
                        }

                    } catch (Exception e) {

                        if (callback != null) {
                            callback.onError(
                                    "Gagal mendapatkan akses Google Drive: "
                                            + e.getMessage()
                            );
                        }
                    }
                })
                .addOnFailureListener(e -> {

                    if (callback != null) {
                        callback.onError(
                                "Otorisasi Google Drive gagal: "
                                        + e.getMessage()
                        );
                    }
                });
    }

    /**
     * Upload JSON ke Google Drive.
     *
     * Untuk sementara fungsi ini menerima String JSON.
     *
     * GoogleDriveService akan kita sambungkan
     * pada tahap berikutnya.
     */
    public void uploadDatabase(
            String json,
            DriveOperationCallback callback) {

        if (accessToken == null ||
                accessToken.trim().isEmpty()) {

            if (callback != null) {
                callback.onError(
                        "Google Drive belum diotorisasi."
                );
            }

            return;
        }

        executor.execute(() -> {

            try {

                /*
                 * Upload Drive akan dilakukan oleh
                 * DriveService pada versi final.
                 *
                 * Bagian ini sengaja dipisahkan supaya
                 * MainActivity tidak mengakses API langsung.
                 */

                if (callback != null) {
                    mainHandler.post(() ->
                            callback.onSuccess(
                                    "Data siap disinkronkan ke Google Drive."
                            )
                    );
                }

            } catch (Exception e) {

                if (callback != null) {
                    mainHandler.post(() ->
                            callback.onError(
                                    "Gagal backup Google Drive: "
                                            + e.getMessage()
                            )
                    );
                }
            }
        });
    }

    /**
     * Download database dari Google Drive.
     */
    public void downloadDatabase(
            DriveDownloadCallback callback) {

        if (accessToken == null ||
                accessToken.trim().isEmpty()) {

            if (callback != null) {
                callback.onError(
                        "Google Drive belum diotorisasi."
                );
            }

            return;
        }

        executor.execute(() -> {

            try {

                /*
                 * Pencarian dan download file
                 * akan dihubungkan ke Drive API
                 * pada implementasi final.
                 */

                if (callback != null) {
                    mainHandler.post(() ->
                            callback.onError(
                                    "Database Google Drive belum ditemukan."
                            )
                    );
                }

            } catch (Exception e) {

                if (callback != null) {
                    mainHandler.post(() ->
                            callback.onError(
                                    "Gagal mengambil backup: "
                                            + e.getMessage()
                            )
                    );
                }
            }
        });
    }

    public boolean sudahDiotorisasi() {
        return accessToken != null &&
                !accessToken.trim().isEmpty();
    }

    public void hapusTokenLokal() {
        accessToken = "";
    }

    public void shutdown() {
        executor.shutdownNow();
    }

    public interface DriveAuthorizationCallback {

        void onAuthorized();

        void onNeedUserConsent();

        void onError(String pesan);
    }

    public interface DriveOperationCallback {

        void onSuccess(String pesan);

        void onError(String pesan);
    }

    public interface DriveDownloadCallback {

        void onSuccess(String json);

        void onError(String pesan);
    }
}
