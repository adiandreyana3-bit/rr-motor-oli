package com.rrmotor.reminder;

import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.Build;

import androidx.core.app.NotificationCompat;

import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;

public class ReminderReceiver extends BroadcastReceiver {

    private static final String CHANNEL_ID = "RR_MOTOR_REMINDER";

    @Override
    public void onReceive(Context context, Intent intent) {

        if (intent == null) {
            return;
        }

        String documentId = intent.getStringExtra("documentId");

        if (documentId == null || documentId.trim().isEmpty()) {
            return;
        }

        String nama = intent.getStringExtra("nama");
        String wa = intent.getStringExtra("wa");
        String pesan = intent.getStringExtra("pesan");

        if (nama == null || nama.trim().isEmpty()) {
            nama = "Bapak/Ibu";
        }

        if (wa == null) {
            wa = "";
        }

        if (pesan == null || pesan.trim().isEmpty()) {
            pesan = "Waktunya melakukan pengecekan atau pergantian oli motor di RR MOTOR.";
        }

        final String namaFinal = nama;
        final String waFinal = wa;
        final String pesanFinal = pesan;

        FirebaseFirestore db =
                FirebaseFirestore.getInstance();

        PendingResult pendingResult = goAsync();

        db.collection("reminders")
                .document(documentId)
                .get()
                .addOnSuccessListener(documentSnapshot -> {

                    if (!documentSnapshot.exists()) {
                        pendingResult.finish();
                        return;
                    }

                    Boolean sudahTerkirim =
                            documentSnapshot.getBoolean("reminderTerkirim");

                    if (Boolean.TRUE.equals(sudahTerkirim)) {

                        // Sudah pernah dikirim.
                        // Jangan kirim lagi.
                        pendingResult.finish();
                        return;
                    }

                    long sekarang =
                            System.currentTimeMillis();

                    db.runTransaction(transaction -> {

                        DocumentSnapshot fresh =
                                transaction.get(
                                        db.collection("reminders")
                                                .document(documentId)
                                );

                        if (!fresh.exists()) {
                            return false;
                        }

                        Boolean terkirim =
                                fresh.getBoolean("reminderTerkirim");

                        if (Boolean.TRUE.equals(terkirim)) {
                            return false;
                        }

                        transaction.update(
                                db.collection("reminders")
                                        .document(documentId),
                                "reminderTerkirim",
                                true,
                                "waktuTerkirim",
                                sekarang,
                                "deleteAt",
                                0L
                        );

                        return true;

                    }).addOnSuccessListener(berhasil -> {

                        if (Boolean.TRUE.equals(berhasil)) {

                            tampilkanNotifikasi(
                                    context,
                                    documentId,
                                    namaFinal,
                                    waFinal,
                                    pesanFinal
                            );
                        }

                        pendingResult.finish();

                    }).addOnFailureListener(e -> {

                        // Jika transaksi gagal,
                        // status tetap BELUM TERKIRIM.
                        // Sistem bisa mencoba kembali nanti.
                        pendingResult.finish();
                    });

                })
                .addOnFailureListener(e -> {
                    pendingResult.finish();
                });
    }

    private void tampilkanNotifikasi(
            Context context,
            String documentId,
            String nama,
            String wa,
            String pesan
    ) {

        NotificationManager notificationManager =
                (NotificationManager)
                        context.getSystemService(
                                Context.NOTIFICATION_SERVICE
                        );

        if (notificationManager == null) {
            return;
        }

        buatNotificationChannel(notificationManager);

        Intent bukaIntent =
                new Intent(context, MainActivity.class);

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

        bukaIntent.putExtra(
                "documentId",
                documentId
        );

        bukaIntent.setFlags(
                Intent.FLAG_ACTIVITY_NEW_TASK
                        | Intent.FLAG_ACTIVITY_CLEAR_TOP
                        | Intent.FLAG_ACTIVITY_SINGLE_TOP
        );

        PendingIntent pendingIntent =
                PendingIntent.getActivity(
                        context,
                        Math.abs(documentId.hashCode()),
                        bukaIntent,
                        PendingIntent.FLAG_UPDATE_CURRENT
                                | PendingIntent.FLAG_IMMUTABLE
                );

        String judul =
                "🏍️ RR MOTOR";

        String isi =
                "Reminder ganti oli untuk " + nama;

        NotificationCompat.Builder builder =
                new NotificationCompat.Builder(
                        context,
                        CHANNEL_ID
                )
                        .setSmallIcon(
                                android.R.drawable.ic_popup_reminder
                        )
                        .setContentTitle(judul)
                        .setContentText(isi)
                        .setStyle(
                                new NotificationCompat.BigTextStyle()
                                        .bigText(pesan)
                        )
                        .setPriority(
                                NotificationCompat.PRIORITY_HIGH
                        )
                        .setCategory(
                                NotificationCompat.CATEGORY_REMINDER
                        )
                        .setAutoCancel(true)
                        .setContentIntent(pendingIntent);

        notificationManager.notify(
                Math.abs(documentId.hashCode()),
                builder.build()
        );
    }

    private void buatNotificationChannel(
            NotificationManager notificationManager
    ) {

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {

            NotificationChannel channel =
                    new NotificationChannel(
                            CHANNEL_ID,
                            "RR MOTOR Reminder",
                            NotificationManager.IMPORTANCE_HIGH
                    );

            channel.setDescription(
                    "Pengingat ganti oli pelanggan RR MOTOR"
            );

            channel.enableVibration(true);

            notificationManager.createNotificationChannel(
                    channel
            );
        }
    }
}
