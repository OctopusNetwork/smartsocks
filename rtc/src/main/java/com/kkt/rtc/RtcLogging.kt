package com.kkt.rtc

import android.util.Log

/**
 * Created by owen on 18-3-23.
 */
class RtcLogging {
    companion object {
        var mLoggingEnable = false

        fun enableLogging() {
            mLoggingEnable = true
        }

        fun disableLogging() {
            mLoggingEnable = false
        }

        fun debug(tag: String, msg: String) {
            if (mLoggingEnable) {
                Log.d(tag, msg)
            }
        }
    }
}