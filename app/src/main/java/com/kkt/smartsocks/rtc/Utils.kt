package com.kkt.smartsocks.rtc

import android.content.Context
import android.util.Log
import java.nio.ByteBuffer
import java.nio.CharBuffer
import java.nio.charset.Charset
import java.nio.charset.CharsetDecoder
import android.net.wifi.WifiManager


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

        fun dump(buf: ByteBuffer) {
            var dumpStr: String = ""

            Log.d(TAG, "Dump hex:")
            for (i in 0 until buf.limit()) {
                dumpStr += ("" + Integer.toHexString(buf.get(i).toInt()) + " ")
            }
            Log.d(TAG, dumpStr)

            dumpStr = ""
            Log.d(TAG, "Dump char:")
            for (i in 0 until buf.limit()) {
                dumpStr += ("" + buf.get(i).toInt().toChar() + " ")
            }
            Log.d(TAG, dumpStr)
        }

        fun getWIFILocalIpAdress(context: Context): String {

            //获取wifi服务
            val wifiManager = context.getSystemService(Context.WIFI_SERVICE) as WifiManager
            //判断wifi是否开启
            if (!wifiManager.isWifiEnabled) {
                return ""
            }
            val wifiInfo = wifiManager.connectionInfo
            val ipAddress = wifiInfo.ipAddress
            return (ipAddress and 0xFF).toString() + "." +
                    (ipAddress shr 8 and 0xFF) + "." +
                    (ipAddress shr 16 and 0xFF) + "." +
                    (ipAddress shr 24 and 0xFF)
        }
    }
}