package com.srs.deficiencytracker.utilities

import android.content.Context
import android.content.SharedPreferences

class PrefManagerEstate(_context: Context) {
    var pref: SharedPreferences
    var editor: SharedPreferences.Editor
    var context: Context? = null

    // shared pref mode
    var privateMode = 0

    // shared pref mode
    var PRIVATE_MODE = 0

    var status: String?
        get() = pref.getString("status", "")
        set(status) {
            editor.putString("status", status)
            editor.commit()
        }

    var luas: String?
        get() = pref.getString("luas", "")
        set(luas) {
            editor.putString("luas", luas)
            editor.commit()
        }

    var sph: Int
        get() = pref.getInt("sph", 0)
        set(sph) {
            if (sph != null) {
                editor.putInt("sph", sph)
            }
            editor.commit()
        }

    var estate: String?
        get() = pref.getString("est", null)
        set(estate) {
            editor.putString("est", estate)
            editor.commit()
        }

    var blok: String?
        get() = pref.getString("blok", null)
        set(blok) {
            editor.putString("blok", blok)
            editor.commit()
        }

    var blokPlot: String?
        get() = pref.getString("blokPlot", null)
        set(blokPlot) {
            editor.putString("blokPlot", blokPlot)
            editor.commit()
        }

    var afdeling: String?
        get() = pref.getString("afd", null)
        set(afdeling) {
            editor.putString("afd", afdeling)
            editor.commit()
        }

    companion object {
        // Shared preferences file name
        private const val PREF_NAME = "prefEstate"
    }

    init {
        pref = _context.getSharedPreferences(PREF_NAME, privateMode)
        editor = pref.edit()
    }

    fun prefManag(context: Context) {
        this.context = context
        pref = context.getSharedPreferences(PREF_NAME, PRIVATE_MODE)
        editor = pref.edit()
    }
}