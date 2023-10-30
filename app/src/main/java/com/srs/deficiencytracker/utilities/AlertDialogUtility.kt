@file:Suppress("DEPRECATION")

package com.srs.deficiencytracker.utilities

import android.annotation.SuppressLint
import android.app.Activity
import android.content.Context
import android.graphics.Typeface
import android.view.LayoutInflater
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import com.srs.deficiencytracker.R
import es.dmoral.toasty.Toasty
import kotlinx.android.synthetic.main.dialog_layout_success.view.*

class AlertDialogUtility {

    companion object {
        @SuppressLint("InflateParams")
        fun alertDialog(context: Context, alertText: String, animAsset: String) {
            if (context is Activity && !context.isFinishing) {
                val layoutBuilder =
                    LayoutInflater.from(context).inflate(R.layout.dialog_layout_success, null)
                val builder: AlertDialog.Builder =
                    AlertDialog.Builder(context).setView(layoutBuilder)
                val alertDialog: AlertDialog = builder.show()
                alertDialog.window?.setBackgroundDrawableResource(R.drawable.background_white)
                layoutBuilder.tv_alert.text = alertText
                layoutBuilder.lottie_anim.setAnimation(animAsset)
                layoutBuilder.lottie_anim.loop(true)
                layoutBuilder.lottie_anim.playAnimation()
                layoutBuilder.btn_dismiss.setOnClickListener {
                    alertDialog.dismiss()
                }
            }
        }

        @SuppressLint("InflateParams")
        fun withTwoActions(context: Context, dismiss: String, action: String, alertText: String, animAsset: String, function: () -> Unit) {
            if (context is Activity && !context.isFinishing) {
                val layoutBuilder =
                    LayoutInflater.from(context).inflate(R.layout.dialog_layout_success, null)
                val builder: AlertDialog.Builder =
                    AlertDialog.Builder(context).setView(layoutBuilder)
                val alertDialog: AlertDialog = builder.show()
                alertDialog.window?.setBackgroundDrawableResource(R.drawable.background_white)
                layoutBuilder.tv_alert.text = alertText
                layoutBuilder.lottie_anim.setAnimation(animAsset)
                layoutBuilder.lottie_anim.loop(true)
                layoutBuilder.lottie_anim.playAnimation()
                layoutBuilder.btn_action.visibility = View.VISIBLE
                layoutBuilder.space.visibility = View.VISIBLE
                layoutBuilder.btn_dismiss.text = dismiss
                layoutBuilder.btn_action.text = action
                layoutBuilder.btn_dismiss.setOnClickListener {
                    alertDialog.dismiss()
                }
                layoutBuilder.btn_action.setOnClickListener {
                    function()
                    alertDialog.dismiss()
                }
            }
        }

        @SuppressLint("InflateParams")
        fun withSingleAction(context: Context, dismiss: String, alertText: String, animAsset: String, function: () -> Unit) {
            if (context is Activity && !context.isFinishing) {
                val layoutBuilder =
                    LayoutInflater.from(context).inflate(R.layout.dialog_layout_success, null)
                val builder: AlertDialog.Builder =
                    AlertDialog.Builder(context).setView(layoutBuilder)
                val alertDialog: AlertDialog = builder.show()
                alertDialog.window?.setBackgroundDrawableResource(R.drawable.background_white)
                layoutBuilder.tv_alert.text = alertText
                layoutBuilder.btn_dismiss.text = dismiss
                layoutBuilder.lottie_anim.setAnimation(animAsset)
                layoutBuilder.lottie_anim.loop(true)
                layoutBuilder.lottie_anim.playAnimation()
                layoutBuilder.btn_dismiss.setOnClickListener {
                    alertDialog.dismiss()
                    function()
                }
            }
        }

        @SuppressLint("InflateParams")
        fun withCheckBox(context: Context, dismiss: String, action: String, alertText: String, listData: String, attention: String, checkBoxText: String, dismissFunction: (() -> Unit)?, function: (() -> Unit)?, missNotif: Boolean? = false) {
            if (context is Activity && !context.isFinishing) {
                val layoutBuilder =
                    LayoutInflater.from(context).inflate(R.layout.dialog_layout_success, null)
                val builder: AlertDialog.Builder =
                    AlertDialog.Builder(context).setView(layoutBuilder)
                val alertDialog: AlertDialog = builder.show()
                alertDialog.window?.setBackgroundDrawableResource(R.drawable.background_white)
                layoutBuilder.tv_alert.setTypeface(null, Typeface.BOLD)
                layoutBuilder.tv_alert.text = alertText
                layoutBuilder.lottie_anim.visibility = View.GONE
                layoutBuilder.btn_action.visibility = if (missNotif!!) View.GONE else View.VISIBLE
                layoutBuilder.space.visibility = View.VISIBLE
                layoutBuilder.sv_message.visibility = View.VISIBLE
                layoutBuilder.tv_message.visibility = View.VISIBLE
                layoutBuilder.tv_message.text = listData
                layoutBuilder.tv_attention.visibility = View.VISIBLE
                layoutBuilder.tv_attention.text = attention
                layoutBuilder.cb_confirmation.visibility = if (missNotif!!) View.GONE else View.VISIBLE
                layoutBuilder.cb_confirmation.text = checkBoxText
                layoutBuilder.btn_dismiss.text = dismiss
                layoutBuilder.btn_action.text = action
                layoutBuilder.btn_dismiss.setOnClickListener {
                    if (dismissFunction != null) {
                        dismissFunction()
                    }
                    alertDialog.dismiss()
                }
                layoutBuilder.btn_action.setOnClickListener {
                    if (layoutBuilder.cb_confirmation.isChecked) {
                        if (function != null) {
                            function()
                        }
                        alertDialog.dismiss()
                    } else {
                        Toasty.warning(context, "Harap centang kotak konfirmasi terlebih dahulu!!").show()
                    }
                }
            }
        }

    }
}
