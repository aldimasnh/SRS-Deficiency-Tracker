package com.srs.deficiencytracker.database

import android.annotation.SuppressLint
import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper
import android.content.ContentValues
import android.database.Cursor
import android.database.sqlite.SQLiteException
import androidx.room.PrimaryKey
import kotlin.collections.ArrayList

//creating the database logic, extending the SQLiteOpenHelper base class
@Suppress("ConvertToStringTemplate")
class PemupukanSQL(context: Context) : SQLiteOpenHelper(
    context,
    db_pupuk, null,
    DATABASE_VERSION
) {
    companion object {
        const val DATABASE_VERSION = 1
        const val db_pupuk = "db_pupuk"

        const val db_tab_meka_pupuk = "mekanisasi_pemupukan"
        const val db_lat = "lat"
        const val db_lon = "lon"
        const val db_datetime = "datetime"
        const val db_menu = "menu"
        const val db_app_ver = "app_version"
        const val db_archive = "archive"

        const val db_p = "P"
        const val db_pp = "PP"
        const val db_mp = "MP"
        const val db_pk = "PK"

        const val db_tabPupuk = "pupuk"
        const val db_id = "id"
        const val db_namaPupuk = "nama"

        const val db_tabPemupukan = "pemupukan"
        const val db_estate = "estate"
        const val db_afdeling = "afdeling"
        const val db_blok = "blok"
        const val db_jenisPupukID = "jenis_pupuk_id"
        const val db_dosisPupuk = "dosis_pupuk"
        const val db_baris1 = "baris1"
        const val db_baris2 = "baris2"
        const val db_dipupukArr = "dipupuk"
        const val db_name = "nama"
        const val db_waktuMulai = "waktu_mulai"
        const val db_waktuSelesai = "waktu_selesai"
        const val db_userId = "user_id"
        const val db_arah = "arah"
        const val db_latAwal = "lat_awal"
        const val db_lonAwal = "lon_awal"
        const val db_latAkhir = "lat_akhir"
        const val db_lonAkhir = "lon_akhir"
        const val db_jenisPupukArr = "jenis_pupuk"
        const val db_lokasiPupukArr = "lokasi_pupuk"
        const val db_sebarPupukArr = "sebar_pupuk"
        const val db_photo = "foto"
        const val db_komen = "komentar"
        const val db_temuan = "temuan"
        const val db_jumlah_pokok = "jumlah_pokok"

        // Preparasi Pemupukan
        const val db_tabPreparasi = "preparasi"
        const val db_kjp = "kjp"
        const val db_apd = "apd"
        const val db_kwp = "kwp"
        const val db_karyawanArr = "karyawan"
        const val db_taraArr = "tara"
        const val db_brutoArr = "bruto"
        const val db_nettoArr = "netto"
        const val db_takaranArr = "kali"

        // Pokok Kuning
        const val db_tabPkKuning = "pokok_kuning"
        const val db_pokok = "pokok"
        const val db_baris = "baris"
        const val db_status_pemupukan = "status_pemupukan"
    }

    @PrimaryKey(autoGenerate = true)
    override fun onCreate(db: SQLiteDatabase?) {
        val createTablePupuk =
            ("CREATE TABLE $db_tabPupuk ($db_id INTEGER PRIMARY KEY, $db_namaPupuk VARCHAR)")

        db?.execSQL(createTablePupuk)
    }

    override fun onUpgrade(db: SQLiteDatabase?, oldVersion: Int, newVersion: Int) {
        db!!.execSQL("DROP TABLE IF EXISTS $db_tabPupuk")
        onCreate(db)
    }

    fun deleteDb() {
        val db = this.writableDatabase
        db.delete(db_tabPupuk, null, null)
        db.close()
    }

    //method to insert data
    fun addPupuk(addPupuk: PupukList): Long {
        val db = this.writableDatabase
        val contentValues = ContentValues()
        contentValues.put(db_id, addPupuk.db_id)
        contentValues.put(db_namaPupuk, addPupuk.db_pupuk)
        val success = db.insert(db_tabPupuk, null, contentValues)
        db.close()
        return success
    }
}