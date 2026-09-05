package com.rrmotor.reminder;

import android.Manifest;
import android.app.AlarmManager;
import android.app.DatePickerDialog;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.ContactsContract;
import android.provider.Settings;
import android.text.InputType;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;

import java.text.NumberFormat;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

public class MainActivity extends AppCompatActivity {

private EditText namaInput;
private EditText nopolInput;
private EditText mesinInput;
private EditText kmInput;
private EditText waInput;
private EditText tanggalInput;

private Spinner jatuhTempoSpinner;

private TextView hasilKmText;

private Button simpanButton;
private Button dataBaruButton;
private Button riwayatButton;

private FirebaseAuth auth;
private FirebaseFirestore db;

private ActivityResultLauncher<String> contactPermissionLauncher;
private ActivityResultLauncher<String> notificationPermissionLauncher;

private final Calendar tanggalTerpilih = Calendar.getInstance();

@Override
protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);

    auth = FirebaseAuth.getInstance();
    db = FirebaseFirestore.getInstance();

    daftarPermissionLauncher();

    buatTampilan();

    isiTanggalHariIni();

    cekLoginFirebase();

    if (getIntent().getBooleanExtra("reminder_dari_notifikasi", false)) {
        bukaWhatsAppDariNotifikasi();
    }
}

private void daftarPermissionLauncher() {

    contactPermissionLauncher =
            registerForActivityResult(
                    new ActivityResultContracts.RequestPermission(),
                    granted -> {
                        if (granted) {
                            pilihKontak();
                        } else {
                            Toast.makeText(
                                    this,
                                    "Izin kontak diperlukan untuk memilih nomor WhatsApp.",
                                    Toast.LENGTH_LONG
                            ).show();
                        }
                    }
            );

    notificationPermissionLauncher =
            registerForActivityResult(
                    new ActivityResultContracts.RequestPermission(),
                    granted -> {
                        if (!granted) {
                            Toast.makeText(
                                    this,
                                    "Izin notifikasi diperlukan agar reminder dapat muncul.",
                                    Toast.LENGTH_LONG
                            ).show();
                        }
                    }
            );
}

