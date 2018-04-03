package com.kkt.rtc

import android.util.Log
import java.nio.ByteBuffer
import java.nio.CharBuffer
import java.nio.charset.Charset
import java.nio.charset.CharsetDecoder

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

        fun debug(tag: String, buf: ByteBuffer) {
            var charset: Charset?
            var decoder: CharsetDecoder?
            var charBuffer: CharBuffer?

            try {
                charset = Charset.forName("UTF-8")
                decoder = charset.newDecoder()
                charBuffer = decoder.decode(buf.asReadOnlyBuffer())
                Log.d(tag, charBuffer.toString())

            } catch (ex: Exception) {
                ex.printStackTrace()
            }
        }
    }
}