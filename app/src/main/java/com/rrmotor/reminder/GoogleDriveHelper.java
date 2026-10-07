package com.rrmotor.reminder;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.os.Handler;
import android.os.Looper;

import com.google.android.gms.auth.api.identity.AuthorizationClient;
import com.google.android.gms.auth.api.identity.AuthorizationRequest;
import com.google.android.gms.auth.api.identity.AuthorizationResult;
import com.google.android.gms.auth.api.identity.Identity;
import com.google.android.gms.common.api.Scope;
import com.google.api.client.googleapis.extensions.android.gms.auth.GoogleAccountCredential;
import com.google.api.client.http.AbstractInputStreamContent;
import com.google.api.client.http.ByteArrayContent;
import com.google.api.client.http.HttpRequest;
import com.google.api.client.http.HttpRequestInitializer;
import com.google.api.client.googleapis.javanet.GoogleNetHttpTransport;
import com.google.api.client.json.gson.GsonFactory;
import com.google.api.services.drive.Drive;
import com.google.api.services.drive.DriveScopes;
import com.google.api.services.drive.model.File;
import com.google.api.services.drive.model.FileList;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class GoogleDriveHelper {

    public static final int REQUEST_DRIVE_AUTH = 9001;

    private static final String DRIVE_SCOPE =
            DriveScopes.DRIVE_FILE;

    private static final String FILE_NAME =
            "RR_MOTOR_REMINDER.json";

    private static final String MIME_TYPE =
            "application/json";

    private static final String PREF_NAME =
            "RR_MOTOR_DRIVE";

    private static final String KEY_FILE_ID =
            "drive_file_id";

    private final Context context;

    private final Handler mainHandler =
            new Handler(Looper.getMainLooper());

    private final ExecutorService executor =
            Executors.newSingleThreadExecutor();

    private AuthorizationClient authorizationClient;

    private Drive driveService;

    private String accessToken = "";

    public GoogleDriveHelper(Context context) {

        this.context =
                context.getApplicationContext();

        authorizationClient =
                Identity.getAuthorizationClient(context);
    }

    // ============================================================
    // OTORISASI GOOGLE DRIVE
    // ============================================================

    public void mintaIzinDrive(
            Activity activity,
            DriveAuthorizationCallback callback) {

        Scope scope =
                new Scope(DRIVE_SCOPE);

        AuthorizationRequest request =
                AuthorizationRequest.builder()
                        .setRequestedScopes(
                                Collections.singletonList(scope)
                        )
                        .build();

        authorizationClient
                .authorize(request)
                .addOnSuccessListener(result -> {

                    if (result.hasResolution()) {

                        try {

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

                        prosesAuthorizationResult(
                                result,
                                callback
                        );
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

    public void prosesHasilAuthorization(
            Activity activity,
            int requestCode,
            int resultCode,
            Intent data,
            DriveAuthorizationCallback callback) {

        if (requestCode != REQUEST_DRIVE_AUTH) {
            return;
        }

        mintaIzinDrive(
                activity,
                callback
        );
    }

    private void prosesAuthorizationResult(
            AuthorizationResult result,
            DriveAuthorizationCallback callback) {

        String token =
                result.getAccessToken();

        if (token == null ||
                token.trim().isEmpty()) {

            if (callback != null) {
                callback.onError(
                        "Access token Google Drive kosong."
                );
            }

            return;
        }

        accessToken = token;

        simpanAccessToken(token);

        buatDriveService(
                token,
                callback
        );
    }

    // ============================================================
    // DRIVE SERVICE
    // ============================================================

    private void buatDriveService(
            String token,
            DriveAuthorizationCallback callback) {

        executor.execute(() -> {

            try {

                final Drive service =
                        new Drive.Builder(
                                GoogleNetHttpTransport.newTrustedTransport(),
                                GsonFactory.getDefaultInstance(),
                                request -> {

                                    request.getHeaders()
                                            .setAuthorization(
                                                    "Bearer " + token
                                            );
                                }
                        )
                                .setApplicationName(
                                        "RR MOTOR REMINDER"
                                )
                                .build();

                driveService = service;

                mainHandler.post(() -> {

                    if (callback != null) {
                        callback.onAuthorized();
                    }
                });

            } catch (
                    GeneralSecurityException |
                    IOException e) {

                mainHandler.post(() -> {

                    if (callback != null) {
                        callback.onError(
                                "Gagal membuat koneksi Google Drive: "
                                        + e.getMessage()
                        );
                    }
                });
            }
        });
    }

    // ============================================================
    // BACKUP / UPLOAD
    // ============================================================

    public void uploadDatabase(
            String json,
            DriveOperationCallback callback) {

        if (json == null) {
            json = "[]";
        }

        final String finalJson = json;

        executor.execute(() -> {

            try {

                Drive service =
                        pastikanDriveService();

                if (service == null) {

                    kirimError(
                            callback,
                            "Google Drive belum diotorisasi."
                    );

                    return;
                }

                String fileId =
                        getSavedFileId();

                if (fileId == null ||
                        fileId.trim().isEmpty()) {

                    fileId =
                            cariFileDatabase(service);
                }

                byte[] bytes =
                        finalJson.getBytes(
                                StandardCharsets.UTF_8
                        );

                AbstractInputStreamContent media =
                        new ByteArrayContent(
                                MIME_TYPE,
                                bytes
                        );

                File metadata =
                        new File();

                metadata.setName(
                        FILE_NAME
                );

                if (fileId == null ||
                        fileId.trim().isEmpty()) {

                    // ------------------------------------------------
                    // FILE BELUM ADA → BUAT FILE BARU
                    // ------------------------------------------------

                    File hasil =
                            service.files()
                                    .create(
                                            metadata,
                                            media
                                    )
                                    .setFields(
                                            "id,name,mimeType,modifiedTime"
                                    )
                                    .execute();

                    if (hasil != null &&
                            hasil.getId() != null) {

                        simpanFileId(
                                hasil.getId()
                        );

                        kirimSukses(
                                callback,
                                "Backup berhasil disimpan ke Google Drive."
                        );

                    } else {

                        kirimError(
                                callback,
                                "File Google Drive gagal dibuat."
                        );
                    }

                } else {

                    // ------------------------------------------------
                    // FILE SUDAH ADA → UPDATE FILE
                    // ------------------------------------------------

                    File hasil =
                            service.files()
                                    .update(
                                            fileId,
                                            metadata,
                                            media
                                    )
                                    .setFields(
                                            "id,name,mimeType,modifiedTime"
                                    )
                                    .execute();

                    if (hasil != null) {

                        simpanFileId(
                                hasil.getId()
                        );

                        kirimSukses(
                                callback,
                                "Backup berhasil diperbarui."
                        );

                    } else {

                        kirimError(
                                callback,
                                "Backup gagal diperbarui."
                        );
                    }
                }

            } catch (Exception e) {

                kirimError(
                        callback,
                        "Gagal backup Google Drive: "
                                + e.getMessage()
                );
            }
        });
    }

    // ============================================================
    // RESTORE / DOWNLOAD
    // ============================================================

    public void downloadDatabase(
            DriveDownloadCallback callback) {

        executor.execute(() -> {

            try {

                Drive service =
                        pastikanDriveService();

                if (service == null) {

                    kirimDownloadError(
                            callback,
                            "Google Drive belum diotorisasi."
                    );

                    return;
                }

                String fileId =
                        getSavedFileId();

                if (fileId == null ||
                        fileId.trim().isEmpty()) {

                    fileId =
                            cariFileDatabase(service);
                }

                if (fileId == null ||
                        fileId.trim().isEmpty()) {

                    kirimDownloadError(
                            callback,
                            "Backup RR MOTOR belum ditemukan di Google Drive."
                    );

                    return;
                }

                InputStream inputStream =
                        service.files()
                                .get(fileId)
                                .executeMediaAsInputStream();

                String json =
                        bacaInputStream(inputStream);

                simpanFileId(fileId);

                kirimDownloadSukses(
                        callback,
                        json
                );

            } catch (Exception e) {

                kirimDownloadError(
                        callback,
                        "Gagal restore Google Drive: "
                                + e.getMessage()
                );
            }
        });
    }

    // ============================================================
    // CARI FILE
    // ============================================================

    private String cariFileDatabase(
            Drive service) throws IOException {

        String query =
                "name = '" +
                        FILE_NAME +
                        "' and trashed = false";

        FileList result =
                service.files()
                        .list()
                        .setQ(query)
                        .setSpaces("drive")
                        .setFields(
                                "files(id,name,mimeType,modifiedTime)"
                        )
                        .setPageSize(10)
                        .execute();

        List<File> files =
                result.getFiles();

        if (files == null ||
                files.isEmpty()) {

            return null;
        }

        /*
         * Kalau ada beberapa file dengan nama sama,
         * ambil file yang pertama.
         */
        File file =
                files.get(0);

        if (file == null) {
            return null;
        }

        String id =
                file.getId();

        if (id != null) {
            simpanFileId(id);
        }

        return id;
    }

    // ============================================================
    // PASTIKAN SERVICE
    // ============================================================

    private synchronized Drive pastikanDriveService()
            throws Exception {

        if (driveService != null) {
            return driveService;
        }

        String token =
                accessToken;

        if (token == null ||
                token.trim().isEmpty()) {

            token =
                    ambilAccessTokenLokal();
        }

        if (token == null ||
                token.trim().isEmpty()) {

            return null;
        }

        accessToken =
                token;

        driveService =
                new Drive.Builder(
                        GoogleNetHttpTransport.newTrustedTransport(),
                        GsonFactory.getDefaultInstance(),
                        request -> {

                            request.getHeaders()
                                    .setAuthorization(
                                            "Bearer " + token
                                    );
                        }
                )
                        .setApplicationName(
                                "RR MOTOR REMINDER"
                        )
                        .build();

        return driveService;
    }

    // ============================================================
    // TOKEN LOKAL
    // ============================================================

    private void simpanAccessToken(
            String token) {

        context.getSharedPreferences(
                PREF_NAME,
                Context.MODE_PRIVATE
        )
                .edit()
                .putString(
                        "access_token",
                        token
                )
                .apply();
    }

    private String ambilAccessTokenLokal() {

        return context
                .getSharedPreferences(
                        PREF_NAME,
                        Context.MODE_PRIVATE
                )
                .getString(
                        "access_token",
                        ""
                );
    }

    // ============================================================
    // FILE ID
    // ============================================================

    private void simpanFileId(
            String fileId) {

        if (fileId == null) {
            return;
        }

        context.getSharedPreferences(
                PREF_NAME,
                Context.MODE_PRIVATE
        )
                .edit()
                .putString(
                        KEY_FILE_ID,
                        fileId
                )
                .apply();
    }

    private String getSavedFileId() {

        return context
                .getSharedPreferences(
                        PREF_NAME,
                        Context.MODE_PRIVATE
                )
                .getString(
                        KEY_FILE_ID,
                        ""
                );
    }

    // ============================================================
    // INPUT STREAM → STRING
    // ============================================================

    private String bacaInputStream(
            InputStream inputStream)
            throws IOException {

        ByteArrayOutputStream output =
                new ByteArrayOutputStream();

        byte[] buffer =
                new byte[8192];

        int jumlah;

        while ((jumlah =
                inputStream.read(buffer)) != -1) {

            output.write(
                    buffer,
                    0,
                    jumlah
            );
        }

        inputStream.close();

        return output.toString(
                StandardCharsets.UTF_8.name()
        );
    }

    // ============================================================
    // CALLBACK
    // ============================================================

    private void kirimSukses(
            DriveOperationCallback callback,
            String pesan) {

        if (callback == null) {
            return;
        }

        mainHandler.post(() ->
                callback.onSuccess(pesan)
        );
    }

    private void kirimError(
            DriveOperationCallback callback,
            String pesan) {

        if (callback == null) {
            return;
        }

        mainHandler.post(() ->
                callback.onError(pesan)
        );
    }

    private void kirimDownloadSukses(
            DriveDownloadCallback callback,
            String json) {

        if (callback == null) {
            return;
        }

        mainHandler.post(() ->
                callback.onSuccess(json)
        );
    }

    private void kirimDownloadError(
            DriveDownloadCallback callback,
            String pesan) {

        if (callback == null) {
            return;
        }

        mainHandler.post(() ->
                callback.onError(pesan)
        );
    }

    // ============================================================
    // CEK STATUS
    // ============================================================

    public boolean sudahDiotorisasi() {

        return accessToken != null &&
                !accessToken.trim().isEmpty();
    }

    public boolean mempunyaiFileBackup() {

        String fileId =
                getSavedFileId();

        return fileId != null &&
                !fileId.trim().isEmpty();
    }

    public void hapusTokenLokal() {

        accessToken = "";
        driveService = null;

        context.getSharedPreferences(
                PREF_NAME,
                Context.MODE_PRIVATE
        )
                .edit()
                .remove("access_token")
                .remove(KEY_FILE_ID)
                .apply();
    }

    public void shutdown() {

        executor.shutdownNow();
    }

    // ============================================================
    // INTERFACE
    // ============================================================

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
