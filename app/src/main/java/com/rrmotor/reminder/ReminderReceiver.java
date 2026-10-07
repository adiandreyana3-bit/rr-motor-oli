package com.rrmotor.reminder;

import android.Manifest;
import android.app.AlarmManager;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;

import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;

public class ReminderReceiver extends BroadcastReceiver {

    private static final String CHANNEL_ID =
            "RR_MOTOR_REMINDER";

    private static final String CHANNEL_NAME =
            "RR MOTOR Reminder";

    @Override
    public void onReceive(Context context, Intent intent) {

        if (intent == null) {
            return;
        }

        String documentId =
                intent.getStringExtra("documentId");

        String nama =
                intent.getStringExtra("nama");

        String wa =
                intent.getStringExtra("wa");

        String pesan =
                intent.getStringExtra("pesan");

        if (documentId == null ||
                documentId.trim().isEmpty()) {

            return;
        }

        if (nama == null ||
                nama.trim().isEmpty()) {

            nama = "Bapak/Ibu";
        }

        if (wa == null) {
            wa = "";
        }

        if (pesan == null ||
                pesan.trim().isEmpty()) {

            pesan =
                    "Waktunya melakukan pengecekan " +
                    "atau pergantian oli motor di RR MOTOR.";
        }

        /*
         * Tandai lokal sebagai TERKIRIM.
         *
         * Tidak ada Firebase / Firestore.
         */
        LocalDataStore store =
                new LocalDataStore(context);

        ReminderData data =
                store.getById(documentId);

        if (data != null) {

            data.setReminderTerkirim(true);

            data.setWaktuTerkirim(
                    System.currentTimeMillis()
            );

            store.update(data);
        }

        buatNotificationChannel(context);

        tampilkanNotifikasi(
                context,
                documentId,
                nama,
                wa,
                pesan
        );
    }

    private void tampilkanNotifikasi(
            Context context,
            String documentId,
            String nama,
            String wa,
            String pesan) {

        Intent bukaIntent =
                new Intent(
                        context,
                        MainActivity.class
                );

        bukaIntent.putExtra(
                "reminder_dari_notifikasi",
                true
        );

        bukaIntent.putExtra(
                "reminder_wa",
                wa
        );

        bukaIntent.putExtra(
                "reminder_pesan",
                pesan
        );

        bukaIntent.addFlags(
                Intent.FLAG_ACTIVITY_NEW_TASK |
                Intent.FLAG_ACTIVITY_CLEAR_TOP |
                Intent.FLAG_ACTIVITY_SINGLE_TOP
        );

        int requestCode =
                buatRequestCode(documentId);

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
                                android.R.drawable.ic_dialog_info
                        )
                        .setContentTitle(
                                "🏍️ RR MOTOR"
                        )
                        .setContentText(
                                "Reminder ganti oli untuk " + nama
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

        if (Build.VERSION.SDK_INT >= 33) {

            if (context.checkSelfPermission(
                    Manifest.permission.POST_NOTIFICATIONS)
                    != PackageManager.PERMISSION_GRANTED) {

                return;
            }
        }

        NotificationManagerCompat manager =
                NotificationManagerCompat.from(context);

        manager.notify(
                requestCode,
                builder.build()
        );
    }

    private void buatNotificationChannel(
            Context context) {

        if (Build.VERSION.SDK_INT < 26) {
            return;
        }

        NotificationManager manager =
                context.getSystemService(
                        NotificationManager.class
                );

        if (manager == null) {
            return;
        }

        NotificationChannel channel =
                new NotificationChannel(
                        CHANNEL_ID,
                        CHANNEL_NAME,
                        NotificationManager.IMPORTANCE_HIGH
                );

        channel.setDescription(
                "Notifikasi reminder ganti oli RR MOTOR"
        );

        manager.createNotificationChannel(
                channel
        );
    }

    private int buatRequestCode(
            String id) {

        int code =
                Math.abs(id.hashCode());

        if (code == 0) {
            code = 1;
        }

        return code;
    }
}
