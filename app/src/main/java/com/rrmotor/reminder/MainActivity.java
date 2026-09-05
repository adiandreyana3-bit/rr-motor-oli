package com.rrmotor.reminder;

import android.Manifest;
import android.app.DatePickerDialog;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.provider.ContactsContract;
import android.view.View;
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

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Locale;

public class MainActivity extends AppCompatActivity {

private EditText namaInput;
private EditText nopolInput;
private EditText mesinInput;
private EditText kmInput;
private EditText waInput;
private EditText tanggalInput;

private Spinner jatuhTempoSpinner;
private Button simpanButton;

private final Calendar kalender = Calendar.getInstance();

private final ActivityResultLauncher<String> izinKontak =
        registerForActivityResult(
                new ActivityResultContracts.RequestPermission(),
                diberikan -> {
                    if (diberikan) {
                        bukaPemilihKontak();
                    } else {
                        Toast.makeText(
                                this,
                                "Izin kontak diperlukan untuk memilih nomor WhatsApp.",
                                Toast.LENGTH_SHORT
                        ).show();
                    }
                }
        );

private final ActivityResultLauncher<Intent> pilihKontak =
        registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                hasil -> {
                    if (hasil.getResultCode() == RESULT_OK
                            && hasil.getData() != null
                            && hasil.getData().getData() != null) {

                        ambilNomorDariKontak(
                                hasil.getData().getData()
                        );
                    }
                }
        );

@Override
protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);

    setTitle("RR MOTOR REMINDER");

    buatTampilan();

    tanggalInput.setText(
            formatTanggal(kalender)
    );
}

private void buatTampilan() {

    ScrollView scrollView = new ScrollView(this);
    scrollView.setFillViewport(true);

    LinearLayout utama = new LinearLayout(this);
    utama.setOrientation(LinearLayout.VERTICAL);
    utama.setPadding(32, 24, 32, 32);

    TextView judul = new TextView(this);
    judul.setText("🏍️ RR MOTOR REMINDER");
    judul.setTextSize(22);
    judul.setPadding(0, 0, 0, 24);

    utama.addView(judul);

    namaInput = buatInput(
            "Nama pelanggan (opsional)"
    );
    utama.addView(namaInput);

    nopolInput = buatInput(
            "Nopol (opsional)"
    );
    utama.addView(nopolInput);

    mesinInput = buatInput(
            "Nomor mesin (opsional)"
    );
    utama.addView(mesinInput);

    kmInput = buatInput(
            "KM terakhir (opsional)"
    );
    kmInput.setInputType(
            android.text.InputType.TYPE_CLASS_NUMBER
    );
    utama.addView(kmInput);

    TextView labelWa = new TextView(this);
    labelWa.setText("Nomor WhatsApp *");
    labelWa.setTextSize(16);
    labelWa.setPadding(0, 16, 0, 8);
    utama.addView(labelWa);

    LinearLayout barisWa = new LinearLayout(this);
    barisWa.setOrientation(LinearLayout.HORIZONTAL);

    waInput = new EditText(this);
    waInput.setHint("08xxxxxxxxxx");
    waInput.setInputType(
            android.text.InputType.TYPE_CLASS_PHONE
    );

    barisWa.addView(
            waInput,
            new LinearLayout.LayoutParams(
                    0,
                    LinearLayout.LayoutParams.WRAP_CONTENT,
                    1
            )
    );

    Button kontakButton = new Button(this);
    kontakButton.setText("📱 KONTAK");

    kontakButton.setOnClickListener(
            v -> mintaIzinKontak()
    );

    barisWa.addView(
            kontakButton,
            new LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.WRAP_CONTENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
            )
    );

    utama.addView(barisWa);

    tanggalInput = buatInput(
            "Tanggal input data"
    );
    tanggalInput.setFocusable(false);
    tanggalInput.setClickable(true);

    tanggalInput.setOnClickListener(
            v -> tampilkanKalender()
    );

    utama.addView(tanggalInput);

    TextView labelTempo = new TextView(this);
    labelTempo.setText("Jatuh tempo reminder");
    labelTempo.setTextSize(16);
    labelTempo.setPadding(0, 16, 0, 8);
    utama.addView(labelTempo);

    jatuhTempoSpinner = new Spinner(this);

    String[] pilihanTempo = {
            "1 bulan",
            "2 bulan"
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

    Button hitungButton = new Button(this);
    hitungButton.setText("HITUNG REMINDER");

    hitungButton.setOnClickListener(
            v -> tampilkanPerhitungan()
    );

    utama.addView(hitungButton);

    simpanButton = new Button(this);
    simpanButton.setText("💾 SIMPAN");

    simpanButton.setOnClickListener(
            v -> simpanDataSementara()
    );

    utama.addView(simpanButton);

    Button riwayatButton = new Button(this);
    riwayatButton.setText("📋 RIWAYAT");

    riwayatButton.setOnClickListener(
            v -> Toast.makeText(
                    this,
                    "Menu Riwayat akan kita sambungkan ke Firebase pada tahap berikutnya.",
                    Toast.LENGTH_LONG
            ).show()
    );

    utama.addView(riwayatButton);

    TextView catatan = new TextView(this);
    catatan.setText(
            "\nNomor WhatsApp wajib diisi.\n" +
            "Nama, Nopol, nomor mesin dan KM boleh dikosongkan."
    );
    catatan.setTextSize(14);

    utama.addView(catatan);

    scrollView.addView(utama);

    setContentView(scrollView);
}

