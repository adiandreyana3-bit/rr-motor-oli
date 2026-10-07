package com.rrmotor.reminder;

import android.content.Context;
import android.content.SharedPreferences;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

public class LocalDataStore {

    private static final String PREF_NAME =
            "RR_MOTOR_LOCAL_DATA";

    private static final String KEY_REMINDERS =
            "reminders_json";

    private final SharedPreferences preferences;

    private final Gson gson;

    public LocalDataStore(Context context) {

        preferences = context.getApplicationContext()
                .getSharedPreferences(
                        PREF_NAME,
                        Context.MODE_PRIVATE
                );

        gson = new Gson();
    }

    /**
     * Mengambil semua reminder dari HP.
     */
    public synchronized ArrayList<ReminderData> getAll() {

        String json = preferences.getString(
                KEY_REMINDERS,
                ""
        );

        if (json == null || json.trim().isEmpty()) {
            return new ArrayList<>();
        }

        try {

            Type type =
                    new TypeToken<ArrayList<ReminderData>>() {
                    }.getType();

            ArrayList<ReminderData> data =
                    gson.fromJson(json, type);

            if (data == null) {
                return new ArrayList<>();
            }

            return data;

        } catch (Exception e) {

            return new ArrayList<>();
        }
    }

    /**
     * Menyimpan seluruh reminder.
     */
    public synchronized boolean saveAll(
            List<ReminderData> reminders) {

        try {

            String json =
                    gson.toJson(reminders);

            return preferences.edit()
                    .putString(
                            KEY_REMINDERS,
                            json
                    )
                    .commit();

        } catch (Exception e) {

            return false;
        }
    }

    /**
     * Tambah reminder baru.
     */
    public synchronized boolean add(
            ReminderData reminder) {

        if (reminder == null) {
            return false;
        }

        ArrayList<ReminderData> list =
                getAll();

        /*
         * Cegah ID yang sama masuk dua kali.
         */
        for (ReminderData data : list) {

            if (data.getId() != null &&
                    data.getId().equals(reminder.getId())) {

                return false;
            }
        }

        list.add(reminder);

        return saveAll(list);
    }

    /**
     * Cari reminder berdasarkan ID.
     */
    public synchronized ReminderData getById(
            String id) {

        if (id == null) {
            return null;
        }

        ArrayList<ReminderData> list =
                getAll();

        for (ReminderData data : list) {

            if (id.equals(data.getId())) {
                return data;
            }
        }

        return null;
    }

    /**
     * Update reminder.
     */
    public synchronized boolean update(
            ReminderData reminder) {

        if (reminder == null ||
                reminder.getId() == null) {

            return false;
        }

        ArrayList<ReminderData> list =
                getAll();

        for (int i = 0; i < list.size(); i++) {

            ReminderData data =
                    list.get(i);

            if (reminder.getId().equals(data.getId())) {

                list.set(i, reminder);

                return saveAll(list);
            }
        }

        return false;
    }

    /**
     * Hapus satu reminder.
     */
    public synchronized boolean delete(
            String id) {

        if (id == null) {
            return false;
        }

        ArrayList<ReminderData> list =
                getAll();

        boolean ditemukan = false;

        for (int i = list.size() - 1; i >= 0; i--) {

            ReminderData data =
                    list.get(i);

            if (id.equals(data.getId())) {

                list.remove(i);
                ditemukan = true;
            }
        }

        if (!ditemukan) {
            return false;
        }

        return saveAll(list);
    }

    /**
     * Hapus semua reminder yang sudah terkirim.
     */
    public synchronized int deleteAllSent() {

        ArrayList<ReminderData> list =
                getAll();

        int jumlah = 0;

        for (int i = list.size() - 1; i >= 0; i--) {

            ReminderData data =
                    list.get(i);

            if (data.isReminderTerkirim()) {

                list.remove(i);
                jumlah++;
            }
        }

        if (jumlah > 0) {
            saveAll(list);
        }

        return jumlah;
    }

    /**
     * Mengambil reminder yang belum terkirim.
     */
    public synchronized ArrayList<ReminderData>
    getUnsent() {

        ArrayList<ReminderData> hasil =
                new ArrayList<>();

        for (ReminderData data : getAll()) {

            if (!data.isReminderTerkirim()) {
                hasil.add(data);
            }
        }

        return hasil;
    }

    /**
     * Mengambil reminder yang sudah terkirim.
     */
    public synchronized ArrayList<ReminderData>
    getSent() {

        ArrayList<ReminderData> hasil =
                new ArrayList<>();

        for (ReminderData data : getAll()) {

            if (data.isReminderTerkirim()) {
                hasil.add(data);
            }
        }

        return hasil;
    }

    /**
     * Urutkan berdasarkan waktu simpan terbaru.
     */
    public synchronized ArrayList<ReminderData>
    getSortedByNewest() {

        ArrayList<ReminderData> list =
                getAll();

        Collections.sort(
                list,
                (a, b) ->
                        Long.compare(
                                b.getWaktuSimpan(),
                                a.getWaktuSimpan()
                        )
        );

        return list;
    }

    /**
     * Menandai reminder sebagai sudah terkirim.
     */
    public synchronized boolean
    tandaiTerkirim(String id) {

        ReminderData data =
                getById(id);

        if (data == null) {
            return false;
        }

        data.setReminderTerkirim(true);
        data.setWaktuTerkirim(
                System.currentTimeMillis()
        );

        return update(data);
    }

    /**
     * Menghapus semua data lokal.
     *
     * Digunakan hanya saat restore penuh
     * atau logout/reset.
     */
    public synchronized boolean clearAll() {

        return preferences.edit()
                .remove(KEY_REMINDERS)
                .commit();
    }

    /**
     * Mengganti seluruh database lokal
     * dengan data hasil restore Google Drive.
     */
    public synchronized boolean replaceAll(
            List<ReminderData> reminders) {

        if (reminders == null) {
            reminders = new ArrayList<>();
        }

        return saveAll(reminders);
    }

    /**
     * Menghasilkan JSON untuk backup Drive.
     */
    public synchronized String toJson() {

        return gson.toJson(getAll());
    }

    /**
     * Mengambil data dari JSON hasil restore.
     */
    public synchronized ArrayList<ReminderData>
    fromJson(String json) {

        if (json == null ||
                json.trim().isEmpty()) {

            return new ArrayList<>();
        }

        try {

            Type type =
                    new TypeToken<ArrayList<ReminderData>>() {
                    }.getType();

            ArrayList<ReminderData> data =
                    gson.fromJson(json, type);

            if (data == null) {
                return new ArrayList<>();
            }

            return data;

        } catch (Exception e) {

            return new ArrayList<>();
        }
    }

    /**
     * Restore JSON ke penyimpanan lokal.
     */
    public synchronized boolean
    restoreFromJson(String json) {

        ArrayList<ReminderData> data =
                fromJson(json);

        if (data.isEmpty()) {

            /*
             * JSON kosong dianggap database kosong.
             */
            if ("[]".equals(json.trim())) {
                return clearAll();
            }

            return false;
        }

        return replaceAll(data);
    }

    /**
     * Jumlah data.
     */
    public synchronized int size() {

        return getAll().size();
    }
}
