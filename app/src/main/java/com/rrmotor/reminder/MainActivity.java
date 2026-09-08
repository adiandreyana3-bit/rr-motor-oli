package com.rrmotor.reminder;

import android.Manifest;
import android.app.AlarmManager;
import android.app.DatePickerDialog;
import android.app.PendingIntent;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.ContactsContract;
import android.provider.Settings;
import android.text.InputType;
import android.view.Gravity;
import android.widget.*;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;

import java.net.URLEncoder;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

public class MainActivity extends AppCompatActivity {

    private FirebaseAuth auth;
    private FirebaseFirestore db;

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

    private String tanggalTerpilih = "";

    private static final int CONTACT_PICKER_REQUEST = 1001;

    private final ActivityResultLauncher<String> izinKontakLauncher =
            registerForActivityResult(
                    new ActivityResultContracts.RequestPermission(),
                    diberikan -> {
                        if (diberikan) {
                            bukaKontak();
                        } else {
                            Toast.makeText(
                                    this,
                                    "Izin kontak diperlukan untuk memilih nomor WhatsApp.",
                                    Toast.LENGTH_LONG
                            ).show();
                        }
                    }
            );

    private final ActivityResultLauncher<String> izinNotifikasiLauncher =
            registerForActivityResult(
                    new ActivityResultContracts.RequestPermission(),
                    diberikan -> {
                        if (!diberikan) {
                            Toast.makeText(
                                    this,
                                    "Izin notifikasi tidak diberikan. Reminder tidak akan tampil.",
                                    Toast.LENGTH_LONG
                            ).show();
                        }
                    }
            );

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        auth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();

        if (auth.getCurrentUser() == null) {
            kembaliKeLogin();
            return;
        }

        buatTampilan();

        cekLoginFirebase();
        bersihkanRiwayatLama();

