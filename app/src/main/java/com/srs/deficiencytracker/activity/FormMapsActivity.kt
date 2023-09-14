package com.srs.deficiencytracker.activity

import android.annotation.SuppressLint
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import com.srs.deficiencytracker.MainActivity
import com.srs.deficiencytracker.R
import com.srs.deficiencytracker.utilities.AlertDialogUtility
import com.srs.deficiencytracker.utilities.FileMan
import com.srs.deficiencytracker.utilities.PrefManager
import com.srs.deficiencytracker.utilities.PrefManagerEstate
import es.dmoral.toasty.Toasty
import kotlinx.android.synthetic.main.activity_form_est.cvNext
import kotlinx.android.synthetic.main.activity_form_est.header_maps
import kotlinx.android.synthetic.main.activity_form_est.sp_afd_form
import kotlinx.android.synthetic.main.activity_form_est.sp_est_form
import kotlinx.android.synthetic.main.header_form.tv_update_header_gudang
import kotlinx.android.synthetic.main.header_form.tv_ver_app_header_gudang
import kotlinx.android.synthetic.main.header_form.view.icLocationHeader
import org.json.JSONObject

open class FormMapsActivity : AppCompatActivity() {

    private var est = ""
    private var afd = ""

    val pilihEstate = "Pilih Estate"
    val pilihAfdeling = "Pilih Afdeling"

    private var estateArrayList = ArrayList<String>()
    private var estateArray = ArrayList<String>()
    private var afdelingArray = ArrayList<String>()
    private var blokArray = ArrayList<String>()
    private var luasArray = ArrayList<String>()
    private var ttArray = ArrayList<String>()
    private var sphArray = ArrayList<String>()

    @SuppressLint("SetTextI18n")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_form_est)

        header_maps.icLocationHeader.visibility = View.GONE
        sp_est_form.text = pilihEstate
        sp_afd_form.text = pilihAfdeling

        var urlCategory = PrefManager(this).dataReg!!
        val obj = JSONObject(
            FileMan().onlineInputStream(
                urlCategory,
                this,
                tv_update_header_gudang,
                tv_ver_app_header_gudang
            )
        )

        val dataBloKArray = obj.getJSONArray("Blok")
        val userArrayLabel = obj.getJSONArray("DataLabelJson")
        Log.d("testreg", userArrayLabel.toString())
        val beforeSplitTag = userArrayLabel.getJSONObject(0).getJSONArray("tag")
        val beforeSplitEst = userArrayLabel.getJSONObject(0).getJSONArray("est")

        var labelArray = ArrayList<String>()
        for (i in 0 until beforeSplitTag.length()) {
            labelArray.add(beforeSplitTag.getString(i))
        }

        var estArray = ArrayList<String>()
        for (i in 0 until beforeSplitEst.length()) {
            estArray.add(beforeSplitEst.getString(i))
        }

        var indexEst = 0
        var indexAFd = 0
        var indexBlok = 0
        var indexLuas = 0
        var indexTT = 0
        var indexSPH = 0

        for (i in labelArray.indices) {
            if ("estate" == labelArray[i]) {
                indexEst = i
            }
            if ("afd baru" == labelArray[i]) {
                indexAFd = i
            }
            if ("blok" == labelArray[i]) {
                indexBlok = i
            }
            if ("luas" == labelArray[i]) {
                indexLuas = i
            }
            if ("tt" == labelArray[i]) {
                indexTT = i
            }
            if ("sph" == labelArray[i]) {
                indexSPH = i
            }
        }

        for (i in 0 until dataBloKArray.length()) {
            val estString = try {
                estArray[dataBloKArray.getJSONObject(i).getString(indexEst.toString()).toInt()]
            } catch (e: Exception) {
                "null"
            }
            if (!estateArrayList.contains(estString) && !estString.contains("TKD") && !estString.contains(
                    "CWS"
                )
            ) {
                estateArrayList.add(estString)
            }
            estateArray.add(estString)
            afdelingArray.add(
                try {
                    dataBloKArray.getJSONObject(i).getString(indexAFd.toString())
                } catch (e: Exception) {
                    "null"
                }
            )
            blokArray.add(
                try {
                    dataBloKArray.getJSONObject(i).getString(indexBlok.toString())
                } catch (e: Exception) {
                    "null"
                }
            )
            ttArray.add(
                try {
                    dataBloKArray.getJSONObject(i).getString(indexTT.toString())
                } catch (e: Exception) {
                    "null"
                }
            )
            luasArray.add(
                try {
                    dataBloKArray.getJSONObject(i).getString(indexLuas.toString())
                } catch (e: Exception) {
                    "null"
                }
            )
            sphArray.add(
                try {
                    dataBloKArray.getJSONObject(i).getString(indexSPH.toString())
                } catch (e: Exception) {
                    "null"
                }
            )
        }
        sp_est_form.setItems(estateArrayList)
        sp_est_form.text = pilihEstate

        val blokArrayList = ArrayList<String>()
        val afdelingArrayList = ArrayList<String>()
        val luasArrayList = ArrayList<String>()
        val sphArrayList = ArrayList<String>()

        sp_est_form.setOnItemSelectedListener { view, position, id, item ->
            afdelingArrayList.clear()
            blokArrayList.clear()
            luasArrayList.clear()
            sphArrayList.clear()

            est = item.toString()

            for (i in this.estateArray.indices) {
                if (!afdelingArrayList.contains(afdelingArray[i]) && item == estateArray[i]) {
                    afdelingArrayList.add(afdelingArray[i])
                }
            }
            if (afdelingArrayList.isEmpty()) {
                Toasty.info(this, "$item tidak memiliki afdeling inti!").show()
            } else {
                sp_afd_form.setItems(afdelingArrayList)
                sp_afd_form.text = pilihAfdeling
            }
        }

        val pm = PrefManagerEstate(this@FormMapsActivity)
        sp_afd_form.setOnItemSelectedListener { view, position, id, item ->
            if (est.isEmpty()) est = pm.estate.toString()
            afd = item.toString()
            //Toasty.info(this, "view: $view || position: $position || id: $id || item: $item").show()
            blokArrayList.clear()
            luasArrayList.clear()
            sphArrayList.clear()
            for (i in blokArray.indices) {
                val namaBlok = "${blokArray[i]}${ttArray[i].substring(2, ttArray[i].lastIndex + 1)}"
                if (!blokArrayList.contains(namaBlok) && item == afdelingArray[i] && est == estateArray[i]) {
                    blokArrayList.add(namaBlok)
                    luasArrayList.add(luasArray[i])
                    sphArrayList.add(sphArray[i])
                }
            }
        }

        cvNext.setOnClickListener {
            if (est.isEmpty() && afd.isEmpty()) {
                Toasty.warning(this, "Ada field yang belum diisi!", Toasty.LENGTH_LONG).show()
            } else {
                val intent =
                    Intent(this, MapsActivity::class.java).putExtra("est", est).putExtra("afd", afd)
                startActivity(intent)
            }
        }
    }

    override fun onBackPressed() {
        AlertDialogUtility.withTwoActions(
            this,
            "Batal",
            "Ya",
            "Apakah anda yakin untuk keluar?",
            "warning.json"
        ) {
            val intent = Intent(this, MainActivity::class.java)
            startActivity(intent)
            finishAffinity()
        }
    }
}