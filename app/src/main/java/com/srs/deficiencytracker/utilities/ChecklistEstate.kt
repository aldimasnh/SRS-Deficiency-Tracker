package com.srs.deficiencytracker.utilities

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.LinearLayout
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.android.volley.Response
import com.android.volley.toolbox.StringRequest
import com.android.volley.toolbox.Volley
import com.google.android.material.checkbox.MaterialCheckBox
import com.srs.deficiencytracker.MainActivity
import com.srs.deficiencytracker.R
import es.dmoral.toasty.Toasty
import kotlinx.android.synthetic.main.activity_checklist_est.btSaveCheckEst
import kotlinx.android.synthetic.main.activity_checklist_est.checkboxContainerEst
import kotlinx.android.synthetic.main.activity_checklist_est.loadingCheckEst
import kotlinx.android.synthetic.main.activity_checklist_est.lottieCbEst
import kotlinx.android.synthetic.main.activity_checklist_est.progressBarCheckEst
import kotlinx.android.synthetic.main.activity_main.loadingMain
import org.json.JSONException
import org.json.JSONObject

class ChecklistEstate : AppCompatActivity() {

    private var getSync = ""

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        UpdateMan().hideStatusNavigationBar(window)
        setContentView(R.layout.activity_checklist_est)

        getSync = try {
            intent.getStringExtra("sync")!!
        } catch (e: Exception) {
            ""
        }

        val urlCategory = PrefManager(this).dataReg!!

        lottieCbEst.setAnimation("wait.json")
        lottieCbEst.loop(true)
        lottieCbEst.playAnimation()

        val prefManager = PrefManager(this)
        val postRequest: StringRequest = object : StringRequest(
            Method.POST, "https://srs-ssms.com/deficiency_tracker/getListDataEstate.php",
            Response.Listener { response ->
                try {
                    progressBarCheckEst.visibility = View.GONE

                    val jObj = JSONObject(response)
                    val success = jObj.getInt("status")
                    if (success == 1) {
                        val dataListEstArray = jObj.getJSONArray("listEst")

                        val estArray = ArrayList<String>()
                        val mtCheckBox = ArrayList<MaterialCheckBox>()
                        for (i in 0 until dataListEstArray.length()) {
                            val option = dataListEstArray.getString(i)
                            estArray.add(option)
                        }
                        estArray.sort()

                        val marginBetweenCheckboxes =
                            resources.getDimensionPixelSize(R.dimen.checkbox_margin)
                        val estYellowValues = prefManager.estYellow?.split(",")?.map { it.trim() } ?: emptyList()
                        for (text in estArray) {
                            val checkBox = MaterialCheckBox(this)
                            checkBox.text = text

                            if (estYellowValues.contains(checkBox.text.toString())) {
                                checkBox.isChecked = true
                            }

                            val layoutParams = LinearLayout.LayoutParams(
                                LinearLayout.LayoutParams.WRAP_CONTENT,
                                LinearLayout.LayoutParams.WRAP_CONTENT
                            )

                            if (checkboxContainerEst.childCount > 0) {
                                layoutParams.topMargin = marginBetweenCheckboxes
                            }

                            checkBox.layoutParams = layoutParams
                            mtCheckBox.add(checkBox)
                            checkboxContainerEst.addView(checkBox)
                        }

                        btSaveCheckEst.setOnClickListener {
                            val selectedCheckboxEst = ArrayList<String>()

                            for (checkbox in mtCheckBox) {
                                if (checkbox.isChecked) {
                                    selectedCheckboxEst.add(checkbox.text.toString())
                                }
                            }

                            if (selectedCheckboxEst.isNotEmpty()) {
                                val fixSelected = selectedCheckboxEst.toTypedArray().contentToString()
                                    .replace("[", "").replace("]", "").replace(" ", "")
                                prefManager.estYellow = fixSelected

                                val intent = Intent(this, MainActivity::class.java)
                                if (getSync == "yes") {
                                    UpdateMan().checkUpdateYellow(intent, this, loadingCheckEst)
                                } else {
                                    UpdateMan().loadFile(
                                        intent,
                                        urlCategory,
                                        fixSelected,
                                        this@ChecklistEstate,
                                        loadingCheckEst,
                                        "download"
                                    )
                                }
                            } else {
                                Toasty.warning(this, "Ceklist data estate terlebih dahulu!!").show()
                            }
                        }
                    }
                } catch (e: JSONException) {
                    AlertDialogUtility.withSingleAction(
                        this, "Ulang", "Data error, hubungi pengembang: $e", "warning.json"
                    ) {
                        val intent = Intent(this, ChecklistEstate::class.java)
                        startActivity(intent)
                    }

                    progressBarCheckEst.visibility = View.GONE
                    e.printStackTrace()
                }
            },
            Response.ErrorListener {
                AlertDialogUtility.withSingleAction(
                    this, "Ulang", "Terjadi kesalahan koneksi", "warning.json"
                ) {
                    val intent = Intent(this, ChecklistEstate::class.java)
                    startActivity(intent)
                }

                progressBarCheckEst.visibility = View.GONE
            }
        ) {
            override fun getParams(): Map<String, String> {
                val params: MutableMap<String, String> = HashMap()
                params["reg"] = prefManager.idReg.toString()
                return params
            }
        }
        val queue = Volley.newRequestQueue(this)
        queue.cache.clear()
        queue.add(postRequest)
    }

    override fun onBackPressed() {
        if (PrefManager(this).estYellow == null) {
            Toasty.warning(this, "Simpan data terlebih dahulu!!").show()
        } else {
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
}