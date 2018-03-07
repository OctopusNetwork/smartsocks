package com.kkt.smartsocks

import android.util.Log
import java.nio.ByteBuffer
import java.nio.CharBuffer
import java.nio.charset.Charset
import java.nio.charset.CharsetDecoder

/**
 * Created by owen on 18-3-7.
 */
class Utils {
    companion object {
        val TAG: String = "Utils"

        fun log(buf: ByteBuffer) {
            var charset: Charset? = null
            var decoder: CharsetDecoder? = null
            var charBuffer: CharBuffer? = null

            try {
                charset = Charset.forName("UTF-8")
                decoder = charset.newDecoder()
                charBuffer = decoder.decode(buf.asReadOnlyBuffer())
                Log.d(TAG, charBuffer.toString())
            } catch (ex: Exception) {
                ex.printStackTrace()
            }
        }
    }
}