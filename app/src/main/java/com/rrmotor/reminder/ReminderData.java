package com.rrmotor.reminder;

public class ReminderData {

    private String id;
    private String nama;
    private String nopol;
    private String nomorMesin;
    private String kmTerakhir;
    private String whatsapp;
    private String tanggalInput;
    private String jatuhTempo;
    private String pesanWhatsApp;

    private long waktuReminder;
    private boolean reminderTerkirim;
    private long waktuTerkirim;
    private long waktuSimpan;

    public ReminderData() {
        // Wajib untuk Gson
    }

    public ReminderData(
            String id,
            String nama,
            String nopol,
            String nomorMesin,
            String kmTerakhir,
            String whatsapp,
            String tanggalInput,
            String jatuhTempo,
            String pesanWhatsApp,
            long waktuReminder,
            boolean reminderTerkirim,
            long waktuTerkirim,
            long waktuSimpan
    ) {
        this.id = id;
        this.nama = nama;
        this.nopol = nopol;
        this.nomorMesin = nomorMesin;
        this.kmTerakhir = kmTerakhir;
        this.whatsapp = whatsapp;
        this.tanggalInput = tanggalInput;
        this.jatuhTempo = jatuhTempo;
        this.pesanWhatsApp = pesanWhatsApp;
        this.waktuReminder = waktuReminder;
        this.reminderTerkirim = reminderTerkirim;
        this.waktuTerkirim = waktuTerkirim;
        this.waktuSimpan = waktuSimpan;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getNama() {
        return nama;
    }

    public void setNama(String nama) {
        this.nama = nama;
    }

    public String getNopol() {
        return nopol;
    }

    public void setNopol(String nopol) {
        this.nopol = nopol;
    }

    public String getNomorMesin() {
        return nomorMesin;
    }

    public void setNomorMesin(String nomorMesin) {
        this.nomorMesin = nomorMesin;
    }

    public String getKmTerakhir() {
        return kmTerakhir;
    }

    public void setKmTerakhir(String kmTerakhir) {
        this.kmTerakhir = kmTerakhir;
    }

    public String getWhatsapp() {
        return whatsapp;
    }

    public void setWhatsapp(String whatsapp) {
        this.whatsapp = whatsapp;
    }

    public String getTanggalInput() {
        return tanggalInput;
    }

    public void setTanggalInput(String tanggalInput) {
        this.tanggalInput = tanggalInput;
    }

    public String getJatuhTempo() {
        return jatuhTempo;
    }

    public void setJatuhTempo(String jatuhTempo) {
        this.jatuhTempo = jatuhTempo;
    }

    public String getPesanWhatsApp() {
        return pesanWhatsApp;
    }

    public void setPesanWhatsApp(String pesanWhatsApp) {
        this.pesanWhatsApp = pesanWhatsApp;
    }

    public long getWaktuReminder() {
        return waktuReminder;
    }

    public void setWaktuReminder(long waktuReminder) {
        this.waktuReminder = waktuReminder;
    }

    public boolean isReminderTerkirim() {
        return reminderTerkirim;
    }

    public void setReminderTerkirim(boolean reminderTerkirim) {
        this.reminderTerkirim = reminderTerkirim;
    }

    public long getWaktuTerkirim() {
        return waktuTerkirim;
    }

    public void setWaktuTerkirim(long waktuTerkirim) {
        this.waktuTerkirim = waktuTerkirim;
    }

    public long getWaktuSimpan() {
        return waktuSimpan;
    }

    public void setWaktuSimpan(long waktuSimpan) {
        this.waktuSimpan = waktuSimpan;
    }
}