private EditText buatInput(String hint) {

    EditText input = new EditText(this);

    input.setHint(hint);
    input.setTextSize(16);
    input.setPadding(0, 12, 0, 12);

    return input;
}

private void tampilkanKalender() {

    DatePickerDialog dialog =
            new DatePickerDialog(
                    this,
                    (view, year, month, dayOfMonth) -> {

                        kalender.set(
                                year,
                                month,
                                dayOfMonth
                        );

                        tanggalInput.setText(
                                formatTanggal(kalender)
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

private String formatTanggal(Calendar tanggal) {

    SimpleDateFormat format =
            new SimpleDateFormat(
                    "dd/MM/yyyy",
                    Locale.getDefault()
            );

    return format.format(
            tanggal.getTime()
    );
}

private void tampilkanPerhitungan() {

    String kmText =
            kmInput.getText()
                    .toString()
                    .trim();

    if (kmText.isEmpty()) {

        Toast.makeText(
                this,
                "KM kosong, jadi perhitungan KM tidak ditampilkan.",
                Toast.LENGTH_LONG
        ).show();

        return;
    }

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

        Toast.makeText(
                this,
                "Maksimal: " +
                        maksimal +
                        " KM\nPaling lambat: " +
                        palingLambat +
                        " KM",
                Toast.LENGTH_LONG
        ).show();

    } catch (NumberFormatException e) {

        Toast.makeText(
                this,
                "KM tidak valid.",
                Toast.LENGTH_SHORT
        ).show();
    }
}

private long hitungKelipatanBerikutnya(
        long km,
        long kelipatan
) {

    return ((km / kelipatan) + 1)
            * kelipatan;
}

private void simpanDataSementara() {

    String wa =
            waInput.getText()
                    .toString()
                    .trim();

    if (wa.isEmpty()) {

        waInput.requestFocus();

        Toast.makeText(
                this,
                "Nomor WhatsApp wajib diisi.",
                Toast.LENGTH_SHORT
        ).show();

        return;
    }

    /*
     * Untuk tahap awal tombol langsung dikunci
     * setelah proses simpan dijalankan.
     *
     * Penyimpanan Firebase akan kita pasang
     * pada tahap berikutnya.
     */

    simpanButton.setEnabled(false);
    simpanButton.setText("✓ SUDAH TERSIMPAN");

    Toast.makeText(
            this,
            "Data berhasil disiapkan untuk disimpan.",
            Toast.LENGTH_SHORT
    ).show();
}

private void mintaIzinKontak() {

    if (android.os.Build.VERSION.SDK_INT >= 23) {

        if (checkSelfPermission(
                Manifest.permission.READ_CONTACTS
        ) != PackageManager.PERMISSION_GRANTED) {

            izinKontak.launch(
                    Manifest.permission.READ_CONTACTS
            );

            return;
        }
    }

    bukaPemilihKontak();
}

private void bukaPemilihKontak() {

    Intent intent =
            new Intent(
                    Intent.ACTION_PICK,
                    ContactsContract.CommonDataKinds.Phone.CONTENT_URI
            );

    pilihKontak.launch(intent);
}

private void ambilNomorDariKontak(Uri contactUri) {

    Cursor cursor = null;

    try {

        cursor = getContentResolver().query(
                contactUri,
                new String[]{
                        ContactsContract.CommonDataKinds.Phone.NUMBER
                },
                null,
                null,
                null
        );

        if (cursor != null
                && cursor.moveToFirst()) {

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

    } finally {

        if (cursor != null) {
            cursor.close();
        }
    }
}

}
