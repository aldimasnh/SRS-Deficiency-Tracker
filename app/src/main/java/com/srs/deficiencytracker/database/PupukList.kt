package com.srs.deficiencytracker.database

class PupukList(
    val db_id: Int,
    val db_pupuk: String
)

class ViewPkKuning(
    val db_id: Int,
    val db_idPk: Int,
    val db_estate: String,
    val db_afdeling: String,
    val db_blok: String,
    val db_status: String,
    val db_kondisi: String,
    val db_datetime: String,
    val db_jenisPupukID: String,
    val db_dosisPupuk: String,
    val db_metode: String,
    val db_photo: String,
    val db_komen: String,
)