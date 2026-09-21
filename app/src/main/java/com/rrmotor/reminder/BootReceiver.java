package com.rrmotor.reminder;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.Build;

import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;

public class BootReceiver extends BroadcastReceiver {

    @Override
    public void onReceive(Context context, Intent intent) {

        if (intent == null || intent.getAction() == null) {
            return;
        }

        String action = intent.getAction();

        if (!Intent.ACTION_BOOT_COMPLETED.equals(action)
                && !Intent.ACTION_MY_PACKAGE_REPLACED.equals(action)
                && !Intent.ACTION_TIME_SET.equals(action)
                && !Intent.ACTION_TIMEZONE_CHANGED.equals(action)) {
            return;
        }

        rescheduleSemuaReminder(context);
    }

    private void rescheduleSemuaReminder(Context context) {

        FirebaseFirestore db = FirebaseFirestore.getInstance();

        db.collection("reminders")
                .whereEqualTo("reminderTerkirim", false)
                .get()
                .addOnSuccessListener(querySnapshot -> {

                    long sekarang = System.currentTimeMillis();

                    for (DocumentSnapshot doc : querySnapshot.getDocuments()) {

                        Long waktuObj = doc.getLong("waktuReminder");

                        if (waktuObj == null) {
                            continue;
                        }

                        long waktuReminder = waktuObj;
                        String documentId = doc.getId();

                        String nama = doc.getString("nama");
                        String wa = doc.getString("whatsapp");
                        String pesan = doc.getString("pesanWhatsApp");

                        if (nama == null || nama.trim().isEmpty()) {
                            nama = "Bapak/Ibu";
                        }

                        if (wa == null) {
                            wa = "";
                        }

                        if (pesan == null || pesan.trim().isEmpty()) {
                            pesan = "Waktunya melakukan pengecekan atau pergantian oli motor di RR MOTOR.";
                        }

                        // Reminder sudah lewat.
                        // Kirim segera ke ReminderReceiver.
                        if (waktuReminder <= sekarang) {

                            Intent reminderIntent =
                                    new Intent(context, ReminderReceiver.class);

                            reminderIntent.putExtra("documentId", documentId);
                            reminderIntent.putExtra("nama", nama);
                            reminderIntent.putExtra("wa", wa);
                            reminderIntent.putExtra("pesan", pesan);

                            context.sendBroadcast(reminderIntent);

                        } else {

                            // Reminder masih akan datang.
                            // Hanya jadwalkan ulang, TIDAK kirim notifikasi.
                            jadwalkanReminder(
                                    context,
                                    documentId,
                                    nama,
                                    wa,
                                    pesan,
                                    waktuReminder
                            );
                        }
                    }
                })
                .addOnFailureListener(e -> {
                    // Dicoba kembali pada pemeriksaan berikutnya.
                });
    }

    private void jadwalkanReminder(
            Context context,
            String documentId,
            String nama,
            String wa,
            String pesan,
            long waktuReminder
    ) {

        AlarmManager alarmManager =
                (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);

        if (alarmManager == null) {
            return;
        }

        Intent intent =
                new Intent(context, ReminderReceiver.class);

        intent.putExtra("documentId", documentId);
        intent.putExtra("nama", nama);
        intent.putExtra("wa", wa);
        intent.putExtra("pesan", pesan);

        int requestCode = Math.abs(documentId.hashCode());

        PendingIntent pendingIntent =
                PendingIntent.getBroadcast(
                        context,
                        requestCode,
                        intent,
                        PendingIntent.FLAG_UPDATE_CURRENT
                                | PendingIntent.FLAG_IMMUTABLE
                );

        // Hindari alarm ganda.
        alarmManager.cancel(pendingIntent);

        try {

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {

                if (alarmManager.canScheduleExactAlarms()) {

                    alarmManager.setExactAndAllowWhileIdle(
                            AlarmManager.RTC_WAKEUP,
                            waktuReminder,
                            pendingIntent
                    );

                } else {

                    alarmManager.setAndAllowWhileIdle(
                            AlarmManager.RTC_WAKEUP,
                            waktuReminder,
                            pendingIntent
                    );
                }

            } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {

                alarmManager.setExactAndAllowWhileIdle(
                        AlarmManager.RTC_WAKEUP,
                        waktuReminder,
                        pendingIntent
                );

            } else {

                alarmManager.setExact(
                        AlarmManager.RTC_WAKEUP,
                        waktuReminder,
                        pendingIntent
                );
            }

        } catch (SecurityException e) {

            try {

                alarmManager.setAndAllowWhileIdle(
                        AlarmManager.RTC_WAKEUP,
                        waktuReminder,
                        pendingIntent
                );

            } catch (Exception ignored) {
            }
        }
    }
}
