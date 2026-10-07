package com.rrmotor.reminder;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.Build;

import java.util.ArrayList;

public class BootReceiver extends BroadcastReceiver {

    @Override
    public void onReceive(
            Context context,
            Intent intent) {

        if (intent == null) {
            return;
        }

        String action =
                intent.getAction();

        if (!Intent.ACTION_BOOT_COMPLETED.equals(action) &&
                !Intent.ACTION_LOCKED_BOOT_COMPLETED.equals(action) &&
                !AlarmManager.ACTION_SCHEDULE_EXACT_ALARM_PERMISSION_STATE_CHANGED.equals(action)) {

            return;
        }

        /*
         * Gunakan goAsync supaya proses reschedule
         * mempunyai waktu untuk berjalan.
         */
        PendingResult pendingResult =
                goAsync();

        new Thread(() -> {

            try {

                jadwalkanUlangSemua(
                        context.getApplicationContext()
                );

            } finally {

                pendingResult.finish();
            }

        }).start();
    }

    private void jadwalkanUlangSemua(
            Context context) {

        LocalDataStore store =
                new LocalDataStore(context);

        ArrayList<ReminderData> reminders =
                store.getUnsent();

        if (reminders == null ||
                reminders.isEmpty()) {

            return;
        }

        long sekarang =
                System.currentTimeMillis();

        for (ReminderData data : reminders) {

            if (data == null) {
                continue;
            }

            if (data.isReminderTerkirim()) {
                continue;
            }

            long waktu =
                    data.getWaktuReminder();

            if (waktu <= 0) {
                continue;
            }

            /*
             * Kalau waktunya belum lewat,
             * pasang alarm sesuai jadwal asli.
             */
            if (waktu > sekarang) {

                jadwalkan(
                        context,
                        data
                );

            } else {

                /*
                 * Kalau HP mati saat waktu reminder
                 * sudah lewat, jalankan reminder
                 * segera setelah HP hidup.
                 */
                jalankanSekarang(
                        context,
                        data
                );
            }
        }
    }

    private void jadwalkan(
            Context context,
            ReminderData data) {

        AlarmManager alarm =
                (AlarmManager)
                        context.getSystemService(
                                Context.ALARM_SERVICE
                        );

        if (alarm == null) {
            return;
        }

        Intent intent =
                buatIntent(
                        context,
                        data
                );

        int requestCode =
                buatRequestCode(
                        data.getId()
                );

        PendingIntent pending =
                PendingIntent.getBroadcast(
                        context,
                        requestCode,
                        intent,
                        PendingIntent.FLAG_UPDATE_CURRENT |
                        PendingIntent.FLAG_IMMUTABLE
                );

        alarm.cancel(pending);

        try {

            if (Build.VERSION.SDK_INT >=
                    Build.VERSION_CODES.S) {

                /*
                 * Android 12+
                 */
                if (!alarm.canScheduleExactAlarms()) {

                    /*
                     * Tidak boleh memaksa exact alarm
                     * jika izin belum diberikan.
                     *
                     * Gunakan alarm inexact sebagai
                     * fallback.
                     */
                    alarm.setAndAllowWhileIdle(
                            AlarmManager.RTC_WAKEUP,
                            data.getWaktuReminder(),
                            pending
                    );

                    return;
                }

                alarm.setExactAndAllowWhileIdle(
                        AlarmManager.RTC_WAKEUP,
                        data.getWaktuReminder(),
                        pending
                );

            } else if (Build.VERSION.SDK_INT >=
                    Build.VERSION_CODES.M) {

                alarm.setExactAndAllowWhileIdle(
                        AlarmManager.RTC_WAKEUP,
                        data.getWaktuReminder(),
                        pending
                );

            } else {

                alarm.setExact(
                        AlarmManager.RTC_WAKEUP,
                        data.getWaktuReminder(),
                        pending
                );
            }

        } catch (SecurityException e) {

            /*
             * Fallback jika exact alarm ditolak.
             */
            try {

                alarm.setAndAllowWhileIdle(
                        AlarmManager.RTC_WAKEUP,
                        data.getWaktuReminder(),
                        pending
                );

            } catch (Exception ignored) {
            }
        }
    }

    private void jalankanSekarang(
            Context context,
            ReminderData data) {

        Intent intent =
                buatIntent(
                        context,
                        data
                );

        context.sendBroadcast(intent);
    }

    private Intent buatIntent(
            Context context,
            ReminderData data) {

        Intent intent =
                new Intent(
                        context,
                        ReminderReceiver.class
                );

        intent.putExtra(
                "documentId",
                data.getId()
        );

        intent.putExtra(
                "nama",
                data.getNama()
        );

        intent.putExtra(
                "wa",
                data.getWhatsapp()
        );

        intent.putExtra(
                "pesan",
                data.getPesanWhatsApp()
        );

        return intent;
    }

    private int buatRequestCode(
            String id) {

        if (id == null ||
                id.trim().isEmpty()) {

            return 1;
        }

        int code =
                Math.abs(id.hashCode());

        if (code == 0) {
            code = 1;
        }

        return code;
    }
}
