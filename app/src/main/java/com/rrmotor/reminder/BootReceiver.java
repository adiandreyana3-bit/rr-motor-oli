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
public void onReceive(
        Context context,
        Intent intent
) {

    String action =
            intent != null
                    ? intent.getAction()
                    : null;

    if (Intent.ACTION_BOOT_COMPLETED.equals(action)
            || Intent.ACTION_MY_PACKAGE_REPLACED.equals(action)
            || Intent.ACTION_TIME_CHANGED.equals(action)
            || Intent.ACTION_TIMEZONE_CHANGED.equals(action)) {

        cekSemuaReminder(context);
    }
}

private void cekSemuaReminder(
        Context context
) {

    FirebaseFirestore db =
            FirebaseFirestore.getInstance();

    db.collection("reminders")
            .whereEqualTo(
                    "reminderTerkirim",
                    false
            )
            .get()
            .addOnSuccessListener(
                    querySnapshot -> {

                        long sekarang =
                                System.currentTimeMillis();

                        for (
                                DocumentSnapshot doc :
                                querySnapshot.getDocuments()
                        ) {

                            Long waktuReminderLong =
                                    doc.getLong(
                                            "waktuReminder"
                                    );

                            if (waktuReminderLong == null) {
                                continue;
                            }

                            long waktuReminder =
                                    waktuReminderLong;

                            String documentId =
                                    doc.getId();

                            String nama =
                                    doc.getString(
                                            "nama"
                                    );

                            String wa =
                                    doc.getString(
                                            "whatsapp"
                                    );

                            String pesan =
                                    doc.getString(
                                            "pesanWhatsApp"
                                    );

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
                                        "Waktunya melakukan pengecekan atau pergantian oli motor di RR MOTOR.";
                            }

                            /*
                             * REMINDER SUDAH LEWAT
                             *
                             * Jalankan langsung.
                             */
                            if (waktuReminder <= sekarang) {

                                kirimReminderSekarang(
                                        context,
                                        documentId,
                                        nama,
                                        wa,
                                        pesan
                                );

                            } else {

                                /*
                                 * REMINDER BELUM LEWAT
                                 *
                                 * Jadwalkan ulang.
                                 */
                                jadwalkanAlarm(
                                        context,
                                        waktuReminder,
                                        documentId,
                                        nama,
                                        wa,
                                        pesan
                                );
                            }
                        }
                    }
            );
}

private void kirimReminderSekarang(
        Context context,
        String documentId,
        String nama,
        String wa,
        String pesan
) {

    Intent reminderIntent =
            new Intent(
                    context,
                    ReminderReceiver.class
            );

    reminderIntent.putExtra(
            "documentId",
            documentId
    );

    reminderIntent.putExtra(
            "nama",
            nama
    );

    reminderIntent.putExtra(
            "wa",
            wa
    );

    reminderIntent.putExtra(
            "pesan",
            pesan
    );

    context.sendBroadcast(
            reminderIntent
    );
}

private void jadwalkanAlarm(
        Context context,
        long waktuReminder,
        String documentId,
        String nama,
        String wa,
        String pesan
) {

    AlarmManager alarmManager =
            (AlarmManager)
                    context.getSystemService(
                            Context.ALARM_SERVICE
                    );

    if (alarmManager == null) {
        return;
    }

    Intent reminderIntent =
            new Intent(
                    context,
                    ReminderReceiver.class
            );

    reminderIntent.putExtra(
            "documentId",
            documentId
    );

    reminderIntent.putExtra(
            "nama",
            nama
    );

    reminderIntent.putExtra(
            "wa",
            wa
    );

    reminderIntent.putExtra(
            "pesan",
            pesan
    );

    int requestCode =
            Math.abs(
                    documentId.hashCode()
            );

    if (requestCode == 0) {
        requestCode = 1;
    }

    PendingIntent pendingIntent =
            PendingIntent.getBroadcast(
                    context,
                    requestCode,
                    reminderIntent,
                    PendingIntent.FLAG_UPDATE_CURRENT
                            | PendingIntent.FLAG_IMMUTABLE
            );

    /*
     * Batalkan alarm lama agar tidak ganda.
     */
    alarmManager.cancel(
            pendingIntent
    );

    try {

        if (Build.VERSION.SDK_INT >=
                Build.VERSION_CODES.S) {

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

        } else if (Build.VERSION.SDK_INT >=
                Build.VERSION_CODES.M) {

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