private void buatTampilan() {

    ScrollView scrollView = new ScrollView(this);
    scrollView.setFillViewport(true);

    LinearLayout utama = new LinearLayout(this);
    utama.setOrientation(LinearLayout.VERTICAL);
    utama.setPadding(30, 30, 30, 40);

    scrollView.addView(utama);

    TextView judul = new TextView(this);
    judul.setText("🏍️ RR MOTOR REMINDER");
    judul.setTextSize(22);
    judul.setPadding(0, 0, 0, 25);
    utama.addView(judul);

    TextView keterangan = new TextView(this);
    keterangan.setText(
            "Pengingat ganti oli pelanggan"
    );
    keterangan.setTextSize(16);
    keterangan.setPadding(0, 0, 0, 20);
    utama.addView(keterangan);

    namaInput = buatInput(
            "Nama pelanggan (opsional)",
            false
    );
    utama.addView(namaInput);

    nopolInput = buatInput(
            "Nopol (opsional)",
            false
    );
    utama.addView(nopolInput);

    mesinInput = buatInput(
            "Nomor mesin (opsional)",
            false
    );
    utama.addView(mesinInput);

    kmInput = buatInput(
            "KM terakhir (opsional)",
            true
    );
    kmInput.setInputType(InputType.TYPE_CLASS_NUMBER);
    utama.addView(kmInput);

    LinearLayout waLayout = new LinearLayout(this);
    waLayout.setOrientation(LinearLayout.HORIZONTAL);

    waInput = buatInput(
            "Nomor WhatsApp (WA) *",
            true
    );
    waInput.setInputType(InputType.TYPE_CLASS_PHONE);

    LinearLayout.LayoutParams waParams =
            new LinearLayout.LayoutParams(
                    0,
                    LinearLayout.LayoutParams.WRAP_CONTENT,
                    1
            );

    waLayout.addView(waInput, waParams);

    Button kontakButton = new Button(this);
    kontakButton.setText("📱 KONTAK");
    kontakButton.setOnClickListener(v -> mintaIzinKontak());

    waLayout.addView(
            kontakButton,
            new LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.WRAP_CONTENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
            )
    );

    utama.addView(waLayout);

    tanggalInput = buatInput(
            "Tanggal input data *",
            false
    );
    tanggalInput.setFocusable(false);
    tanggalInput.setOnClickListener(v -> pilihTanggal());

    utama.addView(tanggalInput);

    TextView tempoLabel = new TextView(this);
    tempoLabel.setText("Jatuh tempo reminder *");
    tempoLabel.setTextSize(15);
    tempoLabel.setPadding(0, 15, 0, 5);
    utama.addView(tempoLabel);

    jatuhTempoSpinner = new Spinner(this);

    String[] pilihanTempo = {
            "1 BULAN",
            "2 BULAN"
    };

    ArrayAdapter<String> adapter =
            new ArrayAdapter<>(
                    this,
                    android.R.layout.simple_spinner_item,
                    pilihanTempo
            );

    adapter.setDropDownViewResource(
            android.R.layout.simple_spinner_dropdown_item
    );

    jatuhTempoSpinner.setAdapter(adapter);

    utama.addView(jatuhTempoSpinner);

    hasilKmText = new TextView(this);
    hasilKmText.setText(
            "Perhitungan KM akan muncul di sini."
    );
    hasilKmText.setTextSize(16);
    hasilKmText.setPadding(0, 20, 0, 20);

    utama.addView(hasilKmText);

    Button hitungButton = new Button(this);
    hitungButton.setText("🔢 HITUNG KM");

    hitungButton.setOnClickListener(
            v -> tampilkanPerhitunganKm()
    );

    utama.addView(hitungButton);

    simpanButton = new Button(this);
    simpanButton.setText("💾 SIMPAN DATA");

    simpanButton.setOnClickListener(
            v -> simpanData()
    );

    utama.addView(simpanButton);

    dataBaruButton = new Button(this);
    dataBaruButton.setText("➕ DATA BARU");

    dataBaruButton.setOnClickListener(
            v -> dataBaru()
    );

    utama.addView(dataBaruButton);

    riwayatButton = new Button(this);
    riwayatButton.setText("📋 RIWAYAT REMINDER");

    riwayatButton.setOnClickListener(
            v -> tampilkanRiwayat()
    );

    utama.addView(riwayatButton);

    setContentView(scrollView);
}

private EditText buatInput(
        String hint,
        boolean wajib
) {

    EditText input = new EditText(this);

    input.setHint(
            wajib ? hint : hint
    );

    input.setTextSize(16);

    input.setPadding(
            10,
            10,
            10,
            10
    );

    LinearLayout.LayoutParams params =
            new LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
            );

    params.setMargins(0, 5, 0, 5);

    input.setLayoutParams(params);

    return input;
}

private void isiTanggalHariIni() {

    SimpleDateFormat format =
            new SimpleDateFormat(
                    "dd/MM/yyyy",
                    Locale.getDefault()
            );

    tanggalInput.setText(
            format.format(
                    tanggalTerpilih.getTime()
            )
    );
}

private void pilihTanggal() {

    DatePickerDialog dialog =
            new DatePickerDialog(
                    this,
                    (view, year, month, dayOfMonth) -> {

                        tanggalTerpilih.set(
                                year,
                                month,
                                dayOfMonth
                        );

                        SimpleDateFormat format =
                                new SimpleDateFormat(
                                        "dd/MM/yyyy",
                                        Locale.getDefault()
                                );

                        tanggalInput.setText(
                                format.format(
                                        tanggalTerpilih.getTime()
                                )
                        );
                    },
                    tanggalTerpilih.get(
                            Calendar.YEAR
                    ),
                    tanggalTerpilih.get(
                            Calendar.MONTH
                    ),
                    tanggalTerpilih.get(
                            Calendar.DAY_OF_MONTH
                    )
            );

    dialog.show();
}

private void tampilkanPerhitunganKm() {

    String kmText =
            kmInput.getText().toString().trim();

    if (kmText.isEmpty()) {

        hasilKmText.setText(
                "KM terakhir kosong.\n" +
                "Perhitungan KM tidak dibuat."
        );

        return;
    }

    long km;

    try {
        km = Long.parseLong(kmText);
    } catch (Exception e) {

        Toast.makeText(
                this,
                "KM tidak valid.",
                Toast.LENGTH_SHORT
        ).show();

        return;
    }

    long maksimal =
            hitungKelipatanBerikutnya(
                    km,
                    1500
            );

    long palingLambat =
            hitungKelipatanBerikutnya(
                    km,
                    2000
            );

    hasilKmText.setText(
            "KM terakhir: " +
                    formatKm(km) +
                    " KM\n\n" +

                    "Maksimal ganti oli: " +
                    formatKm(maksimal) +
                    " KM\n\n" +

                    "Paling lambat: " +
                    formatKm(palingLambat) +
                    " KM"
    );
}

