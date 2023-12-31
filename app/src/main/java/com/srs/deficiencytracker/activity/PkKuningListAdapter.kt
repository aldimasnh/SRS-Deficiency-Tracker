package com.srs.deficiencytracker.activity

import android.annotation.SuppressLint
import android.app.Activity
import android.content.Intent
import android.util.Log
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import com.srs.deficiencytracker.R
import com.srs.deficiencytracker.database.PemupukanSQL
import com.srs.deficiencytracker.database.PemupukanSQL.Companion.db_tabPkKuning
import com.srs.deficiencytracker.utilities.AlertDialogUtility
import com.srs.deficiencytracker.utilities.PrefManager
import com.srs.deficiencytracker.utilities.PrefManagerEstate
import org.json.JSONObject
import java.io.File
import java.util.*

class PkKuningListAdapter(
    private val context: Activity,
    private val id: ArrayList<Int>,
    private val idPk: ArrayList<Int>,
    private val est: ArrayList<String>,
    private val afd: ArrayList<String>,
    private val blok: ArrayList<String>,
    private val status: ArrayList<String>,
    private val kondisi: ArrayList<String>,
    private val datetime: ArrayList<String>,
    private val archive: Int? = 0
) : ArrayAdapter<String>(
    context,
    R.layout.list_view_data, est
) {

    @SuppressLint("SetTextI18n", "ViewHolder", "InflateParams", "MissingInflatedId")
    override fun getView(pos: Int, view: View?, parent: ViewGroup): View {
        val inflater = context.layoutInflater
        val rowView = inflater.inflate(R.layout.list_view_data, null, true)

        val tvTanggal = rowView.findViewById(R.id.tv_tgl_list) as TextView
        val tvIdPk = rowView.findViewById(R.id.tv_list_idPk) as TextView
        val tvLokasi = rowView.findViewById(R.id.tv_lokasi_list) as TextView
        val tvKondisi = rowView.findViewById(R.id.tv_kondisi_list) as TextView
        val tvStatus = rowView.findViewById(R.id.tv_status_list) as TextView
        val ivDel = rowView.findViewById(R.id.delListPk) as ImageView
        val llDel = rowView.findViewById(R.id.llDelList) as LinearLayout

        val ll_list = rowView.findViewById(R.id.linearLyListData) as LinearLayout

        llDel.visibility = if (archive == 1) View.GONE else View.VISIBLE

        ivDel.setOnClickListener {
            AlertDialogUtility.withTwoActions(
                context,
                "Batalkan",
                "Hapus",
                "Apakah anda yakin untuk menghapus data yang dipilih?",
                "warning.json"
            ) {
                val pkPath =
                    context.getExternalFilesDir(null)?.absolutePath + "/MAIN/pk" + PrefManager(
                        context
                    ).dataReg!!
                val fileMaps = File(pkPath)
                if (fileMaps.exists()) {
                    try {
                        val pm = PrefManagerEstate(context)
                        val pmPrevCons = try {
                            pm.prevCons!!
                        } catch (e: Exception) {
                            ""
                        }

                        val readMaps = fileMaps.readText()
                        val objMaps = JSONObject(readMaps)

                        val estObjMaps = objMaps.getJSONObject(est[pos])
                        val afdObjMaps =
                            estObjMaps.getJSONObject(afd[pos])
                        val blokObjMaps =
                            afdObjMaps.getJSONObject(blok[pos])
                        for (index in blokObjMaps.keys()) {
                            val item =
                                blokObjMaps.getJSONObject(index)
                            if (idPk[pos].toString() == index) {
                                if (status[pos] == "Terverifikasi") {
                                    if (kondisi[pos] == "Sembuh") {
                                        if (pmPrevCons.isNotEmpty()) {
                                            val splitPrevCons =
                                                pm.prevCons!!.replace("[", "").replace("]", "")
                                                    .replace(" ", "").split(",")
                                            for (a in splitPrevCons.indices) {
                                                val valPrev = splitPrevCons[a].split("$")
                                                if (idPk[pos].toString() == valPrev[0]) {
                                                    item.put("status", valPrev[2])
                                                    item.remove("tanggal")
                                                }
                                            }
                                        }
                                    } else {
                                        if (pmPrevCons.isNotEmpty()) {
                                            val splitPrevCons =
                                                pm.prevCons!!.replace("[", "").replace("]", "")
                                                    .replace(" ", "").split(",")
                                            for (a in splitPrevCons.indices) {
                                                val valPrev = splitPrevCons[a].split("$")
                                                if (idPk[pos].toString() == valPrev[0]) {
                                                    item.put("status", valPrev[2])
                                                    if (valPrev[3].isNotEmpty()) {
                                                        item.put("tanggal", valPrev[3].replace("|", " "))
                                                    }
                                                }
                                            }
                                        }
                                    }
                                } else {
                                    if (kondisi[pos] == "Sembuh") {
                                        if (pmPrevCons.isNotEmpty()) {
                                            val splitPrevCons =
                                                pm.prevCons!!.replace("[", "").replace("]", "")
                                                    .replace(" ", "").split(",")
                                            for (a in splitPrevCons.indices) {
                                                val valPrev = splitPrevCons[a].split("$")
                                                if (idPk[pos].toString() == valPrev[0]) {
                                                    item.put("kondisi", valPrev[1])
                                                }
                                            }
                                        }
                                    }

                                    item.put("status", "Belum")
                                    item.remove("tanggal")
                                    item.remove("perlakuan")
                                }
                            }
                        }
                        fileMaps.writeText(objMaps.toString())

                        val db = PemupukanSQL(context).readableDatabase
                        val rowsDeleted = db.delete(
                            db_tabPkKuning,
                            "id" + "=?",
                            arrayOf(id[pos].toString())
                        )
                        db.close()

                        if (rowsDeleted > 0) {
                            val intent = Intent(context, PkKuningListActivity::class.java)
                            context.startActivity(intent)
                            (context).overridePendingTransition(0, 0)
                        } else {
                            AlertDialogUtility.alertDialog(
                                context,
                                "Terjadi kesalahan, hubungi pengembang.",
                                "warning.json"
                            )
                        }
                    } catch (e: Exception) {
                        AlertDialogUtility.alertDialog(
                            context,
                            "Terjadi kesalahan, hubungi pengembang. Error: $e",
                            "warning.json"
                        )
                        e.printStackTrace()
                    }
                } else {
                    AlertDialogUtility.alertDialog(
                        context,
                        "File JSON tidak ditemukan!",
                        "warning.json"
                    )
                }
            }
        }

        tvTanggal.text = datetime[pos]
        tvIdPk.text = idPk[pos].toString()
        tvLokasi.text = "${est[pos]}\n${afd[pos]} - ${blok[pos]}"
        tvKondisi.text = kondisi[pos]
        tvStatus.text = status[pos]

        return rowView
    }
}