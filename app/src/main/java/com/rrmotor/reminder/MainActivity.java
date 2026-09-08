package com.rrmotor.reminder;

import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.Build;

import androidx.core.app.NotificationCompat;

import com.google.firebase.firestore.FirebaseFirestore;

import java.util.HashMap;
import java.util.Map;

public class ReminderReceiver extends BroadcastReceiver {

    private static final String CHANNEL_ID =
            "RR_MOTOR_REMINDER";

    @Override
    public void onReceive(
            Context context,
            Intent intent
    ) {

        String documentId =
                intent.getStringExtra("documentId");

        String nama =
                intent.getStringExtra("nama");

        String wa =
                intent.getStringExtra("wa");

        String pesan =
                intent.getStringExtra("pesan");

        if (nama == null ||
                nama.trim().isEmpty()) {

            nama = "Bapak/Ibu";
        }

        if (pesan == null ||
                pesan.trim().isEmpty()) {

            pesan =
                    "Waktunya melakukan pengecekan atau pergantian oli motor di RR MOTOR.";
        }

        /*
         * Reminder sudah masuk waktunya.
         *
         * Di sini data diberi tanda TERKIRIM dan
         * deleteAt diisi 2 hari dari waktu reminder.
         *
         * Firestore TTL nantinya dapat menggunakan
         * field deleteAt untuk menghapus otomatis.
         */
        tandaiReminderTerkirim(
                documentId
        );

        buatChannel(context);

        Intent bukaIntent =
                new Intent(
                        context,
                        MainActivity.class
                );

        bukaIntent.putExtra(
                "reminder_nama",
                nama
        );

        bukaIntent.putExtra(
                "reminder_wa",
                wa
        );

        bukaIntent.putExtra(
                "reminder_pesan",
                pesan
        );

        bukaIntent.putExtra(
                "reminder_dari_notifikasi",
                true
        );

        bukaIntent.setFlags(
                Intent.FLAG_ACTIVITY_NEW_TASK |
                        Intent.FLAG_ACTIVITY_CLEAR_TOP |
                        Intent.FLAG_ACTIVITY_SINGLE_TOP
        );

        int requestCode;

        if (documentId != null &&
                !documentId.trim().isEmpty()) {

            requestCode =
                    Math.abs(
                            documentId.hashCode()
                    );

            if (requestCode == 0) {
                requestCode = 1;
            }

        } else {

            requestCode =
                    (int)
                            (System.currentTimeMillis()
                                    & 0x7fffffff);

            if (requestCode == 0) {
                requestCode = 1;
            }
        }

        PendingIntent pendingIntent =
                PendingIntent.getActivity(
                        context,
                        requestCode,
                        bukaIntent,
                        PendingIntent.FLAG_UPDATE_CURRENT |
                                PendingIntent.FLAG_IMMUTABLE
                );

        NotificationCompat.Builder builder =
                new NotificationCompat.Builder(
                        context,
                        CHANNEL_ID
                )
                        .setSmallIcon(
                                android.R.drawable.ic_popup_reminder
                        )
                        .setContentTitle(
                                "🏍️ RR MOTOR"
                        )
                        .setContentText(
                                "Reminder ganti oli untuk "
                                        + nama
                        )
                        .setStyle(
                                new NotificationCompat.BigTextStyle()
                                        .bigText(pesan)
                        )
                        .setPriority(
                                NotificationCompat.PRIORITY_HIGH
                        )
                        .setAutoCancel(true)
                        .setContentIntent(
                                pendingIntent
                        );

        NotificationManager manager =
                (NotificationManager)
                        context.getSystemService(
                                Context.NOTIFICATION_SERVICE
                        );

        if (manager != null) {

            manager.notify(
                    requestCode,
                    builder.build()
            );
        }
    }

    private void tandaiReminderTerkirim(
            String documentId
    ) {

        if (documentId == null ||
                documentId.trim().isEmpty()) {

            return;
        }

        long waktuTerkirim =
                System.currentTimeMillis();

        /*
         * 2 hari = 48 jam.
         */
        long duaHari =
                2L
                        * 24L
                        * 60L
                        * 60L
                        * 1000L;

        long deleteAt =
                waktuTerkirim + duaHari;

        Map<String, Object> update =
                new HashMap<>();

        update.put(
                "reminderTerkirim",
                true
        );

        update.put(
                "waktuTerkirim",
                waktuTerkirim
        );

        /*
         * Field ini digunakan sebagai waktu
         * penghapusan otomatis.
         */
        update.put(
                "deleteAt",
                deleteAt
        );

        FirebaseFirestore.getInstance()
                .collection("reminders")
                .document(documentId)
                .update(update)
                .addOnFailureListener(
                        e -> {
                            // Tidak menghentikan notifikasi
                        }
                );
    }

    private void buatChannel(
            Context context
    ) {

        if (Build.VERSION.SDK_INT >=
                Build.VERSION_CODES.O) {

            NotificationChannel channel =
                    new NotificationChannel(
                            CHANNEL_ID,
                            "RR MOTOR Reminder",
                            NotificationManager.IMPORTANCE_HIGH
                    );

            channel.setDescription(
                    "Notifikasi pengingat ganti oli RR MOTOR"
            );

            NotificationManager manager =
                    context.getSystemService(
                            NotificationManager.class
                    );

            if (manager != null) {

                manager.createNotificationChannel(
                        channel
                );
            }
        }
    }
}