        prosesNotifikasiIntent(getIntent());
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);

        setIntent(intent);

        prosesNotifikasiIntent(intent);
    }

    // =====================================================
    // PROSES NOTIFIKASI
    // =====================================================

    private void prosesNotifikasiIntent(Intent intent) {

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
                intent.getStringExtra("reminder_wa");

        String pesan =
                intent.getStringExtra("reminder_pesan");

        if (wa != null &&
                !wa.trim().isEmpty()) {

            bukaWhatsAppDariNotifikasi(
                    wa,
                    pesan
            );

            // Supaya intent tidak diproses lagi
            intent.removeExtra(
                    "reminder_dari_notifikasi"
            );
        }
    }

    private void kembaliKeLogin() {

        Intent intent =
                new Intent(
                        MainActivity.this,
                        LoginActivity.class
                );

        startActivity(intent);
        finish();
    }

    // =====================================================
    // TAMPILAN UTAMA
    // =====================================================

    private void buatTampilan() {

        ScrollView scrollView =
                new ScrollView(this);

        scrollView.setFillViewport(true);
        scrollView.setClipToPadding(false);

        LinearLayout utama =
                new LinearLayout(this);

        utama.setOrientation(
                LinearLayout.VERTICAL
        );

        utama.setPadding(
                25,
                20,
                25,
                40
        );

        scrollView.addView(utama);

        TextView judul =
                new TextView(this);

        judul.setText("🏍️ RR MOTOR");
        judul.setTextSize(17);
        judul.setGravity(Gravity.CENTER);
        judul.setPadding(0, 0, 0, 5);

        TextView subjudul =
                new TextView(this);

        subjudul.setText(
                "REMINDER GANTI OLI"
        );

        subjudul.setTextSize(14);
        subjudul.setGravity(Gravity.CENTER);
        subjudul.setPadding(
                0,
                0,
                0,
                20
        );

        utama.addView(judul);
        utama.addView(subjudul);

        // =====================================================
        // NAMA
        // =====================================================

        namaInput =
                buatInput(
                        "Nama pelanggan (opsional)",
                        InputType.TYPE_CLASS_TEXT
                );

        utama.addView(namaInput);

        // =====================================================
        // NOPOL
        // =====================================================

        nopolInput =
                buatInput(
                        "Nopol (opsional)",
                        InputType.TYPE_CLASS_TEXT
                );

        utama.addView(nopolInput);

        // =====================================================
        // NOMOR MESIN
        // =====================================================

        mesinInput =
                buatInput(
                        "Nomor mesin (opsional)",
                        InputType.TYPE_CLASS_TEXT
                );

        utama.addView(mesinInput);

        // =====================================================
        // KM
        // =====================================================

        kmInput =
                buatInput(
                        "KM terakhir (opsional)",
                        InputType.TYPE_CLASS_NUMBER
                );

        utama.addView(kmInput);

        hasilKmText =
                new TextView(this);

        hasilKmText.setTextSize(15);

        hasilKmText.setPadding(
                5,
                0,
                5,
                15
        );

        utama.addView(hasilKmText);

        kmInput.setOnFocusChangeListener(
                (v, hasFocus) -> {

                    if (!hasFocus) {
                        tampilkanPerhitunganKm();
                    }
                }
        );

        kmInput.setOnEditorActionListener(
                (v, actionId, event) -> {

                    tampilkanPerhitunganKm();

                    return false;
                }
        );

        // =====================================================
        // WHATSAPP
        // =====================================================

        TextView labelWa =
                new TextView(this);

        labelWa.setText(
                "Nomor WhatsApp *"
        );

        labelWa.setTextSize(14);

        labelWa.setPadding(
                5,
                10,
                5,
                5
        );

        utama.addView(labelWa);

        LinearLayout barisWa =
                new LinearLayout(this);

        barisWa.setOrientation(
                LinearLayout.HORIZONTAL
        );

        barisWa.setGravity(
                Gravity.CENTER_VERTICAL
        );

        waInput =
                new EditText(this);

        waInput.setHint(
                "08xxxxxxxxxx"
        );

        waInput.setSingleLine(true);

        waInput.setInputType(
                InputType.TYPE_CLASS_PHONE
        );

        LinearLayout.LayoutParams waParams =
                new LinearLayout.LayoutParams(
                        0,
                        LinearLayout.LayoutParams.WRAP_CONTENT,
                        1
                );

        barisWa.addView(
                waInput,
                waParams
        );

        Button kontakButton =
                new Button(this);

        kontakButton.setText(
                "📱 KONTAK"
        );

        LinearLayout.LayoutParams kontakParams =
                new LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.WRAP_CONTENT,
                        LinearLayout.LayoutParams.WRAP_CONTENT
                );

        kontakParams.setMargins(
                8,
                0,
                0,
                0
        );

        barisWa.addView(
                kontakButton,
                kontakParams
        );

        kontakButton.setOnClickListener(
                v -> {

                    if (ContextCompat.checkSelfPermission(
                            this,
                            Manifest.permission.READ_CONTACTS
                    ) == PackageManager.PERMISSION_GRANTED) {

                        bukaKontak();

                    } else {

                        izinKontakLauncher.launch(
                                Manifest.permission.READ_CONTACTS
                        );
                    }
                }
        );

        utama.addView(barisWa);

        // =====================================================
        // TANGGAL INPUT
        // =====================================================

        TextView labelTanggal =
                new TextView(this);

        labelTanggal.setText(
                "Tanggal input data *"
        );

        labelTanggal.setTextSize(14);

        labelTanggal.setPadding(
                5,
                15,
                5,
                5
        );

        utama.addView(labelTanggal);

        tanggalInput =
                buatInput(
                        "Pilih tanggal",
                        InputType.TYPE_CLASS_DATETIME
                );

        tanggalInput.setFocusable(false);
        tanggalInput.setClickable(true);

        tanggalInput.setOnClickListener(
                v -> tampilkanDatePicker()
        );

        utama.addView(tanggalInput);

        // =====================================================
        // JATUH TEMPO
        // =====================================================

        TextView labelJatuhTempo =
                new TextView(this);

        labelJatuhTempo.setText(
                "Jatuh tempo reminder"
        );

        labelJatuhTempo.setTextSize(14);

        labelJatuhTempo.setPadding(
                5,
                15,
                5,
                5
        );

        utama.addView(labelJatuhTempo);

        jatuhTempoSpinner =
                new Spinner(this);

        String[] pilihanTempo = {
                "1 BULAN",
                "2 BULAN"
        };

        ArrayAdapter<String> adapter =
                new ArrayAdapter<>(
                        this,
                        android.R.layout.simple_spinner_dropdown_item,
                        pilihanTempo
                );

        jatuhTempoSpinner.setAdapter(
                adapter
        );

        utama.addView(
                jatuhTempoSpinner
        );

        // =====================================================
        // SIMPAN
        // =====================================================

        simpanButton =
                new Button(this);

        simpanButton.setText(
                "💾 SIMPAN DATA"
        );

        simpanButton.setOnClickListener(
                v -> simpanData()
        );

        utama.addView(
                simpanButton
        );

        // =====================================================
        // DATA BARU
        // =====================================================

        dataBaruButton =
                new Button(this);

        dataBaruButton.setText(
                "➕ DATA BARU"
        );

        dataBaruButton.setOnClickListener(
                v -> dataBaru()
        );

        utama.addView(
                dataBaruButton
        );

        // =====================================================
        // RIWAYAT
        // =====================================================

        riwayatButton =
                new Button(this);

        riwayatButton.setText(
                "📋 RIWAYAT REMINDER"
        );

        riwayatButton.setOnClickListener(
                v -> tampilkanRiwayat()
        );

        utama.addView(
                riwayatButton
        );

        setContentView(scrollView);
    }

    private EditText buatInput(
            String hint,
            int inputType
    ) {

        EditText input =
                new EditText(this);

        input.setHint(hint);
        input.setSingleLine(true);
        input.setTextSize(16);
        input.setInputType(inputType);

        LinearLayout.LayoutParams params =
                new LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.MATCH_PARENT,
                        LinearLayout.LayoutParams.WRAP_CONTENT
                );

        params.setMargins(
                0,
                5,
                0,
                5
        );

        input.setLayoutParams(params);

        return input;
    }

    // =====================================================
    // DATE PICKER
    // =====================================================

    private void tampilkanDatePicker() {

        Calendar kalender =
                Calendar.getInstance();

        DatePickerDialog dialog =
                new DatePickerDialog(
                        this,
                        (view, year, month, dayOfMonth) -> {

                            Calendar pilih =
                                    Calendar.getInstance();

                            pilih.set(
                                    year,
                                    month,
                                    dayOfMonth,
                                    0,
                                    0,
                                    0
                            );

                            pilih.set(
                                    Calendar.MILLISECOND,
                                    0
                            );

                            SimpleDateFormat format =
                                    new SimpleDateFormat(
                                            "dd-MM-yyyy",
                                            Locale.getDefault()
                                    );

                            tanggalTerpilih =
                                    format.format(
                                            pilih.getTime()
                                    );

                            tanggalInput.setText(
                                    tanggalTerpilih
                            );
                        },
                        kalender.get(
                                Calendar.YEAR
                        ),
                        kalender.get(
                                Calendar.MONTH
                        ),
                        kalender.get(
                                Calendar.DAY_OF_MONTH
                        )
                );

        dialog.show();
    }

    // =====================================================
    // HITUNG KM
    // =====================================================

    private void tampilkanPerhitunganKm() {

        String teksKm =
                kmInput.getText()
                        .toString()
                        .trim();

        if (teksKm.isEmpty()) {

            hasilKmText.setText("");

            return;
        }

        try {

            long km =
                    Long.parseLong(
                            teksKm.replace(
                                    ".",
                                    ""
                            )
                    );

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
                    "Maksimal ganti oli: "
                            + formatKm(maksimal)
                            + " KM\n"
                            + "Paling lambat: "
                            + formatKm(palingLambat)
                            + " KM"
            );

        } catch (Exception e) {

            hasilKmText.setText("");
        }
    }

    private long hitungKelipatanBerikutnya(
            long km,
            long kelipatan
    ) {

        if (km < 0) {
            return kelipatan;
        }

        return km + kelipatan;
    }

    private String formatKm(long angka) {

        return String.format(
                Locale.getDefault(),
                "%,d",
                angka
        ).replace(
                ",",
                "."
        );
    }

    // =====================================================
    // KONTAK
    // =====================================================

    private void bukaKontak() {

        try {

            Intent intent =
                    new Intent(
                            Intent.ACTION_PICK,
                            ContactsContract.CommonDataKinds.Phone.CONTENT_URI
                    );

            startActivityForResult(
                    intent,
                    CONTACT_PICKER_REQUEST
            );

        } catch (Exception e) {

            Toast.makeText(
                    this,
                    "Tidak dapat membuka kontak.",
                    Toast.LENGTH_LONG
            ).show();
        }
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

        if (requestCode != CONTACT_PICKER_REQUEST) {
            return;
        }

        if (resultCode != RESULT_OK ||
                data == null) {

            return;
        }

        Uri contactUri =
                data.getData();

        if (contactUri == null) {
            return;
        }

        Cursor cursor = null;

        try {

            cursor =
                    getContentResolver().query(
                            contactUri,
                            new String[]{
                                    ContactsContract.CommonDataKinds.Phone.NUMBER
                            },
                            null,
                            null,
                            null
                    );

            if (cursor != null &&
                    cursor.moveToFirst()) {

                int index =
                        cursor.getColumnIndex(
                                ContactsContract.CommonDataKinds.Phone.NUMBER
                        );

                if (index >= 0) {

                    String nomor =
                            cursor.getString(index);

                    waInput.setText(nomor);
                }
            }

        } catch (Exception e) {

            Toast.makeText(
                    this,
                    "Nomor kontak tidak dapat dibaca.",
                    Toast.LENGTH_LONG
            ).show();

        } finally {

            if (cursor != null) {
                cursor.close();
            }
        }
    }

    // =====================================================
    // SIMPAN DATA
    // =====================================================

    private void simpanData() {

        if (auth.getCurrentUser() == null) {

            kembaliKeLogin();

            return;
        }

        String nama =
                namaInput.getText()
                        .toString()
                        .trim();

        String nopol =
                nopolInput.getText()
                        .toString()
                        .trim();

        String mesin =
                mesinInput.getText()
                        .toString()
                        .trim();

        String km =
                kmInput.getText()
                        .toString()
                        .trim();

        String wa =
                waInput.getText()
                        .toString()
                        .trim();

        String tanggal =
                tanggalInput.getText()
                        .toString()
                        .trim();

        if (wa.isEmpty()) {

            waInput.setError(
                    "Nomor WhatsApp wajib diisi"
            );

            waInput.requestFocus();

            return;
        }

        if (tanggal.isEmpty()) {

            tanggalInput.setError(
                    "Tanggal input wajib diisi"
            );

            tanggalInput.requestFocus();

            Toast.makeText(
                    this,
                    "Tanggal input data wajib diisi.",
                    Toast.LENGTH_LONG
            ).show();

            return;
        }

        String pilihan =
                jatuhTempoSpinner
                        .getSelectedItem()
                        .toString();

        int jumlahBulan = 1;

        if (pilihan.startsWith("2")) {
            jumlahBulan = 2;
        }

        String pesan =
                buatPesanWhatsApp(
                        nama,
                        nopol,
                        mesin,
                        km,
                        jumlahBulan
                );

        long waktuInput =
                System.currentTimeMillis();

        long waktuReminder =
                hitungTanggalReminder(
                        tanggal,
                        jumlahBulan
                );

        if (waktuReminder <= 0) {

            Toast.makeText(
                    this,
                    "Tanggal tidak valid.",
                    Toast.LENGTH_LONG
            ).show();

            return;
        }

        Map<String, Object> data =
                new HashMap<>();

        data.put("nama", nama);
        data.put("nopol", nopol);
        data.put("nomorMesin", mesin);
        data.put("kmTerakhir", km);
        data.put("whatsapp", wa);
        data.put("tanggalInput", tanggal);
        data.put("jatuhTempo", pilihan);
        data.put("pesanWhatsApp", pesan);
        data.put(
                "waktuReminder",
                waktuReminder
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
                waktuInput
        );

        simpanButton.setEnabled(false);

        db.collection("reminders")
                .add(data)
                .addOnSuccessListener(
                        documentReference -> {

                            String documentId =
                                    documentReference.getId();

                            jadwalkanReminder(
                                    waktuReminder,
                                    documentId,
                                    nama,
                                    wa,
                                    pesan
                            );

                            Toast.makeText(
                                    this,
                                    "Data berhasil disimpan.\nReminder sudah dijadwalkan.",
                                    Toast.LENGTH_LONG
                            ).show();

                            simpanButton.setEnabled(
                                    false
                            );
                        }
                )
                .addOnFailureListener(
                        e -> {

                            simpanButton.setEnabled(
                                    true
                            );

                            Toast.makeText(
                                    this,
                                    "Gagal menyimpan data: "
                                            + e.getMessage(),
                                    Toast.LENGTH_LONG
                            ).show();
                        }
                );
    }

    // =====================================================
    // PESAN WHATSAPP
    // =====================================================

    private String buatPesanWhatsApp(
            String nama,
            String nopol,
            String mesin,
            String km,
            int jumlahBulan
    ) {

        String sapaan;

        if (nama.isEmpty()) {

            sapaan =
                    "Halo Bapak/Ibu.";

        } else {

            sapaan =
                    "Halo Bapak/Ibu "
                            + nama
                            + ".";
        }

        StringBuilder pesan =
                new StringBuilder();

        pesan.append(
                "🏍️ RR MOTOR\n\n"
        );

        pesan.append(
                sapaan
        ).append("\n\n");

        pesan.append(
                "Sudah "
        )
                .append(jumlahBulan)
                .append(
                        " bulan sejak terakhir ganti oli di RR MOTOR."
                )
                .append("\n\n");

        pesan.append(
                "Kami mengingatkan untuk melakukan pengecekan atau pergantian oli motor."
        ).append("\n\n");

        if (!nopol.isEmpty()) {

            pesan.append(
                    "Nopol: "
            )
                    .append(nopol)
                    .append("\n");
        }

        if (!mesin.isEmpty()) {

            pesan.append(
                    "Nomor mesin: "
            )
                    .append(mesin)
                    .append("\n");
        }

        if (!nopol.isEmpty() ||
                !mesin.isEmpty()) {

            pesan.append("\n");
        }

        if (!km.isEmpty()) {

            try {

                long angkaKm =
                        Long.parseLong(
                                km.replace(
                                        ".",
                                        ""
                                )
                        );

                long maksimal =
                        hitungKelipatanBerikutnya(
                                angkaKm,
                                1500
                        );

                long palingLambat =
                        hitungKelipatanBerikutnya(
                                angkaKm,
                                2000
                        );

                pesan.append(
                        "KM terakhir: "
                )
                        .append(
                                formatKm(angkaKm)
                        )
                        .append(
                                " KM\n"
                        );

                pesan.append(
                        "Maksimal ganti oli: "
                )
                        .append(
                                formatKm(maksimal)
                        )
                        .append(
                                " KM\n"
                        );

                pesan.append(
                        "Paling lambat: "
                )
                        .append(
                                formatKm(palingLambat)
                        )
                        .append(
                                " KM\n\n"
                        );

            } catch (Exception ignored) {
            }
        }

        pesan.append(
                "Pengecekan oli GRATIS."
        ).append("\n\n");

        pesan.append(
                "Terima kasih telah mempercayakan perawatan motor Anda kepada RR MOTOR."
        );

        return pesan.toString();
    }

    // =====================================================
    // TANGGAL REMINDER
    // =====================================================

    private long hitungTanggalReminder(
            String tanggal,
            int jumlahBulan
    ) {

        try {

            SimpleDateFormat format =
                    new SimpleDateFormat(
                            "dd-MM-yyyy",
                            Locale.getDefault()
                    );

            format.setLenient(false);

            Date tanggalDate =
                    format.parse(tanggal);

            if (tanggalDate == null) {
                return 0;
            }

            Calendar kalender =
                    Calendar.getInstance();

            kalender.setTime(
                    tanggalDate
            );

            kalender.add(
                    Calendar.MONTH,
                    jumlahBulan
            );

            kalender.set(
                    Calendar.HOUR_OF_DAY,
                    9
            );

            kalender.set(
                    Calendar.MINUTE,
                    0
            );

            kalender.set(
                    Calendar.SECOND,
                    0
            );

            kalender.set(
                    Calendar.MILLISECOND,
                    0
            );

            return kalender.getTimeInMillis();

        } catch (Exception e) {

            return 0;
        }
    }

    // =====================================================
    // JADWALKAN REMINDER
    // =====================================================

    private void jadwalkanReminder(
            long waktu,
            String documentId,
            String nama,
            String wa,
            String pesan
    ) {

        AlarmManager alarm =
                (AlarmManager)
                        getSystemService(
                                ALARM_SERVICE
                        );

        if (alarm == null) {
            return;
        }

        Intent intent =
                new Intent(
                        this,
                        ReminderReceiver.class
                );

        intent.putExtra(
                "documentId",
                documentId
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

        int requestCode =
                Math.abs(
                        documentId.hashCode()
                );

        PendingIntent pending =
                PendingIntent.getBroadcast(
                        this,
                        requestCode,
                        intent,
                        PendingIntent.FLAG_UPDATE_CURRENT |
                                PendingIntent.FLAG_IMMUTABLE
                );

        alarm.cancel(pending);

        if (Build.VERSION.SDK_INT >=
                Build.VERSION_CODES.S) {

            if (!alarm.canScheduleExactAlarms()) {

                try {

                    Intent settingsIntent =
                            new Intent(
                                    Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM
                            );

                    settingsIntent.setData(
                            Uri.parse(
                                    "package:"
                                            + getPackageName()
                            )
                    );

                    startActivity(
                            settingsIntent
                    );

                } catch (Exception ignored) {
                }

                alarm.setAndAllowWhileIdle(
                        AlarmManager.RTC_WAKEUP,
                        waktu,
                        pending
                );

                return;
            }

            alarm.setExactAndAllowWhileIdle(
                    AlarmManager.RTC_WAKEUP,
                    waktu,
                    pending
            );

        } else {

            alarm.setAndAllowWhileIdle(
                    AlarmManager.RTC_WAKEUP,
                    waktu,
                    pending
            );
        }
    }

    // =====================================================
    // WHATSAPP
    // =====================================================

    private void bukaWhatsAppDariNotifikasi(
            String nomor,
            String pesan
    ) {

        try {

            String nomorBersih =
                    nomor.replaceAll(
                            "[^0-9]",
                            ""
                    );

            if (nomorBersih.startsWith("0")) {

                nomorBersih =
                        "62"
                                + nomorBersih.substring(1);
            }

            if (nomorBersih.isEmpty()) {

                Toast.makeText(
                        this,
                        "Nomor WhatsApp tidak valid.",
                        Toast.LENGTH_LONG
                ).show();

                return;
            }

            if (pesan == null) {
                pesan = "";
            }

            String encoded =
                    URLEncoder.encode(
                            pesan,
                            "UTF-8"
                    );

            Intent intent =
                    new Intent(
                            Intent.ACTION_VIEW,
                            Uri.parse(
                                    "https://wa.me/"
                                            + nomorBersih
                                            + "?text="
                                            + encoded
                            )
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

    // =====================================================
    // DATA BARU
    // =====================================================

    private void dataBaru() {

        namaInput.setText("");
        nopolInput.setText("");
        mesinInput.setText("");
        kmInput.setText("");
        waInput.setText("");
        tanggalInput.setText("");

        tanggalTerpilih = "";

        jatuhTempoSpinner.setSelection(0);

        hasilKmText.setText("");

        simpanButton.setEnabled(true);

        namaInput.requestFocus();

        Toast.makeText(
                this,
                "Form data baru siap diisi.",
                Toast.LENGTH_SHORT
        ).show();
    }

    // =====================================================
    // RIWAYAT
    // =====================================================

    private void tampilkanRiwayat() {

        if (auth.getCurrentUser() == null) {

            kembaliKeLogin();

            return;
        }

        String[] pilihanFilter = {
                "SEMUA",
                "BELUM TERKIRIM",
                "TERKIRIM"
        };

        new android.app.AlertDialog.Builder(this)
                .setTitle(
                        "📋 RIWAYAT REMINDER"
                )
                .setItems(
                        pilihanFilter,
                        (dialog, which) -> {

                            if (which == 0) {

                                ambilRiwayat(
                                        "SEMUA"
                                );

                            } else if (which == 1) {

                                ambilRiwayat(
                                        "BELUM TERKIRIM"
                                );

                            } else {

                                ambilRiwayat(
                                        "TERKIRIM"
                                );
                            }
                        }
                )
                .setNegativeButton(
                        "BATAL",
                        null
                )
                .show();
    }

    private void ambilRiwayat(
            String filter
    ) {

        Query query =
                db.collection("reminders")
                        .orderBy(
                                "waktuSimpan",
                                Query.Direction.DESCENDING
                        )
                        .limit(100);

        if (filter.equals(
                "BELUM TERKIRIM"
        )) {

            query =
                    db.collection("reminders")
                            .whereEqualTo(
                                    "reminderTerkirim",
                                    false
                            )
                            .orderBy(
                                    "waktuSimpan",
                                    Query.Direction.DESCENDING
                            )
                            .limit(100);
        }

        if (filter.equals(
                "TERKIRIM"
        )) {

            query =
                    db.collection("reminders")
                            .whereEqualTo(
                                    "reminderTerkirim",
                                    true
                            )
                            .orderBy(
                                    "waktuSimpan",
                                    Query.Direction.DESCENDING
                            )
                            .limit(100);
        }

        query.get()
                .addOnSuccessListener(
                        querySnapshot -> {

                            ArrayList<DocumentSnapshot>
                                    dokumen =
                                    new ArrayList<>();

                            ArrayList<String>
                                    daftar =
                                    new ArrayList<>();

                            for (
                                    DocumentSnapshot doc :
                                    querySnapshot.getDocuments()
                            ) {

                                dokumen.add(doc);

                                String nama =
                                        doc.getString(
                                                "nama"
                                        );

                                String wa =
                                        doc.getString(
                                                "whatsapp"
                                        );

                                String tanggal =
                                        doc.getString(
                                                "tanggalInput"
                                        );

                                Boolean terkirim =
                                        doc.getBoolean(
                                                "reminderTerkirim"
                                        );

                                if (nama == null ||
                                        nama.trim().isEmpty()) {

                                    nama =
                                            "Bapak/Ibu";
                                }

                                if (wa == null ||
                                        wa.trim().isEmpty()) {

                                    wa = "-";
                                }

                                if (tanggal == null ||
                                        tanggal.trim().isEmpty()) {

                                    tanggal = "-";
                                }

                                String status =
                                        Boolean.TRUE.equals(
                                                terkirim
                                        )
                                                ? "TERKIRIM"
                                                : "BELUM TERKIRIM";

                                String teks =
                                        nama
                                                + "\nWA: "
                                                + wa
                                                + "\nTanggal input: "
                                                + tanggal
                                                + "\nStatus: "
                                                + status;

                                daftar.add(teks);
                            }

                            if (daftar.isEmpty()) {

                                new android.app.AlertDialog.Builder(
                                        this
                                )
                                        .setTitle(
                                                "RIWAYAT "
                                                        + filter
                                        )
                                        .setMessage(
                                                "Tidak ada data."
                                        )
                                        .setPositiveButton(
                                                "TUTUP",
                                                null
                                        )
                                        .show();

                                return;
                            }

                            String[] array =
                                    daftar.toArray(
                                            new String[0]
                                    );

                            new android.app.AlertDialog.Builder(
                                    this
                            )
                                    .setTitle(
                                            "RIWAYAT - "
                                                    + filter
                                    )
                                    .setItems(
                                            array,
                                            (dialog, which) -> {

                                                if (which >= 0 &&
                                                        which < dokumen.size()) {

                                                    tampilkanDetailRiwayat(
                                                            dokumen.get(which)
                                                    );
                                                }
                                            }
                                    )
                                    .setPositiveButton(
                                            "TUTUP",
                                            null
                                    )
                                    .show();
                        }
                )
                .addOnFailureListener(
                        e -> {

                            Toast.makeText(
                                    this,
                                    "Gagal mengambil riwayat: "
                                            + e.getMessage(),
                                    Toast.LENGTH_LONG
                            ).show();
                        }
                );
    }

    // =====================================================
    // DETAIL RIWAYAT
    // =====================================================

    private void tampilkanDetailRiwayat(
            DocumentSnapshot doc
    ) {

        String nama =
                doc.getString("nama");

        String nopol =
                doc.getString("nopol");

        String mesin =
                doc.getString("nomorMesin");

        String km =
                doc.getString("kmTerakhir");

        String wa =
                doc.getString("whatsapp");

        String tanggal =
                doc.getString("tanggalInput");

        String jatuhTempo =
                doc.getString("jatuhTempo");

        String pesan =
                doc.getString("pesanWhatsApp");

        Boolean terkirim =
                doc.getBoolean(
                        "reminderTerkirim"
                );

        if (nama == null ||
                nama.trim().isEmpty()) {

            nama = "Bapak/Ibu";
        }

        if (nopol == null ||
                nopol.trim().isEmpty()) {

            nopol = "-";
        }

        if (mesin == null ||
                mesin.trim().isEmpty()) {

            mesin = "-";
        }

        if (km == null ||
                km.trim().isEmpty()) {

            km = "-";
        }

        if (wa == null ||
                wa.trim().isEmpty()) {

            wa = "-";
        }

        if (tanggal == null ||
                tanggal.trim().isEmpty()) {

            tanggal = "-";
        }

        if (jatuhTempo == null ||
                jatuhTempo.trim().isEmpty()) {

            jatuhTempo = "-";
        }

        String status =
                Boolean.TRUE.equals(
                        terkirim
                )
                        ? "TERKIRIM"
                        : "BELUM TERKIRIM";

        StringBuilder detail =
                new StringBuilder();

        detail.append(
                "Nama: "
        ).append(nama)
                .append("\n\n");

        detail.append(
                "WhatsApp: "
        ).append(wa)
                .append("\n\n");

        detail.append(
                "Nopol: "
        ).append(nopol)
                .append("\n\n");

        detail.append(
                "Nomor mesin: "
        ).append(mesin)
                .append("\n\n");

        detail.append(
                "Tanggal input: "
        ).append(tanggal)
                .append("\n\n");

        detail.append(
                "Jatuh tempo: "
        ).append(jatuhTempo)
                .append("\n\n");

        detail.append(
                "KM terakhir: "
        ).append(km)
                .append("\n");

        if (!km.equals("-")) {

            try {

                long angkaKm =
                        Long.parseLong(
                                km.replace(
                                        ".",
                                        ""
                                )
                        );

                detail.append(
                        "Maksimal ganti oli: "
                ).append(
                        formatKm(
                                angkaKm + 1500
                        )
                ).append(
                        " KM\n"
                );

                detail.append(
                        "Paling lambat: "
                ).append(
                        formatKm(
                                angkaKm + 2000
                        )
                ).append(
                        " KM\n"
                );

            } catch (Exception ignored) {
            }
        }

        detail.append("\n");

        detail.append(
                "Status: "
        ).append(status);

        if (pesan != null &&
                !pesan.trim().isEmpty()) {

            detail.append(
                    "\n\nPesan WhatsApp:\n\n"
            );

            detail.append(pesan);
        }

        new android.app.AlertDialog.Builder(this)
                .setTitle(
                        "DETAIL REMINDER"
                )
                .setMessage(
                        detail.toString()
                )
                .setPositiveButton(
                        "TUTUP",
                        null
                )
                .show();
    }

    // =====================================================
    // PEMBERSIHAN RIWAYAT LAMA
    // HANYA TERKIRIM YANG BOLEH DIHAPUS
    // =====================================================

    private void bersihkanRiwayatLama() {

        if (auth.getCurrentUser() == null) {
            return;
        }

        long sekarang =
                System.currentTimeMillis();

        db.collection("reminders")
                .whereEqualTo(
                        "reminderTerkirim",
                        true
                )
                .whereLessThanOrEqualTo(
                        "deleteAt",
                        sekarang
                )
                .get()
                .addOnSuccessListener(
                        querySnapshot -> {

                            for (
                                    DocumentSnapshot doc :
                                    querySnapshot.getDocuments()
                            ) {

                                doc.getReference()
                                        .delete();
                            }
                        }
                )
                .addOnFailureListener(
                        e -> {
                            // Tidak mengganggu aplikasi
                        }
                );
    }

    // =====================================================
    // LOGIN + NOTIFIKASI
    // =====================================================

    private void cekLoginFirebase() {

        if (auth.getCurrentUser() == null) {

            kembaliKeLogin();

            return;
        }

        mintaIzinNotifikasi();
    }

    private void mintaIzinNotifikasi() {

        if (Build.VERSION.SDK_INT >= 33) {

            if (ContextCompat.checkSelfPermission(
                    this,
                    Manifest.permission.POST_NOTIFICATIONS
            ) != PackageManager.PERMISSION_GRANTED) {

                izinNotifikasiLauncher.launch(
                        Manifest.permission.POST_NOTIFICATIONS
                );
            }
        }
    }
}
