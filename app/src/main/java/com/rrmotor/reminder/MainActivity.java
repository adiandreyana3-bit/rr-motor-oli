package com.rrmotor.reminder;

import android.Manifest;
import android.app.AlarmManager;
import android.app.DatePickerDialog;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.ContactsContract;
import android.provider.Settings;
import android.text.InputType;
import android.view.Gravity;
import android.view.View;
import android.widget.*;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.UUID;

public class MainActivity extends AppCompatActivity {

    private static final String PREF_LOGIN = "RR_MOTOR_LOGIN";
    private static final String KEY_LOGGED_IN = "logged_in";

    private static final int REQ_CONTACT = 1001;
    private static final int REQ_NOTIFICATION = 1002;

    private LocalDataStore localDataStore;
    private GoogleDriveHelper googleDriveHelper;

    private EditText etNama;
    private EditText etNopol;
    private EditText etNomorMesin;
    private EditText etKm;
    private EditText etWhatsapp;
    private EditText etTanggal;

    private Spinner spinnerBulan;

    private Button btnSimpan;
    private Button btnDataBaru;
    private Button btnRiwayat;
    private Button btnHapusRiwayat;
    private Button btnBackup;
    private Button btnRestore;

    private TextView tvKmInfo;

    private final SimpleDateFormat tanggalFormat =
            new SimpleDateFormat("dd-MM-yyyy", Locale.getDefault());

    private final ActivityResultLauncher<String> notificationPermissionLauncher =
            registerForActivityResult(
                    new ActivityResultContracts.RequestPermission(),
                    granted -> {
                        if (!granted) {
                            Toast.makeText(
                                    this,
                                    "Izin notifikasi belum diberikan.",
                                    Toast.LENGTH_SHORT
                            ).show();
                        }
                    }
            );

    private final ActivityResultLauncher<Intent> contactPickerLauncher =
            registerForActivityResult(
                    new ActivityResultContracts.StartActivityForResult(),
                    result -> {
                        if (result.getResultCode() == RESULT_OK
                                && result.getData() != null
                                && result.getData().getData() != null) {

                            Uri contactUri = result.getData().getData();
                            ambilDataKontak(contactUri);
                        }
                    }
            );

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        localDataStore = new LocalDataStore(this);
        googleDriveHelper = new GoogleDriveHelper(this);

        if (!isLoggedIn()) {
            bukaLogin();
            return;
        }

        buatTampilan();

        mintaIzinNotifikasi();

        prosesIntentNotifikasi();

        cekIzinAlarm();