private long hitungKelipatanBerikutnya(
        long km,
        long kelipatan
) {

    if (km < 0) {
        return kelipatan;
    }

    return ((km / kelipatan) + 1) * kelipatan;
}

private String formatKm(long angka) {

    NumberFormat format =
            NumberFormat.getIntegerInstance(
                    new Locale("id", "ID")
            );

    return format.format(angka);
}

private void simpanData() {

    String wa =
            waInput.getText().toString().trim();

    if (wa.isEmpty()) {

        waInput.requestFocus();

        Toast.makeText(
                this,
                "Nomor WhatsApp wajib diisi.",
                Toast.LENGTH_LONG
        ).show();

        return;
    }

    if (tanggalInput.getText().toString().trim().isEmpty()) {

        tanggalInput.requestFocus();

        Toast.makeText(
                this,
                "Tanggal input data wajib diisi.",
                Toast.LENGTH_LONG
        ).show();

        return;
    }

    if (auth.getCurrentUser() == null) {

        Toast.makeText(
                this,
                "Akun Firebase belum login.",
                Toast.LENGTH_LONG
        ).show();

        return;
    }

    long waktuSimpan =
            System.currentTimeMillis();

    String nama =
            namaInput.getText().toString().trim();

    String nopol =
            nopolInput.getText().toString().trim();

    String mesin =
            mesinInput.getText().toString().trim();

    String km =
            kmInput.getText().toString().trim();

    String tempo =
            jatuhTempoSpinner.getSelectedItem().toString();

    String pesan =
            buatPesanWhatsApp(
                    nama,
                    nopol,
                    mesin,
                    km,
                    tempo
            );

    Calendar waktuReminder =
            (Calendar) tanggalTerpilih.clone();

    int bulanTambah =
            tempo.startsWith("2") ? 2 : 1;

    waktuReminder.add(
            Calendar.MONTH,
            bulanTambah
    );

    Map<String, Object> data =
            new HashMap<>();

    data.put(
            "nama",
            nama
    );

    data.put(
            "nopol",
            nopol
    );

    data.put(
            "nomorMesin",
            mesin
    );

    data.put(
            "kmTerakhir",
            km
    );

    data.put(
            "whatsapp",
            wa
    );

    data.put(
            "tanggalInput",
            tanggalInput.getText().toString()
    );

    data.put(
            "jatuhTempo",
            tempo
    );

    data.put(
            "pesanWhatsApp",
            pesan
    );

    data.put(
            "waktuReminder",
            waktuReminder.getTimeInMillis()
    );

    data.put(
            "reminderTerkirim",
            false
    );

    data.put(
            "waktuTerkirim",
            0L
    );

    data.put(
            "deleteAt",
            0L
    );

    data.put(
            "waktuSimpan",
            waktuSimpan
    );

    db.collection("reminders")
            .add(data)
            .addOnSuccessListener(documentReference -> {

                jadwalkanReminder(
                        waktuReminder,
                        nama,
                        wa,
                        pesan,
                        documentReference.getId()
                );

                simpanButton.setEnabled(false);

                Toast.makeText(
                        this,
                        "Data berhasil disimpan.",
                        Toast.LENGTH_LONG
                ).show();
            })
            .addOnFailureListener(e -> {

                Toast.makeText(
                        this,
                        "Gagal menyimpan data: " +
                                e.getMessage(),
                        Toast.LENGTH_LONG
                ).show();
            });
}

