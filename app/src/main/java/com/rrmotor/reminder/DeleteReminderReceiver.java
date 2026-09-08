package com.rrmotor.reminder;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.Build;

import com.google.firebase.Timestamp;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.Date;
import java.util.Map;

public class DeleteReminderReceiver extends BroadcastReceiver {

    private static final String TAG =
            "DeleteReminderReceiver";

    /*
     * Pemeriksaan dilakukan setiap 6 jam.
     */
    private static final long INTERVAL_CHECK =
            6L * 60L * 60L * 1000L;

    /*
     * Nama collection Firestore.
     */
    private static final String COLLECTION =
            "reminders";

    @Override
    public void onReceive(
            Context context,
            Intent intent
    ) {

        /*
         * Jadwalkan pemeriksaan berikutnya terlebih dahulu.
         * Jadi walaupun proses penghapusan mengalami
         * masalah, pemeriksaan berikutnya tetap ada.
         */
        jadwalkanPemeriksaanBerikutnya(context);

        hapusReminderKedaluwarsa();
    }

    /**
     * Memeriksa semua reminder yang sudah terkirim.
     */
    private void hapusReminderKedaluwarsa() {

        FirebaseFirestore db =
                FirebaseFirestore.getInstance();

        db.collection(COLLECTION)
                .whereEqualTo(
                        "reminderTerkirim",
                        true
                )
                .get()
                .addOnSuccessListener(
                        querySnapshot -> {

                            long sekarang =
                                    System.currentTimeMillis();

                            for (
                                    DocumentSnapshot document
                                    : querySnapshot.getDocuments()
                            ) {

                                Object deleteAtObject =
                                        document.get("deleteAt");

                                if (deleteAtObject == null) {
                                    continue;
                                }

                                Long deleteAtMillis =
                                        ambilWaktuMillis(
                                                deleteAtObject
                                        );

                                if (deleteAtMillis == null) {
                                    continue;
                                }

                                /*
                                 * Jika waktu penghapusan sudah
                                 * lewat, hapus dokumen.
                                 */
                                if (
                                        sekarang >=
                                                deleteAtMillis
                                ) {

                                    String documentId =
                                            document.getId();

                                    db.collection(COLLECTION)
                                            .document(documentId)
                                            .delete()
                                            .addOnSuccessListener(
                                                    unused -> {
                                                        // Berhasil dihapus
                                                    }
                                            )
                                            .addOnFailureListener(
                                                    e -> {
                                                        // Tidak menghentikan
                                                        // proses dokumen lain
                                                    }
                                            );
                                }
                            }
                        }
                )
                .addOnFailureListener(
                        e -> {
                            /*
                             * Jangan crash jika Firestore
                             * sedang tidak tersedia.
                             *
                             * Pemeriksaan berikutnya akan
                             * mencoba lagi.
                             */
                        }
                );
    }

    /**
     * Mengubah berbagai kemungkinan format deleteAt
     * menjadi milliseconds.
     *
     * Mendukung:
     *
     * 1. Long
     * 2. Integer
     * 3. Double
     * 4. Timestamp Firestore
     * 5. Date
     */
    private Long ambilWaktuMillis(
            Object value
    ) {

        if (value instanceof Long) {

            return (Long) value;

        }

        if (value instanceof Integer) {

            return ((Integer) value).longValue();

        }

        if (value instanceof Double) {

            return ((Double) value).longValue();

        }

        if (value instanceof Timestamp) {

            Timestamp timestamp =
                    (Timestamp) value;

            return timestamp.toDate().getTime();

        }

        if (value instanceof Date) {

            return ((Date) value).getTime();

        }

        return null;
    }

    /**
     * Menjadwalkan pemeriksaan berikutnya.
     *
     * Menggunakan setAndAllowWhileIdle supaya alarm
     * tetap dapat dijalankan ketika perangkat sedang
     * idle/Doze.
     */
    public static void jadwalkanPemeriksaan(
            Context context
    ) {

        AlarmManager alarmManager =
                (AlarmManager)
                        context.getSystemService(
                                Context.ALARM_SERVICE
                        );

        if (alarmManager == null) {
            return;
        }

        Intent intent =
                new Intent(
                        context,
                        DeleteReminderReceiver.class
                );

        PendingIntent pendingIntent =
                PendingIntent.getBroadcast(
                        context,
                        987654,
                        intent,
                        PendingIntent.FLAG_UPDATE_CURRENT
                                | PendingIntent.FLAG_IMMUTABLE
                );

        long waktuBerikutnya =
                System.currentTimeMillis()
                        + INTERVAL_CHECK;

        /*
         * Hapus alarm lama jika ada.
         */
        alarmManager.cancel(pendingIntent);

        /*
         * Tidak menggunakan setExact().
         *
         * Dengan demikian aplikasi tidak membutuhkan
         * SCHEDULE_EXACT_ALARM hanya untuk pekerjaan
         * pembersihan data.
         */
        if (Build.VERSION.SDK_INT >=
                Build.VERSION_CODES.M) {

            alarmManager.setAndAllowWhileIdle(
                    AlarmManager.RTC_WAKEUP,
                    waktuBerikutnya,
                    pendingIntent
            );

        } else {

            alarmManager.set(
                    AlarmManager.RTC_WAKEUP,
                    waktuBerikutnya,
                    pendingIntent
            );
        }
    }

    /**
     * Dipanggil setelah receiver menerima alarm.
     */
    private void jadwalkanPemeriksaanBerikutnya(
            Context context
    ) {

        jadwalkanPemeriksaan(context);
    }
}
