package com.srs.deficiencytracker.utilities

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.android.volley.Response
import com.android.volley.toolbox.StringRequest
import com.android.volley.toolbox.Volley
import com.bumptech.glide.Glide
import com.srs.deficiencytracker.MainActivity
import com.srs.deficiencytracker.R
import es.dmoral.toasty.Toasty
import kotlinx.android.synthetic.main.activity_login.logo_ssms
import kotlinx.android.synthetic.main.activity_login.lottie
import kotlinx.android.synthetic.main.activity_qc_reg.*
import kotlinx.android.synthetic.main.panen_spinner.view.*
import org.json.JSONException
import org.json.JSONObject

const val urlGet = "https://srs-ssms.com/getListDataRegional.php"

class QcReg : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        UpdateMan().hideStatusNavigationBar(window)
        setContentView(R.layout.activity_qc_reg)

        Glide.with(this)//GLIDE LOGO FOR LOADING LAYOUT
            .load(R.drawable.logo_png_white)
            .into(logo_ssms_qc_reg)
        lottieQCReg.setAnimation("loading_circle.json")//ANIMATION WITH LOTTIE FOR LOADING LAYOUT
        lottieQCReg.loop(true)
        lottieQCReg.playAnimation()

        lottieMain.setAnimation("wait.json")
        lottieMain.loop(true)
        lottieMain.playAnimation()

        val postRequest: StringRequest = object : StringRequest(
            Method.GET, urlGet,
            Response.Listener { response ->
                try {
                    progressBarQCReg.visibility = View.GONE

                    val jObj = JSONObject(response)
                    val success = jObj.getInt("status")
                    if (success == 1) {
                        val dataListRegArray = jObj.getJSONObject("listData")
                        val beforeSplitId = dataListRegArray.getJSONArray("id")
                        val beforeSplitData = dataListRegArray.getJSONArray("data")
                        val beforeSplitReg = dataListRegArray.getJSONArray("reg")
                        Log.d("parsing", beforeSplitData.toString())

                        var idArray = ArrayList<Int>()
                        for (i in 0 until beforeSplitId.length()) {
                            idArray.add(beforeSplitId.getInt(i))
                        }

                        var dataArray = ArrayList<String>()
                        for (i in 0 until beforeSplitData.length()) {
                            dataArray.add(beforeSplitData.getString(i))
                        }

                        var regArray = ArrayList<String>()
                        for (i in 0 until beforeSplitReg.length()) {
                            regArray.add(beforeSplitReg.getString(i))
                        }

                        var idReg = 0
                        var reg = ""
                        var dataReg = ""

                        layoutRegQC.tvPanenTBS.text = "Regional"
                        layoutRegQC.spPanenTBS.setItems(regArray)
                        layoutRegQC.spPanenTBS.setOnItemSelectedListener { view, position, id, item ->
                            idReg = idArray[position]
                            reg = item.toString()
                            dataReg = dataArray[position]
                            Log.d("testreg", "reg:$reg || dataReg:$dataReg")
                        }
                        layoutRegQC.spPanenTBS.text = "Pilih Regional"

                        bt_save_qc_reg.setOnClickListener {
                            if (reg == "") {
                                Toasty.warning(this, "Pilihlah regional terlebih dahulu!!").show()
                            } else {
                                PrefManager(this).idReg = idReg
                                PrefManager(this).reg = reg
                                PrefManager(this).dataReg = dataReg

                                progressBarQCReg.visibility = View.VISIBLE
                                val intent = Intent(this, ChecklistEstate::class.java)
                                startActivity(intent)
                            }
                        }

                    } else {
                        AlertDialogUtility.alertDialog(
                            this,
                            jObj.getString(Database.TAG_MESSAGE),
                            "warning.json"
                        )
                    }
                } catch (e: JSONException) {
                    AlertDialogUtility.withSingleAction(
                        this, "Ulang", "Data error, hubungi pengembang: $e", "warning.json"
                    ) {
                        val intent = Intent(this, QcReg::class.java)
                        startActivity(intent)
                    }

                    progressBarQCReg.visibility = View.GONE
                    e.printStackTrace()
                }
            },
            Response.ErrorListener {
                AlertDialogUtility.withSingleAction(
                    this, "Ulang", "Terjadi kesalahan koneksi", "warning.json"
                ) {
                    val intent = Intent(this, QcReg::class.java)
                    startActivity(intent)
                }

                progressBarQCReg.visibility = View.GONE
            }
        ) {

        }
        val queue = Volley.newRequestQueue(this)
        queue.cache.clear()
        queue.add(postRequest)
    }

    override fun onBackPressed() {
        Toasty.warning(this, "Simpan data terlebih dahulu!!").show()
    }
}