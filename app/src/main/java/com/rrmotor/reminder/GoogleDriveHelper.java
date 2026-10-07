package com.rrmotor.reminder;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.os.Handler;
import android.os.Looper;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.IntentSenderRequest;

import com.google.android.gms.auth.api.identity.AuthorizationRequest;
import com.google.android.gms.auth.api.identity.AuthorizationResult;
import com.google.android.gms.auth.api.identity.Identity;
import com.google.android.gms.auth.api.identity.AuthorizationClient;
import com.google.android.gms.common.api.ApiException;
import com.google.android.gms.common.api.Scope;
import com.google.api.client.googleapis.extensions.android.gms.auth.UserRecoverableAuthIOException;
import com.google.api.client.http.ByteArrayContent;
import com.google.api.client.http.HttpRequestInitializer;
import com.google.api.client.http.javanet.NetHttpTransport;
import com.google.api.client.json.gson.GsonFactory;
import com.google.api.services.drive.Drive;
import com.google.api.services.drive.DriveScopes;

import java.io.ByteArrayOutputStream;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class GoogleDriveHelper {

    private static final String FILE_NAME =
            "RR_MOTOR_REMINDER.json";

    private static final String MIME_TYPE =
            "application/json";

    private final Context context;

    private final ExecutorService executor =
            Executors.newSingleThreadExecutor();

    private final Handler mainHandler =
            new Handler(Looper.getMainLooper());

    private Drive driveService;

    private DriveCallback pendingAuthorizationCallback;

    public GoogleDriveHelper(Context context) {
        this.context = context.getApplicationContext();
    }

    // ============================================================
    // CALLBACK
    // ============================================================

    public interface DriveCallback {

        void onSuccess();

        void onError(Exception e);
    }

    public interface DataCallback {

        void onSuccess(String json);

        void onError(Exception e);
    }

    // ============================================================
    // AUTHORIZATION
    // ============================================================

    public void mintaIzinDrive(
            Activity activity,
            DriveCallback callback
    ) {

        pendingAuthorizationCallback =
                callback;

        try {

            List<Scope> requestedScopes =
                    Collections.singletonList(
                            new Scope(
                                    DriveScopes.DRIVE_FILE
                            )
                    );

            AuthorizationRequest request =
                    AuthorizationRequest
                            .builder()
                            .setRequestedScopes(
                                    requestedScopes
                            )
                            .build();

            AuthorizationClient client =
                    Identity.getAuthorizationClient(
                            activity
                    );

            client.authorize(request)
                    .addOnSuccessListener(
                            activity,
                            result -> {

                                if (result.hasResolution()) {

                                    IntentSenderRequest senderRequest =
                                            new IntentSenderRequest
                                                    .Builder(
                                                            result
                                                                    .getPendingIntent()
                                                                    .getIntentSender()
                                                    )
                                                    .build();

                                    launchAuthorization(
                                            activity,
                                            senderRequest
                                    );

                                } else {

                                    gunakanHasilAuthorization(
                                            result
                                    );
                                }
                            }
                    )
                    .addOnFailureListener(
                            activity,
                            e -> {

                                if (pendingAuthorizationCallback
                                        != null) {

                                    pendingAuthorizationCallback
                                            .onError(
                                                    e
                                            );

                                    pendingAuthorizationCallback =
                                            null;
                                }
                            }
                    );

        } catch (Exception e) {

            if (callback != null) {
                callback.onError(e);
            }
        }
    }

    /*
     * MainActivity harus memanggil metode ini melalui
     * ActivityResultLauncher.
     */
    private void launchAuthorization(
            Activity activity,
            IntentSenderRequest request
    ) {

        if (activity instanceof MainActivity) {

            ((MainActivity) activity)
                    .launchDriveAuthorization(request);

        } else {

            if (pendingAuthorizationCallback != null) {

                pendingAuthorizationCallback
                        .onError(
                                new Exception(
                                        "Activity tidak mendukung authorization Google Drive."
                                )
                        );

                pendingAuthorizationCallback =
                        null;
            }
        }
    }

    public void prosesHasilAuthorization(
            Activity activity,
            Intent data
    ) {

        try {

            AuthorizationResult result =
                    Identity
                            .getAuthorizationClient(
                                    activity
                            )
                            .getAuthorizationResultFromIntent(
                                    data
                            );

            gunakanHasilAuthorization(
                    result
            );

        } catch (ApiException e) {

            if (pendingAuthorizationCallback != null) {

                pendingAuthorizationCallback
                        .onError(e);

                pendingAuthorizationCallback =
                        null;
            }
        }
    }

    private void gunakanHasilAuthorization(
            AuthorizationResult result
    ) {

        try {

            String accessToken =
                    result.getAccessToken();

            if (accessToken == null
                    || accessToken.trim().isEmpty()) {

                throw new Exception(
                        "Access token Google Drive kosong."
                );
            }

            HttpRequestInitializer initializer =
                    request -> {

                        request.getHeaders()
                                .setAuthorization(
                                        "Bearer "
                                                + accessToken
                                );
                    };

            driveService =
                    new Drive.Builder(
                            new NetHttpTransport(),
                            GsonFactory.getDefaultInstance(),
                            initializer
                    )
                            .setApplicationName(
                                    "RR MOTOR REMINDER"
                            )
                            .build();

            if (pendingAuthorizationCallback
                    != null) {

                DriveCallback callback =
                        pendingAuthorizationCallback;

                pendingAuthorizationCallback =
                        null;

                mainHandler.post(
                        callback::onSuccess
                );
            }

        } catch (Exception e) {

            if (pendingAuthorizationCallback != null) {

                DriveCallback callback =
                        pendingAuthorizationCallback;

                pendingAuthorizationCallback =
                        null;

                mainHandler.post(
                        () -> callback.onError(e)
                );
            }
        }
    }

    // ============================================================
    // UPLOAD
    // ============================================================

    public void uploadDatabase(
            String json,
            DriveCallback callback
    ) {

        if (driveService == null) {

            callback.onError(
                    new Exception(
                            "Google Drive belum diotorisasi."
                    )
            );

            return;
        }

        executor.execute(() -> {

            try {

                com.google.api.services.drive.model.File existing =
                        cariFileDatabaseInternal();

                ByteArrayContent content =
                        new ByteArrayContent(
                                MIME_TYPE,
                                json.getBytes(
                                        java.nio.charset.StandardCharsets.UTF_8
                                )
                        );

                if (existing == null) {

                    com.google.api.services.drive.model.File metadata =
                            new com.google.api.services.drive.model.File();

                    metadata.setName(
                            FILE_NAME
                    );

                    metadata.setMimeType(
                            MIME_TYPE
                    );

                    driveService
                            .files()
                            .create(
                                    metadata,
                                    content
                            )
                            .setFields(
                                    "id,name"
                            )
                            .execute();

                } else {

                    driveService
                            .files()
                            .update(
                                    existing.getId(),
                                    null,
                                    content
                            )
                            .setFields(
                                    "id,name"
                            )
                            .execute();
                }

                mainHandler.post(
                        callback::onSuccess
                );

            } catch (Exception e) {

                mainHandler.post(
                        () -> callback.onError(e)
                );
            }
        });
    }

    // ============================================================
    // DOWNLOAD
    // ============================================================

    public void downloadDatabase(
            DataCallback callback
    ) {

        if (driveService == null) {

            callback.onError(
                    new Exception(
                            "Google Drive belum diotorisasi."
                    )
            );

            return;
        }

        executor.execute(() -> {

            try {

                com.google.api.services.drive.model.File file =
                        cariFileDatabaseInternal();

                if (file == null) {

                    mainHandler.post(
                            () -> callback.onSuccess("")
                    );

                    return;
                }

                ByteArrayOutputStream output =
                        new ByteArrayOutputStream();

                driveService
                        .files()
                        .get(file.getId())
                        .executeMediaAndDownloadTo(
                                output
                        );

                String json =
                        output.toString(
                                java.nio.charset.StandardCharsets.UTF_8.name()
                        );

                mainHandler.post(
                        () -> callback.onSuccess(json)
                );

            } catch (Exception e) {

                mainHandler.post(
                        () -> callback.onError(e)
                );
            }
        });
    }

    // ============================================================
    // CARI FILE
    // ============================================================

    private com.google.api.services.drive.model.File
    cariFileDatabaseInternal()
            throws Exception {

        String query =
                "name = '"
                        + FILE_NAME
                        + "'"
                        + " and trashed = false";

        com.google.api.services.drive.model.FileList result =
                driveService
                        .files()
                        .list()
                        .setQ(query)
                        .setSpaces("drive")
                        .setFields(
                                "files(id,name,mimeType)"
                        )
                        .setPageSize(10)
                        .execute();

        List<com.google.api.services.drive.model.File> files =
                result.getFiles();

        if (files == null
                || files.isEmpty()) {

            return null;
        }

        return files.get(0);
    }

    // ============================================================
    // SHUTDOWN
    // ============================================================

    public void shutdown() {

        executor.shutdownNow();

        driveService = null;

        pendingAuthorizationCallback =
                null;
    }
}