private String buatPesanWhatsApp(
        String nama,
        String nopol,
        String mesin,
        String kmText,
        String tempo
) {

    StringBuilder pesan =
            new StringBuilder();

    pesan.append("🏍️ RR MOTOR\n\n");

    if (nama.isEmpty()) {
        pesan.append("Halo Bapak/Ibu.\n\n");
    } else {
        pesan.append(
                "Halo Bapak/Ibu "
                        + nama
                        + ".\n\n"
        );
    }

    if (tempo.startsWith("2")) {

        pesan.append(
                "Sudah 2 bulan sejak terakhir " +
                        "ganti oli di RR MOTOR.\n\n"
        );

    } else {

        pesan.append(
                "Sudah 1 bulan sejak terakhir " +
                        "ganti oli di RR MOTOR.\n\n"
        );
    }

    pesan.append(
            "Kami mengingatkan untuk melakukan " +
                    "pengecekan atau pergantian oli motor.\n\n"
    );

    if (!nopol.isEmpty()) {

        pesan.append(
                "Nopol: "
                        + nopol
                        + "\n"
        );
    }

    if (!mesin.isEmpty()) {

        pesan.append(
                "Nomor mesin: "
                        + mesin
                        + "\n"
        );
    }

    if (!kmText.isEmpty()) {

        try {

            long km =
                    Long.parseLong(kmText);

            long maksimal =
                    hitungKelipatanBerikutnya(
                            km,
                            1500
                    );

            long palingLambat =
                    hitungKelipatanBerikutnya(
                            km,
                            2000
                    );

            pesan.append(
                    "KM terakhir: "
                            + formatKm(km)
                            + " KM\n"
            );

            pesan.append(
                    "Maksimal ganti oli: "
                            + formatKm(maksimal)
                            + " KM\n"
            );

            pesan.append(
                    "Paling lambat: "
                            + formatKm(palingLambat)
                            + " KM\n"
            );

        } catch (Exception ignored) {
        }
    }

    pesan.append("\n");

    pesan.append(
            "Pengecekan oli GRATIS.\n\n"
    );

    pesan.append(
            "Terima kasih telah mempercayakan " +
                    "perawatan motor Anda kepada RR MOTOR."
    );

    return pesan.toString();
}

private void jadwalkanReminder(
        Calendar waktuReminder,
        String nama,
        String wa,
        String pesan,
        String documentId
) {

    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {

        AlarmManager alarmManager =
                (AlarmManager) getSystemService(
                        Context.ALARM_SERVICE
                );

        if (alarmManager != null &&
                !alarmManager.canScheduleExactAlarms()) {

            Intent intent =
                    new Intent(
                            Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM
                    );

            startActivity(intent);
        }
    }

    Intent intent =
            new Intent(
                    this,
                    ReminderReceiver.class
            );

    intent.putExtra(
            "nama",
            nama
    );

    intent.putExtra(
            "wa",
            wa
    );

    intent.putExtra(
            "pesan",
            pesan
    );

    intent.putExtra(
            "documentId",
            documentId
    );

    int requestCode =
            Math.abs(documentId.hashCode());

    PendingIntent pendingIntent =
            PendingIntent.getBroadcast(
                    this,
                    requestCode,
                    intent,
                    PendingIntent.FLAG_UPDATE_CURRENT |
                            PendingIntent.FLAG_IMMUTABLE
            );

    AlarmManager alarmManager =
            (AlarmManager) getSystemService(
                    Context.ALARM_SERVICE
            );

    if (alarmManager == null) {
        return;
    }

    long waktu =
            waktuReminder.getTimeInMillis();

    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {

        if (alarmManager.canScheduleExactAlarms()) {

            alarmManager.setExactAndAllowWhileIdle(
                    AlarmManager.RTC_WAKEUP,
                    waktu,
                    pendingIntent
            );

        } else {

            alarmManager.setAndAllowWhileIdle(
                    AlarmManager.RTC_WAKEUP,
                    waktu,
                    pendingIntent
            );
        }

    } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {

        alarmManager.setExactAndAllowWhileIdle(
                AlarmManager.RTC_WAKEUP,
                waktu,
                pendingIntent
        );

    } else {

        alarmManager.setExact(
                AlarmManager.RTC_WAKEUP,
                waktu,
                pendingIntent
        );
    }
}

