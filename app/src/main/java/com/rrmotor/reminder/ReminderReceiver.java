package com.rrmotor.reminder;

import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.Build;

import androidx.core.app.NotificationCompat;

public class ReminderReceiver extends BroadcastReceiver {

private static final String CHANNEL_ID = "RR_MOTOR_REMINDER";

@Override
public void onReceive(Context context, Intent intent) {

    String nama = intent.getStringExtra("nama");
    String wa = intent.getStringExtra("wa");
    String pesan = intent.getStringExtra("pesan");

    if (nama == null || nama.trim().isEmpty()) {
        nama = "Bapak/Ibu";
    }

    if (pesan == null || pesan.trim().isEmpty()) {
        pesan = "Waktunya melakukan pengecekan atau pergantian oli motor di RR MOTOR.";
    }

    buatChannel(context);

    Intent bukaIntent = new Intent(context, MainActivity.class);
    bukaIntent.putExtra("reminder_nama", nama);
    bukaIntent.putExtra("reminder_wa", wa);
    bukaIntent.putExtra("reminder_pesan", pesan);
    bukaIntent.putExtra("reminder_dari_notifikasi", true);

    PendingIntent pendingIntent = PendingIntent.getActivity(
            context,
            (int) System.currentTimeMillis(),
            bukaIntent,
            PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
    );

    NotificationCompat.Builder builder =
            new NotificationCompat.Builder(context, CHANNEL_ID)
                    .setSmallIcon(android.R.drawable.ic_popup_reminder)
                    .setContentTitle("🏍️ RR MOTOR")
                    .setContentText("Reminder ganti oli untuk " + nama)
                    .setStyle(new NotificationCompat.BigTextStyle()
                            .bigText(pesan))
                    .setPriority(NotificationCompat.PRIORITY_HIGH)
                    .setAutoCancel(true)
                    .setContentIntent(pendingIntent);

    NotificationManager manager =
            (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);

    if (manager != null) {
        manager.notify(
                (int) (System.currentTimeMillis() & 0x7fffffff),
                builder.build()
        );
    }
}

private void buatChannel(Context context) {

    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {

        NotificationChannel channel = new NotificationChannel(
                CHANNEL_ID,
                "RR MOTOR Reminder",
                NotificationManager.IMPORTANCE_HIGH
        );

        channel.setDescription(
                "Notifikasi pengingat ganti oli RR MOTOR"
        );

        NotificationManager manager =
                context.getSystemService(NotificationManager.class);

        if (manager != null) {
            manager.createNotificationChannel(channel);
        }
    }
}

}
