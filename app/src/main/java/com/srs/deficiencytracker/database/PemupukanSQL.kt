package com.srs.deficiencytracker.database

import android.annotation.SuppressLint
import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper
import android.content.ContentValues
import android.database.Cursor
import android.database.sqlite.SQLiteException
import android.util.Log
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

        const val db_tabPupuk = "pupuk"
        const val db_tabPkKuning = "pokok_kuning"

        // DB List Pupuk
        const val db_id = "id"
        const val db_namaPupuk = "nama"
        const val db_satuan = "satuan"

        // DB Pokok Kuning
        const val db_idPk = "idPk"
        const val db_estate = "estate"
        const val db_afdeling = "afdeling"
        const val db_blok = "blok"
        const val db_status = "status"
        const val db_kondisi = "kondisi"
        const val db_datetime = "datetime"
        const val db_jenisPupukID = "jenis_pupuk_id"
        const val db_dosisPupuk = "dosis_pupuk"
        const val db_photo = "foto"
        const val db_komen = "komentar"
        const val db_app_ver = "app_version"
        const val db_archive = "archive"
    }

    @PrimaryKey(autoGenerate = true)
    override fun onCreate(db: SQLiteDatabase?) {
        val createTablePupuk =
            ("CREATE TABLE $db_tabPupuk ($db_id INTEGER PRIMARY KEY, $db_namaPupuk VARCHAR, $db_satuan VARCHAR)")
        val createTablePkKuning =
            ("CREATE TABLE $db_tabPkKuning (" +
                    "$db_id INTEGER PRIMARY KEY, " +
                    "$db_idPk INTEGER, " +
                    "$db_estate VARCHAR, " +
                    "$db_afdeling VARCHAR, " +
                    "$db_blok VARCHAR, " +
                    "$db_status VARCHAR, " +
                    "$db_kondisi VARCHAR, " +
                    "$db_datetime VARCHAR, " +
                    "$db_jenisPupukID VARCHAR, " +
                    "$db_dosisPupuk VARCHAR, " +
                    "$db_photo VARCHAR," +
                    "$db_komen VARCHAR, " +
                    "$db_archive INTEGER, " +
                    "$db_app_ver VARCHAR)")

        db?.execSQL(createTablePupuk)
        db?.execSQL(createTablePkKuning)
    }

    override fun onUpgrade(db: SQLiteDatabase?, oldVersion: Int, newVersion: Int) {
        db!!.execSQL("DROP TABLE IF EXISTS $db_tabPupuk")
        db.execSQL("DROP TABLE IF EXISTS $db_tabPkKuning")
        onCreate(db)
    }

    fun deleteDbPupuk() {
        val db = this.writableDatabase
        db.delete(db_tabPupuk, null, null)
        db.close()
    }

    fun deleteDb() {
        val db = this.writableDatabase
        db.delete(db_tabPupuk, null, null)
        db.delete(db_tabPkKuning, null, null)
        db.close()
    }

    //method to insert data
    fun addPupuk(addPupuk: PupukList): Long {
        val db = this.writableDatabase
        val contentValues = ContentValues()
        contentValues.put(db_id, addPupuk.db_id)
        contentValues.put(db_namaPupuk, addPupuk.db_pupuk)
        contentValues.put(db_satuan, addPupuk.db_satuan)
        val success = db.insert(db_tabPupuk, null, contentValues)
        db.close()
        return success
    }

    fun addPokokKuning(
        idPk: Int,
        estate: String,
        afdeling: String,
        blok: String,
        status: String,
        kondisi: String,
        datetime: String,
        jenisPupukId: String,
        dosis: String,
        foto: String,
        komen: String,
        app_ver: String
    ): Long {
        val db = this.writableDatabase
        val contentValues = ContentValues()
        contentValues.put(db_idPk, idPk)
        contentValues.put(db_estate, estate)
        contentValues.put(db_afdeling, afdeling)
        contentValues.put(db_blok, blok)
        contentValues.put(db_status, status)
        contentValues.put(db_kondisi, kondisi)
        contentValues.put(db_datetime, datetime)
        contentValues.put(db_jenisPupukID, jenisPupukId)
        contentValues.put(db_dosisPupuk, dosis)
        contentValues.put(db_photo, foto)
        contentValues.put(db_komen, komen)
        contentValues.put(db_archive, 0)
        contentValues.put(db_app_ver, app_ver)
        val success = db.insert(db_tabPkKuning, null, contentValues)
        db.close()
        return success
    }

    @SuppressLint("Range")
    fun viewListPkKuning(archive: Int? = 0): ArrayList<ViewPkKuning> {
        val pkKuningList: ArrayList<ViewPkKuning> = ArrayList()
        val selectQuery = "SELECT * FROM $db_tabPkKuning WHERE $db_archive = '$archive' ORDER BY $db_id DESC"
        val db = this.readableDatabase
        val cursor: Cursor?
        try {
            cursor = db.rawQuery(selectQuery, null)
        } catch (e: SQLiteException) {
            db.execSQL(selectQuery)
            return ArrayList()
        }
        if (cursor.moveToFirst()) {
            do {
                pkKuningList.add(
                    ViewPkKuning(
                        cursor.getInt(cursor.getColumnIndex(db_id)),
                        cursor.getInt(cursor.getColumnIndex(db_idPk)),
                        cursor.getString(cursor.getColumnIndex(db_estate)),
                        cursor.getString(cursor.getColumnIndex(db_afdeling)),
                        cursor.getString(cursor.getColumnIndex(db_blok)),
                        cursor.getString(cursor.getColumnIndex(db_status)),
                        cursor.getString(cursor.getColumnIndex(db_kondisi)),
                        cursor.getString(cursor.getColumnIndex(db_datetime))
                    )
                )
            } while (cursor.moveToNext())
        }
        return pkKuningList
    }

    @SuppressLint("Recycle")
    fun setRecordPkKuning(): String {
        val selectQueryPkKng = "SELECT * FROM $db_tabPkKuning WHERE $db_archive = '0'"
        val db = this.readableDatabase
        lateinit var cursor: Cursor
        try {
            cursor = db.rawQuery(selectQueryPkKng, null)
        } catch (e: SQLiteException) {
            Log.e("SQLiteException", "Error executing query: $selectQueryPkKng", e)
            return "Error querying data"
        }
        val pkKng = try {
            cursor.count.toString()
        } catch (e: Exception) {
            0
        }
        val total = try {
            pkKng.toString().toInt()
        } catch (e: Exception) {
            0
        }
        return total.toString()
    }
}