private void bukaWhatsAppDariNotifikasi() {

    String wa =
            getIntent().getStringExtra(
                    "reminder_wa"
            );

    String pesan =
            getIntent().getStringExtra(
                    "reminder_pesan"
            );

    if (wa == null || wa.trim().isEmpty()) {
        return;
    }

    if (pesan == null) {
        pesan = "";
    }

    String nomor =
            wa.replaceAll(
                    "[^0-9+]",
                    ""
            );

    if (nomor.startsWith("0")) {
        nomor =
                "62"
                        + nomor.substring(1);
    }

    String url =
            "https://wa.me/"
                    + nomor
                    + "?text="
                    + Uri.encode(pesan);

    try {

        Intent intent =
                new Intent(
                        Intent.ACTION_VIEW,
                        Uri.parse(url)
                );

        startActivity(intent);

    } catch (Exception e) {

        Toast.makeText(
                this,
                "WhatsApp tidak dapat dibuka.",
                Toast.LENGTH_LONG
        ).show();
    }
}

private void cekLoginFirebase() {

    if (auth.getCurrentUser() == null) {

        Toast.makeText(
                this,
                "Firebase belum memiliki akun login.",
                Toast.LENGTH_LONG
        ).show();

    } else {

        if (Build.VERSION.SDK_INT >= 33) {

            if (ContextCompat.checkSelfPermission(
                    this,
                    Manifest.permission.POST_NOTIFICATIONS
            ) != PackageManager.PERMISSION_GRANTED) {

                notificationPermissionLauncher.launch(
                        Manifest.permission.POST_NOTIFICATIONS
                );
            }
        }
    }
}

private void mintaIzinKontak() {

    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {

        if (ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.READ_CONTACTS
        ) != PackageManager.PERMISSION_GRANTED) {

            contactPermissionLauncher.launch(
                    Manifest.permission.READ_CONTACTS
            );

            return;
        }
    }

    pilihKontak();
}

private void pilihKontak() {

    Intent intent =
            new Intent(
                    Intent.ACTION_PICK,
                    ContactsContract.CommonDataKinds.Phone.CONTENT_URI
            );

    startActivityForResult(
            intent,
            1001
    );
}

@Override
protected void onActivityResult(
        int requestCode,
        int resultCode,
        Intent data
) {

    super.onActivityResult(
            requestCode,
            resultCode,
            data
    );

    if (requestCode == 1001 &&
            resultCode == RESULT_OK &&
            data != null) {

        Uri contactUri =
                data.getData();

        if (contactUri == null) {
            return;
        }

        Cursor cursor =
                getContentResolver().query(
                        contactUri,
                        new String[]{
                                ContactsContract.CommonDataKinds.Phone.NUMBER
                        },
                        null,
                        null,
                        null
                );

        if (cursor != null) {

            if (cursor.moveToFirst()) {

                int index =
                        cursor.getColumnIndex(
                                ContactsContract.CommonDataKinds.Phone.NUMBER
                        );

                if (index >= 0) {

                    String nomor =
                            cursor.getString(index);

                    waInput.setText(
                            nomor
                    );
                }
            }

            cursor.close();
        }
    }
}

private void dataBaru() {

    namaInput.setText("");
    nopolInput.setText("");
    mesinInput.setText("");
    kmInput.setText("");
    waInput.setText("");

    tanggalTerpilih.setTimeInMillis(
            System.currentTimeMillis()
    );

    isiTanggalHariIni();

    jatuhTempoSpinner.setSelection(0);

    hasilKmText.setText(
            "Perhitungan KM akan muncul di sini."
    );

    simpanButton.setEnabled(true);

    namaInput.requestFocus();

    Toast.makeText(
            this,
            "Form siap untuk data baru.",
            Toast.LENGTH_SHORT
    ).show();
}

private void tampilkanRiwayat() {

    if (auth.getCurrentUser() == null) {

        Toast.makeText(
                this,
                "Silakan login Firebase terlebih dahulu.",
                Toast.LENGTH_LONG
        ).show();

        return;
    }

    db.collection("reminders")
            .orderBy(
                    "waktuSimpan"
            )
            .get()
            .addOnSuccessListener(
                    queryDocumentSnapshots -> {

                        int jumlah =
                                queryDocumentSnapshots.size();

                        Toast.makeText(
                                this,
                                "Jumlah riwayat: "
                                        + jumlah,
                                Toast.LENGTH_LONG
                        ).show();
                    }
            )
            .addOnFailureListener(
                    e -> Toast.makeText(
                            this,
                            "Gagal mengambil riwayat: "
                                    + e.getMessage(),
                            Toast.LENGTH_LONG
                    ).show()
            );
}

}
