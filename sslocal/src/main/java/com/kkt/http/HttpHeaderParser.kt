package com.kkt.http

import com.kkt.utils.EasyValue
import java.util.*
import kotlin.experimental.and

/**
 * Created by owen on 18-4-5.
 */
object HttpHeaderParser {
    fun parseHost(buffer: ByteArray, offset: Int, count: Int): String? {
        try {
            when (buffer[offset]) {
                'G'.toByte()//GET
                , 'H'.toByte()//HEAD
                , 'P'.toByte()//POST,PUT
                , 'D'.toByte()//DELETE
                , 'O'.toByte()//OPTIONS
                , 'T'.toByte()//TRACE
                , 'C'.toByte()//CONNECT
                -> return getHttpHost(buffer, offset, count)
                0x16.toByte()//SSL
                -> return getSNI(buffer, offset, count)
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return null
    }

    internal fun getHttpHost(buffer: ByteArray, offset: Int, count: Int): String? {
        val headerString = String(buffer, offset, count)
        val headerLines = headerString.split("\\r\\n".toRegex()).dropLastWhile { it.isEmpty() }.toTypedArray()
        val requestLine = headerLines[0]
        if (requestLine.startsWith("GET") || requestLine.startsWith("POST") ||
                requestLine.startsWith("HEAD") || requestLine.startsWith("OPTIONS")) {
            for (i in 1 until headerLines.size) {
                val nameValueStrings = headerLines[i].split(
                        ":".toRegex()).dropLastWhile { it.isEmpty() }.toTypedArray()
                if (nameValueStrings.size == 2) {
                    val name = nameValueStrings[0].toLowerCase(Locale.ENGLISH).trim { it <= ' ' }
                    val value = nameValueStrings[1].trim { it <= ' ' }
                    if ("host" == name) {
                        return value
                    }
                }
            }
        }
        return null
    }

    internal fun getSNI(buffer: ByteArray, offset: Int, count: Int): String? {
        var offset = offset
        val limit = offset + count
        if (count > 43 && buffer[offset].toInt() == 0x16) {//TLS Client Hello
            offset += 43//skip 43 bytes header

            //read sessionID:
            if (offset + 1 > limit) return null
            val sessionIDLength = buffer[offset++] and 0xFF.toByte()
            offset += sessionIDLength

            //read cipher suites:
            if (offset + 2 > limit) return null
            val cipherSuitesLength = EasyValue.readShort(buffer, offset) and 0xFFFF.toShort()
            offset += 2
            offset += cipherSuitesLength

            //read Compression method:
            if (offset + 1 > limit) return null
            val compressionMethodLength = buffer[offset++] and 0xFF.toByte()
            offset += compressionMethodLength

            if (offset == limit) {
                System.err.println("TLS Client Hello packet doesn't contains SNI info.(offset == limit)")
                return null
            }

            //read Extensions:
            if (offset + 2 > limit) return null
            val extensionsLength = EasyValue.readShort(buffer, offset) and 0xFFFF.toShort()
            offset += 2

            if (offset + extensionsLength > limit) {
                System.err.println("TLS Client Hello packet is incomplete.")
                return null
            }

            while (offset + 4 <= limit) {
                val type0 = buffer[offset++] and 0xFF.toByte()
                val type1 = buffer[offset++] and 0xFF.toByte()
                var length = (EasyValue.readShort(buffer, offset) and 0xFFFF.toShort()).toInt()
                offset += 2

                if (type0 == 0.toByte() && type1 == 0x00.toByte() && length > 5) { //have SNI
                    offset += 5 //skip SNI header.
                    length -= 5 //SNI size;
                    if (offset + length > limit) return null
                    return String(buffer, offset, length)
                } else {
                    offset += length
                }
            }

            return null
        } else {
            return null
        }
    }
}