package com.rrmotor.reminder;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.widget.Toast;

import com.google.firebase.firestore.FirebaseFirestore;

public class DeleteReminderReceiver extends BroadcastReceiver {

    @Override
    public void onReceive(
            Context context,
            Intent intent
    ) {

        String documentId =
                intent.getStringExtra("documentId");

        if (documentId == null ||
                documentId.trim().isEmpty()) {

            return;
        }

        FirebaseFirestore.getInstance()
                .collection("reminders")
                .document(documentId)
                .get()
                .addOnSuccessListener(
                        documentSnapshot -> {

                            if (!documentSnapshot.exists()) {
                                return;
                            }

                            /*
                             * Hanya hapus reminder yang
                             * memang sudah ditandai terkirim.
                             */
                            Boolean terkirim =
                                    documentSnapshot.getBoolean(
                                            "reminderTerkirim"
                                    );

                            if (terkirim == null ||
                                    !terkirim) {

                                return;
                            }

                            /*
                             * Pastikan waktu penghapusan
                             * memang sudah lewat.
                             */
                            Long deleteAt =
                                    documentSnapshot.getLong(
                                            "deleteAt"
                                    );

                            if (deleteAt == null) {
                                return;
                            }

                            long sekarang =
                                    System.currentTimeMillis();

                            if (sekarang < deleteAt) {

                                return;
                            }

                            /*
                             * Hapus dokumen Firestore.
                             */
                            FirebaseFirestore.getInstance()
                                    .collection("reminders")
                                    .document(documentId)
                                    .delete()
                                    .addOnSuccessListener(
                                            unused -> {

                                                Toast.makeText(
                                                        context,
                                                        "Riwayat reminder otomatis dihapus",
                                                        Toast.LENGTH_SHORT
                                                ).show();

                                            }
                                    );
                        }
                );
    }
}