        // Kalau HP baru / aplikasi baru dan data lokal kosong,
        // coba pulihkan data dari Google Drive.
        if (localDataStore.size() == 0) {
            restoreDariGoogleDriveOtomatis();
        } else {
            jadwalkanSemuaReminder();
        }
    }

    // ============================================================
    // LOGIN
    // ============================================================

    private boolean isLoggedIn() {
        SharedPreferences pref =
                getSharedPreferences(PREF_LOGIN, MODE_PRIVATE);

        return pref.getBoolean(KEY_LOGGED_IN, false);
    }

    private void bukaLogin() {
        Intent intent = new Intent(this, LoginActivity.class);
        startActivity(intent);
        finish();
    }

    // ============================================================
    // TAMPILAN
    // ============================================================

    private void buatTampilan() {

        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setPadding(dp(16), dp(12), dp(16), dp(20));
        root.setBackgroundColor(0xFFF5F5F5);

        ScrollView scrollView = new ScrollView(this);

        LinearLayout isi = new LinearLayout(this);
        isi.setOrientation(LinearLayout.VERTICAL);

        TextView title = new TextView(this);
        title.setText("🏍️ RR MOTOR");
        title.setTextSize(28);
        title.setGravity(Gravity.CENTER);
        title.setTypeface(null, android.graphics.Typeface.BOLD);
        title.setTextColor(0xFF087F23);

        isi.addView(title);

        TextView subtitle = new TextView(this);
        subtitle.setText("REMINDER GANTI OLI");
        subtitle.setTextSize(16);
        subtitle.setGravity(Gravity.CENTER);
        subtitle.setTextColor(0xFF555555);

        isi.addView(subtitle);

        isi.addView(jarak(12));

        etNama = buatEditText(
                "Nama pelanggan",
                InputType.TYPE_CLASS_TEXT |
                        InputType.TYPE_TEXT_FLAG_CAP_WORDS
        );
        isi.addView(etNama);

        etNopol = buatEditText(
                "Nopol / Plat nomor (opsional)",
                InputType.TYPE_CLASS_TEXT |
                        InputType.TYPE_TEXT_FLAG_CAP_CHARACTERS
        );
        isi.addView(etNopol);

        etNomorMesin = buatEditText(
                "Nomor mesin (opsional)",
                InputType.TYPE_CLASS_TEXT
        );
        isi.addView(etNomorMesin);

        etKm = buatEditText(
                "KM terakhir (opsional)",
                InputType.TYPE_CLASS_NUMBER
        );
        isi.addView(etKm);

        LinearLayout waRow = new LinearLayout(this);
        waRow.setOrientation(LinearLayout.HORIZONTAL);
        waRow.setGravity(Gravity.CENTER_VERTICAL);

        etWhatsapp = buatEditText(
                "Nomor WhatsApp",
                InputType.TYPE_CLASS_PHONE
        );

        LinearLayout.LayoutParams waParams =
                new LinearLayout.LayoutParams(
                        0,
                        LinearLayout.LayoutParams.WRAP_CONTENT,
                        1
                );

        waRow.addView(etWhatsapp, waParams);

        Button btnKontak = new Button(this);
        btnKontak.setText("📱 KONTAK");
        btnKontak.setOnClickListener(v -> bukaKontak());

        waRow.addView(btnKontak);

        isi.addView(waRow);

        etTanggal = buatEditText(
                "Tanggal input",
                InputType.TYPE_CLASS_DATETIME
        );

        etTanggal.setFocusable(false);
        etTanggal.setClickable(true);

        etTanggal.setOnClickListener(v -> pilihTanggal());

        isi.addView(etTanggal);

        spinnerBulan = new Spinner(this);

        String[] pilihanBulan = {
                "1 BULAN",
                "2 BULAN"
        };

        ArrayAdapter<String> adapter =
                new ArrayAdapter<>(
                        this,
                        android.R.layout.simple_spinner_dropdown_item,
                        pilihanBulan
                );

        spinnerBulan.setAdapter(adapter);

        isi.addView(spinnerBulan);

        isi.addView(jarak(8));

        tvKmInfo = new TextView(this);
        tvKmInfo.setTextSize(14);
        tvKmInfo.setTextColor(0xFF444444);
        tvKmInfo.setPadding(
                dp(10),
                dp(10),
                dp(10),
                dp(10)
        );

        isi.addView(tvKmInfo);

        etKm.setOnFocusChangeListener((v, hasFocus) -> {
            if (!hasFocus) {
                tampilkanPerhitunganKm();
            }
        });

        isi.addView(jarak(8));

        btnSimpan = buatButton(
                "💾 SIMPAN DATA",
                0xFF087F23
        );

        btnSimpan.setOnClickListener(v -> simpanData());

        isi.addView(btnSimpan);

        btnDataBaru = buatButton(
                "➕ DATA BARU",
                0xFF087F23
        );

        btnDataBaru.setOnClickListener(v -> dataBaru());

        isi.addView(btnDataBaru);

        btnRiwayat = buatButton(
                "📋 RIWAYAT REMINDER",
                0xFF087F23
        );

        btnRiwayat.setOnClickListener(v -> tampilkanRiwayat());

        isi.addView(btnRiwayat);

        btnHapusRiwayat = buatButton(
                "🗑️ HAPUS RIWAYAT TERKIRIM",
                0xFF8B0000
        );

        btnHapusRiwayat.setOnClickListener(
                v -> konfirmasiHapusRiwayat()
        );

        isi.addView(btnHapusRiwayat);

        btnBackup = buatButton(
                "☁️ BACKUP KE GOOGLE DRIVE",
                0xFF087F23
        );

        btnBackup.setOnClickListener(
                v -> backupKeGoogleDrive()
        );

        isi.addView(btnBackup);

        btnRestore = buatButton(
                "♻️ RESTORE DARI GOOGLE DRIVE",
                0xFF087F23
        );

        btnRestore.setOnClickListener(
                v -> konfirmasiRestore()
        );

        isi.addView(btnRestore);

        scrollView.addView(isi);

        root.addView(
                scrollView,
                new LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.MATCH_PARENT,
                        0,
                        1
                )
        );

        setContentView(root);

        isiTanggalHariIni();

        btnDataBaru.setVisibility(View.VISIBLE);
    }

    private EditText buatEditText(
            String hint,
            int inputType
    ) {

        EditText editText = new EditText(this);

        editText.setHint(hint);
        editText.setTextSize(16);
        editText.setSingleLine(true);
        editText.setInputType(inputType);
        editText.setPadding(
                dp(12),
                dp(10),
                dp(12),
                dp(10)
        );

        LinearLayout.LayoutParams params =
                new LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.MATCH_PARENT,
                        LinearLayout.LayoutParams.WRAP_CONTENT
                );

        params.setMargins(0, dp(4), 0, dp(4));

        editText.setLayoutParams(params);

        return editText;
    }

    private Button buatButton(
            String text,
            int color
    ) {

        Button button = new Button(this);

        button.setText(text);
        button.setTextSize(14);
        button.setTextColor(0xFFFFFFFF);
        button.setBackgroundColor(color);

        LinearLayout.LayoutParams params =
                new LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.MATCH_PARENT,
                        LinearLayout.LayoutParams.WRAP_CONTENT
                );

        params.setMargins(
                0,
                dp(5),
                0,
                dp(5)
        );

        button.setLayoutParams(params);

        return button;
    }

    private TextView jarak(int dp) {
        TextView view = new TextView(this);

        view.setHeight(dp(dp));

        return view;
    }

    private int dp(int value) {
        return (int) (
                value *
                        getResources()
                                .getDisplayMetrics()
                                .density
        );
    }

    // ============================================================
    // TANGGAL
    // ============================================================

    private void isiTanggalHariIni() {

        etTanggal.setText(
                tanggalFormat.format(new Date())
        );
    }

    private void pilihTanggal() {

        Calendar calendar = Calendar.getInstance();

        try {
            Date date =
                    tanggalFormat.parse(
                            etTanggal.getText().toString()
                    );

            if (date != null) {
                calendar.setTime(date);
            }

        } catch (Exception ignored) {
        }

        DatePickerDialog dialog =
                new DatePickerDialog(
                        this,
                        (view, year, month, dayOfMonth) -> {

                            Calendar selected =
                                    Calendar.getInstance();

                            selected.set(
                                    year,
                                    month,
                                    dayOfMonth
                            );

                            etTanggal.setText(
                                    tanggalFormat.format(
                                            selected.getTime()
                                    )
                            );
                        },
                        calendar.get(Calendar.YEAR),
                        calendar.get(Calendar.MONTH),
                        calendar.get(Calendar.DAY_OF_MONTH)
                );

        dialog.show();
    }

    // ============================================================
    // HITUNG KM
    // ============================================================

    private void tampilkanPerhitunganKm() {

        String kmText =
                etKm.getText().toString().trim();

        if (kmText.isEmpty()) {
            tvKmInfo.setText("");
            return;
        }

        try {

            long km =
                    Long.parseLong(kmText);

            long maksimal =
                    km + 1500;

            long palingLambat =
                    km + 2000;

            tvKmInfo.setText(
                    "🔧 PERKIRAAN GANTI OLI\n\n" +
                    "Maksimal ganti oli : " +
                    maksimal +
                    " KM\n" +
                    "Paling lambat       : " +
                    palingLambat +
                    " KM"
            );

        } catch (Exception e) {

            tvKmInfo.setText(
                    "KM tidak valid."
            );
        }
    }

    // ============================================================
    // KONTAK
    // ============================================================

    private void bukaKontak() {

        if (ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.READ_CONTACTS
        ) != PackageManager.PERMISSION_GRANTED) {

            ActivityCompat.requestPermissions(
                    this,
                    new String[]{
                            Manifest.permission.READ_CONTACTS
                    },
                    REQ_CONTACT
            );

            return;
        }

        Intent intent =
                new Intent(
                        Intent.ACTION_PICK,
                        ContactsContract.CommonDataKinds.Phone.CONTENT_URI
                );

        contactPickerLauncher.launch(intent);
    }

    private void ambilDataKontak(Uri contactUri) {

        Cursor cursor = null;

        try {

            cursor = getContentResolver().query(
                    contactUri,
                    new String[]{
                            ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME,
                            ContactsContract.CommonDataKinds.Phone.NUMBER
                    },
                    null,
                    null,
                    null
            );

            if (cursor != null && cursor.moveToFirst()) {

                int namaIndex =
                        cursor.getColumnIndex(
                                ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME
                        );

                int nomorIndex =
                        cursor.getColumnIndex(
                                ContactsContract.CommonDataKinds.Phone.NUMBER
                        );

                if (namaIndex >= 0) {
                    etNama.setText(
                            cursor.getString(namaIndex)
                    );
                }

                if (nomorIndex >= 0) {
                    etWhatsapp.setText(
                            bersihkanNomor(
                                    cursor.getString(nomorIndex)
                            )
                    );
                }
            }

        } catch (Exception e) {

            Toast.makeText(
                    this,
                    "Gagal membaca kontak.",
                    Toast.LENGTH_SHORT
            ).show();

        } finally {

            if (cursor != null) {
                cursor.close();
            }
        }
    }

    private String bersihkanNomor(String nomor) {

        if (nomor == null) {
            return "";
        }

        nomor = nomor.replaceAll(
                "[^0-9+]",
                ""
        );

        if (nomor.startsWith("+62")) {
            nomor = "0" + nomor.substring(3);
        }

        if (nomor.startsWith("62")) {
            nomor = "0" + nomor.substring(2);
        }

        return nomor;
    }

    // ============================================================
    // SIMPAN
    // ============================================================

    private void simpanData() {

        String nama =
                etNama.getText().toString().trim();

        String nopol =
                etNopol.getText().toString().trim();

        String nomorMesin =
                etNomorMesin.getText().toString().trim();

        String km =
                etKm.getText().toString().trim();

        String whatsapp =
                bersihkanNomor(
                        etWhatsapp.getText().toString().trim()
                );

        String tanggalInput =
                etTanggal.getText().toString().trim();

        if (whatsapp.isEmpty()) {

            etWhatsapp.requestFocus();

            Toast.makeText(
                    this,
                    "Nomor WhatsApp wajib diisi.",
                    Toast.LENGTH_SHORT
            ).show();

            return;
        }

        if (tanggalInput.isEmpty()) {

            Toast.makeText(
                    this,
                    "Tanggal wajib diisi.",
                    Toast.LENGTH_SHORT
            ).show();

            return;
        }

        Date tanggal;

        try {

            tanggal =
                    tanggalFormat.parse(
                            tanggalInput
                    );

        } catch (ParseException e) {

            Toast.makeText(
                    this,
                    "Format tanggal tidak valid.",
                    Toast.LENGTH_SHORT
            ).show();

            return;
        }

        if (tanggal == null) {
            return;
        }

        int bulan =
                spinnerBulan.getSelectedItemPosition() == 0
                        ? 1
                        : 2;

        Calendar jatuhTempo =
                Calendar.getInstance();

        jatuhTempo.setTime(tanggal);

        jatuhTempo.add(
                Calendar.MONTH,
                bulan
        );

        String tanggalJatuhTempo =
                tanggalFormat.format(
                        jatuhTempo.getTime()
                );

        String pesan =
                buatPesanWhatsApp(
                        nama,
                        nopol,
                        nomorMesin,
                        km,
                        bulan
                );

        long waktuReminder =
                setJam09(
                        jatuhTempo
                );

        String id =
                UUID.randomUUID().toString();

        ReminderData data =
                new ReminderData(
                        id,
                        nama,
                        nopol,
                        nomorMesin,
                        km,
                        whatsapp,
                        tanggalInput,
                        tanggalJatuhTempo,
                        pesan,
                        waktuReminder,
                        false,
                        0,
                        System.currentTimeMillis()
                );

        boolean berhasil =
                localDataStore.add(data);

        if (!berhasil) {

            Toast.makeText(
                    this,
                    "Data gagal disimpan.",
                    Toast.LENGTH_SHORT
            ).show();

            return;
        }

        jadwalkanReminder(data);

        btnSimpan.setEnabled(false);

        Toast.makeText(
                this,
                "✅ Data tersimpan.\nReminder: "
                        + tanggalJatuhTempo
                        + " jam 09:00",
                Toast.LENGTH_LONG
        ).show();

        backupKeGoogleDriveOtomatis();
    }

    private long setJam09(Calendar calendar) {

        Calendar waktu =
                (Calendar) calendar.clone();

        waktu.set(
                Calendar.HOUR_OF_DAY,
                9
        );

        waktu.set(
                Calendar.MINUTE,
                0
        );

        waktu.set(
                Calendar.SECOND,
                0
        );

        waktu.set(
                Calendar.MILLISECOND,
                0
        );

        return waktu.getTimeInMillis();
    }

    private String buatPesanWhatsApp(
            String nama,
            String nopol,
            String nomorMesin,
            String km,
            int bulan
    ) {

        StringBuilder pesan =
                new StringBuilder();

        pesan.append(
                "🏍️ *RR MOTOR*\n"
        );

        pesan.append(
                "Pengingat Ganti Oli\n\n"
        );

        if (!nama.isEmpty()) {

            pesan.append(
                    "Halo "
                            + nama
                            + ",\n\n"
            );
        } else {

            pesan.append(
                    "Halo,\n\n"
            );
        }

        pesan.append(
                "Sudah waktunya melakukan "
                        + "penggantian oli motor Anda.\n"
        );

        pesan.append(
                "Interval pengingat: "
                        + bulan
                        + " bulan.\n"
        );

        if (!nopol.isEmpty()) {

            pesan.append(
                    "Nopol: "
                            + nopol
                            + "\n"
            );
        }

        if (!nomorMesin.isEmpty()) {

            pesan.append(
                    "Nomor mesin: "
                            + nomorMesin
                            + "\n"
            );
        }

        if (!km.isEmpty()) {

            try {

                long nilaiKm =
                        Long.parseLong(km);

                pesan.append(
                        "Maksimal ganti oli: "
                                + (nilaiKm + 1500)
                                + " KM\n"
                );

                pesan.append(
                        "Paling lambat: "
                                + (nilaiKm + 2000)
                                + " KM\n"
                );

            } catch (Exception ignored) {
            }
        }

        pesan.append(
                "\nPengecekan oli *GRATIS*.\n\n"
        );

        pesan.append(
                "Silakan datang ke *RR MOTOR* "
                        + "untuk pengecekan dan penggantian oli.\n\n"
        );

        pesan.append(
                "Terima kasih 🙏"
        );

        return pesan.toString();
    }

    // ============================================================
    // ALARM
    // ============================================================

    private void jadwalkanReminder(
            ReminderData data
    ) {

        AlarmManager alarmManager =
                (AlarmManager)
                        getSystemService(
                                ALARM_SERVICE
                        );

        if (alarmManager == null) {
            return;
        }

        Intent intent =
                new Intent(
                        this,
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

        int requestCode =
                data.getId().hashCode();

        PendingIntent pendingIntent =
                PendingIntent.getBroadcast(
                        this,
                        requestCode,
                        intent,
                        PendingIntent.FLAG_UPDATE_CURRENT
                                | PendingIntent.FLAG_IMMUTABLE
                );

        long waktu =
                data.getWaktuReminder();

        if (waktu <= System.currentTimeMillis()) {
            return;
        }

        try {

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

                    Toast.makeText(
                            this,
                            "Aktifkan izin Alarm & pengingat agar reminder lebih tepat jam 09:00.",
                            Toast.LENGTH_LONG
                    ).show();
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

        } catch (SecurityException e) {

            alarmManager.setAndAllowWhileIdle(
                    AlarmManager.RTC_WAKEUP,
                    waktu,
                    pendingIntent
            );
        }
    }

    private void jadwalkanSemuaReminder() {

        List<ReminderData> semua =
                localDataStore.getUnsent();

        for (ReminderData data : semua) {

            if (data.getWaktuReminder()
                    > System.currentTimeMillis()) {

                jadwalkanReminder(data);

            } else {

                Intent intent =
                        new Intent(
                                this,
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

                sendBroadcast(intent);
            }
        }
    }

    private void batalkanReminder(
            ReminderData data
    ) {

        AlarmManager alarmManager =
                (AlarmManager)
                        getSystemService(
                                ALARM_SERVICE
                        );

        if (alarmManager == null) {
            return;
        }

        Intent intent =
                new Intent(
                        this,
                        ReminderReceiver.class
                );

        PendingIntent pendingIntent =
                PendingIntent.getBroadcast(
                        this,
                        data.getId().hashCode(),
                        intent,
                        PendingIntent.FLAG_UPDATE_CURRENT
                                | PendingIntent.FLAG_IMMUTABLE
                );

        alarmManager.cancel(
                pendingIntent
        );
    }

    // ============================================================
    // IZIN ALARM
    // ============================================================

    private void cekIzinAlarm() {

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {

            AlarmManager alarmManager =
                    (AlarmManager)
                            getSystemService(
                                    ALARM_SERVICE
                            );

            if (alarmManager != null
                    && !alarmManager.canScheduleExactAlarms()) {

                // Tidak langsung memaksa.
                // Alarm tetap memakai fallback.
            }
        }
    }

    // ============================================================
    // NOTIFIKASI
    // ============================================================

    private void mintaIzinNotifikasi() {

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

    private void prosesIntentNotifikasi() {

        Intent intent = getIntent();

        if (intent == null) {
            return;
        }

        boolean dariNotifikasi =
                intent.getBooleanExtra(
                        "reminder_dari_notifikasi",
                        false
                );

        if (!dariNotifikasi) {
            return;
        }

        String wa =
                intent.getStringExtra(
                        "reminder_wa"
                );

        String pesan =
                intent.getStringExtra(
                        "reminder_pesan"
                );

        if (wa == null || wa.trim().isEmpty()) {
            return;
        }

        new android.os.Handler().postDelayed(
                () -> bukaWhatsApp(
                        wa,
                        pesan
                ),
                700
        );

        intent.removeExtra(
                "reminder_dari_notifikasi"
        );
    }

    private void bukaWhatsApp(
            String nomor,
            String pesan
    ) {

        try {

            String nomorBersih =
                    bersihkanNomor(nomor);

            String encoded =
                    Uri.encode(pesan == null ? "" : pesan);

            Intent intent =
                    new Intent(
                            Intent.ACTION_VIEW
                    );

            intent.setData(
                    Uri.parse(
                            "https://wa.me/"
                                    + nomorBersih.substring(
                                            nomorBersih.startsWith("0")
                                                    ? 1
                                                    : 0
                                    )
                                    .replaceFirst(
                                            "^",
                                            "62"
                                    )
                                    + "?text="
                                    + encoded
                    )
            );

            startActivity(intent);

        } catch (Exception e) {

            Toast.makeText(
                    this,
                    "WhatsApp tidak dapat dibuka.",
                    Toast.LENGTH_SHORT
            ).show();
        }
    }

    // ============================================================
    // DATA BARU
    // ============================================================

    private void dataBaru() {

        etNama.setText("");
        etNopol.setText("");
        etNomorMesin.setText("");
        etKm.setText("");
        etWhatsapp.setText("");

        isiTanggalHariIni();

        spinnerBulan.setSelection(0);

        tvKmInfo.setText("");

        btnSimpan.setEnabled(true);

        etNama.requestFocus();
    }

    // ============================================================
    // RIWAYAT
    // ============================================================

    private void tampilkanRiwayat() {

        final String[] filter = {
                "SEMUA",
                "BELUM TERKIRIM",
                "TERKIRIM"
        };

        new AlertDialog.Builder(this)
                .setTitle("📋 RIWAYAT REMINDER")
                .setItems(
                        filter,
                        (dialog, which) -> {

                            if (which == 0) {
                                tampilkanDaftarRiwayat(
                                        "SEMUA"
                                );
                            } else if (which == 1) {
                                tampilkanDaftarRiwayat(
                                        "BELUM TERKIRIM"
                                );
                            } else {
                                tampilkanDaftarRiwayat(
                                        "TERKIRIM"
                                );
                            }
                        }
                )
                .show();
    }

    private void tampilkanDaftarRiwayat(
            String jenis
    ) {

        List<ReminderData> data =
                localDataStore.getSortedByNewest();

        List<ReminderData> tampil =
                new ArrayList<>();

        for (ReminderData item : data) {

            if (jenis.equals("SEMUA")) {

                tampil.add(item);

            } else if (
                    jenis.equals("TERKIRIM")
                            && item.isReminderTerkirim()
            ) {

                tampil.add(item);

            } else if (
                    jenis.equals("BELUM TERKIRIM")
                            && !item.isReminderTerkirim()
            ) {

                tampil.add(item);
            }
        }

        if (tampil.isEmpty()) {

            Toast.makeText(
                    this,
                    "Tidak ada data.",
                    Toast.LENGTH_SHORT
            ).show();

            return;
        }

        String[] daftar =
                new String[tampil.size()];

        for (int i = 0; i < tampil.size(); i++) {

            ReminderData item =
                    tampil.get(i);

            String status =
                    item.isReminderTerkirim()
                            ? "✅ TERKIRIM"
                            : "⏳ BELUM TERKIRIM";

            String nama =
                    item.getNama();

            if (nama == null
                    || nama.trim().isEmpty()) {

                nama = "(Tanpa nama)";
            }

            String nopol =
                    item.getNopol();

            if (nopol == null
                    || nopol.trim().isEmpty()) {

                nopol = "-";
            }

            String lewat = "";

            if (!item.isReminderTerkirim()
                    && item.getWaktuReminder()
                    < System.currentTimeMillis()) {

                lewat =
                        "\n⚠️ SUDAH LEWAT TEMPO";
            }

            daftar[i] =
                    status
                            + "\n"
                            + nama
                            + "\nNopol: "
                            + nopol
                            + "\nJatuh tempo: "
                            + item.getJatuhTempo()
                            + lewat;
        }

        new AlertDialog.Builder(this)
                .setTitle(
                        "RIWAYAT - "
                                + jenis
                )
                .setItems(
                        daftar,
                        (dialog, which) -> {

                            tampilkanDetail(
                                    tampil.get(which)
                            );
                        }
                )
                .show();
    }

    private void tampilkanDetail(
            ReminderData data
    ) {

        StringBuilder detail =
                new StringBuilder();

        detail.append(
                "Nama: "
                        + nilai(data.getNama())
                        + "\n"
        );

        detail.append(
                "Nopol: "
                        + nilai(data.getNopol())
                        + "\n"
        );

        detail.append(
                "Nomor mesin: "
                        + nilai(data.getNomorMesin())
                        + "\n"
        );

        detail.append(
                "KM terakhir: "
                        + nilai(data.getKmTerakhir())
                        + "\n"
        );

        detail.append(
                "WhatsApp: "
                        + nilai(data.getWhatsapp())
                        + "\n"
        );

        detail.append(
                "Tanggal input: "
                        + nilai(data.getTanggalInput())
                        + "\n"
        );

        detail.append(
                "Jatuh tempo: "
                        + nilai(data.getJatuhTempo())
                        + "\n"
        );

        detail.append(
                "Status: "
                        + (
                        data.isReminderTerkirim()
                                ? "TERKIRIM"
                                : "BELUM TERKIRIM"
                )
                        + "\n"
        );

        if (!data.isReminderTerkirim()
                && data.getWaktuReminder()
                < System.currentTimeMillis()) {

            detail.append(
                    "\n⚠️ SUDAH LEWAT TEMPO\n"
            );
        }

        detail.append(
                "\nPesan WhatsApp:\n"
        );

        detail.append(
                data.getPesanWhatsApp()
        );

        AlertDialog dialog =
                new AlertDialog.Builder(this)
                        .setTitle("DETAIL REMINDER")
                        .setMessage(detail.toString())
                        .setNegativeButton(
                                "TUTUP",
                                null
                        )
                        .setPositiveButton(
                                "WHATSAPP",
                                null
                        )
                        .setNeutralButton(
                                "HAPUS",
                                null
                        )
                        .create();

        dialog.setOnShowListener(
                d -> {

                    Button wa =
                            dialog.getButton(
                                    AlertDialog.BUTTON_POSITIVE
                            );

                    wa.setOnClickListener(
                            v -> bukaWhatsApp(
                                    data.getWhatsapp(),
                                    data.getPesanWhatsApp()
                            )
                    );

                    Button hapus =
                            dialog.getButton(
                                    AlertDialog.BUTTON_NEUTRAL
                            );

                    hapus.setOnClickListener(
                            v -> {

                                new AlertDialog.Builder(this)
                                        .setTitle("Hapus data?")
                                        .setMessage(
                                                "Data reminder ini akan dihapus dari HP."
                                        )
                                        .setNegativeButton(
                                                "BATAL",
                                                null
                                        )
                                        .setPositiveButton(
                                                "HAPUS",
                                                (dd, ww) -> {

                                                    batalkanReminder(
                                                            data
                                                    );

                                                    localDataStore.delete(
                                                            data.getId()
                                                    );

                                                    dialog.dismiss();

                                                    backupKeGoogleDriveOtomatis();

                                                    Toast.makeText(
                                                            this,
                                                            "Data dihapus.",
                                                            Toast.LENGTH_SHORT
                                                    ).show();
                                                }
                                        )
                                        .show();
                            }
                    );
                }
        );

        dialog.show();
    }

    private String nilai(String text) {

        if (text == null
                || text.trim().isEmpty()) {

            return "-";
        }

        return text;
    }

    // ============================================================
    // HAPUS RIWAYAT
    // ============================================================

    private void konfirmasiHapusRiwayat() {

        new AlertDialog.Builder(this)
                .setTitle("🗑️ Hapus riwayat?")
                .setMessage(
                        "Yang dihapus hanya data yang sudah TERKIRIM.\n\n"
                                + "Data BELUM TERKIRIM tidak akan dihapus."
                )
                .setNegativeButton(
                        "BATAL",
                        null
                )
                .setPositiveButton(
                        "HAPUS",
                        (dialog, which) -> {

                            List<ReminderData> sent =
                                    localDataStore.getSent();

                            for (ReminderData data : sent) {
                                batalkanReminder(data);
                            }

                            localDataStore.deleteAllSent();

                            backupKeGoogleDriveOtomatis();

                            Toast.makeText(
                                    this,
                                    "Riwayat terkirim dihapus.",
                                    Toast.LENGTH_SHORT
                            ).show();
                        }
                )
                .show();
    }

    // ============================================================
    // GOOGLE DRIVE
    // ============================================================

    private void backupKeGoogleDrive() {

        if (localDataStore.size() == 0) {

            Toast.makeText(
                    this,
                    "Belum ada data untuk dibackup.",
                    Toast.LENGTH_SHORT
            ).show();

            return;
        }

        Toast.makeText(
                this,
                "Menghubungkan ke Google Drive...",
                Toast.LENGTH_SHORT
        ).show();

        googleDriveHelper.mintaIzinDrive(
                this,
                new GoogleDriveHelper.DriveCallback() {

                    @Override
                    public void onSuccess() {

                        googleDriveHelper.uploadDatabase(
                                localDataStore.toJson(),
                                new GoogleDriveHelper.DriveCallback() {

                                    @Override
                                    public void onSuccess() {

                                        Toast.makeText(
                                                MainActivity.this,
                                                "☁️ Backup Google Drive berhasil.",
                                                Toast.LENGTH_LONG
                                        ).show();
                                    }

                                    @Override
                                    public void onError(
                                            Exception e
                                    ) {

                                        Toast.makeText(
                                                MainActivity.this,
                                                "Backup gagal: "
                                                        + e.getMessage(),
                                                Toast.LENGTH_LONG
                                        ).show();
                                    }
                                }
                        );
                    }

                    @Override
                    public void onError(
                            Exception e
                    ) {

                        Toast.makeText(
                                MainActivity.this,
                                "Izin Google Drive gagal: "
                                        + e.getMessage(),
                                Toast.LENGTH_LONG
                        ).show();
                    }
                }
        );
    }

    private void backupKeGoogleDriveOtomatis() {

        if (localDataStore.size() == 0) {
            return;
        }

        googleDriveHelper.mintaIzinDrive(
                this,
                new GoogleDriveHelper.DriveCallback() {

                    @Override
                    public void onSuccess() {

                        googleDriveHelper.uploadDatabase(
                                localDataStore.toJson(),
                                new GoogleDriveHelper.DriveCallback() {

                                    @Override
                                    public void onSuccess() {
                                        // Berhasil.
                                    }

                                    @Override
                                    public void onError(
                                            Exception e
                                    ) {
                                        // Backup otomatis gagal.
                                        // Data lokal tetap aman.
                                    }
                                }
                        );
                    }

                    @Override
                    public void onError(
                            Exception e
                    ) {
                        // Data lokal tetap aman.
                    }
                }
        );
    }

    private void restoreDariGoogleDriveOtomatis() {

        googleDriveHelper.mintaIzinDrive(
                this,
                new GoogleDriveHelper.DriveCallback() {

                    @Override
                    public void onSuccess() {

                        googleDriveHelper.downloadDatabase(
                                new GoogleDriveHelper.DataCallback() {

                                    @Override
                                    public void onSuccess(
                                            String json
                                    ) {

                                        if (json == null
                                                || json.trim().isEmpty()) {

                                            return;
                                        }

                                        localDataStore.restoreFromJson(
                                                json
                                        );

                                        jadwalkanSemuaReminder();

                                        runOnUiThread(() -> {

                                            Toast.makeText(
                                                    MainActivity.this,
                                                    "♻️ Data Google Drive berhasil dipulihkan.",
                                                    Toast.LENGTH_LONG
                                            ).show();
                                        });
                                    }

                                    @Override
                                    public void onError(
                                            Exception e
                                    ) {
                                        // Belum ada backup.
                                    }
                                }
                        );
                    }

                    @Override
                    public void onError(
                            Exception e
                    ) {
                        // Belum mendapatkan izin Drive.
                    }
                }
        );
    }

    private void konfirmasiRestore() {

        new AlertDialog.Builder(this)
                .setTitle("♻️ Restore Google Drive")
                .setMessage(
                        "Data lokal di HP akan diganti dengan data backup Google Drive.\n\n"
                                + "Lanjutkan?"
                )
                .setNegativeButton(
                        "BATAL",
                        null
                )
                .setPositiveButton(
                        "RESTORE",
                        (dialog, which) ->
                                restoreManual()
                )
                .show();
    }

    private void restoreManual() {

        Toast.makeText(
                this,
                "Mengambil backup dari Google Drive...",
                Toast.LENGTH_SHORT
        ).show();

        googleDriveHelper.mintaIzinDrive(
                this,
                new GoogleDriveHelper.DriveCallback() {

                    @Override
                    public void onSuccess() {

                        googleDriveHelper.downloadDatabase(
                                new GoogleDriveHelper.DataCallback() {

                                    @Override
                                    public void onSuccess(
                                            String json
                                    ) {

                                        if (json == null
                                                || json.trim().isEmpty()) {

                                            runOnUiThread(() ->
                                                    Toast.makeText(
                                                            MainActivity.this,
                                                            "Backup tidak ditemukan.",
                                                            Toast.LENGTH_LONG
                                                    ).show()
                                            );

                                            return;
                                        }

                                        localDataStore.restoreFromJson(
                                                json
                                        );

                                        jadwalkanSemuaReminder();

                                        runOnUiThread(() -> {

                                            Toast.makeText(
                                                    MainActivity.this,
                                                    "♻️ Restore berhasil.",
                                                    Toast.LENGTH_LONG
                                            ).show();
                                        });
                                    }

                                    @Override
                                    public void onError(
                                            Exception e
                                    ) {

                                        runOnUiThread(() ->
                                                Toast.makeText(
                                                        MainActivity.this,
                                                        "Restore gagal: "
                                                                + e.getMessage(),
                                                        Toast.LENGTH_LONG
                                                ).show()
                                        );
                                    }
                                }
                        );
                    }

                    @Override
                    public void onError(
                            Exception e
                    ) {

                        Toast.makeText(
                                MainActivity.this,
                                "Google Drive tidak bisa diakses.",
                                Toast.LENGTH_LONG
                        ).show();
                    }
                }
        );
    }

    // ============================================================
    // UTIL
    // ============================================================

    @Override
    protected void onDestroy() {
        super.onDestroy();

        if (googleDriveHelper != null) {
            googleDriveHelper.shutdown();
        }
    }
}
