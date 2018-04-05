package com.kkt.utils

import kotlin.experimental.and

/**
 * Created by owen on 18-4-4.
 */
object EasyValue {
    fun readInt(data: ByteArray, offset: Int): Int {
        return (((data[offset].toInt() and 0xFF) shl 24)
                or ((data[offset + 1].toInt() and 0xFF) shl 16)
                or ((data[offset + 2].toInt() and 0xFF) shl 8)
                or ((data[offset + 3].toInt() and 0xFF)))
    }

    fun readShort(data: ByteArray, offset: Int): Short {
        val r = (((data[offset].toInt() and 0xFF) shl 8)
                or (data[offset + 1].toInt() and 0xFF))
        return r.toShort()
    }

    fun writeInt(data: ByteArray, offset: Int, value: Int) {
        data[offset] = (value shr 24).toByte()
        data[offset + 1] = (value shr 16).toByte()
        data[offset + 2] = (value shr 8).toByte()
        data[offset + 3] = value.toByte()
    }

    fun writeShort(data: ByteArray, offset: Int, value: Short) {
        data[offset] = (value.toInt() shr 8).toByte()
        data[offset + 1] = value.toByte()
    }

    fun htons(u: Short): Short {
        val r = (((u and 0xFFFF.toShort()).toInt() shl 8)
                or ((u and 0xFFFF.toShort()).toInt() shr 8))
        return r.toShort()
    }

    fun ntohs(u: Short): Short {
        val r = (((u and 0xFFFF.toShort()).toInt() shl 8)
                or ((u and 0xFFFF.toShort()).toInt() shr 8))
        return r.toShort()
    }

    fun hton(u: Int): Int {
        var r = u shr 24 and 0x000000FF
        r = r or (u shr 8 and 0x0000FF00)
        r = r or (u shl 8 and 0x00FF0000)
        r = r or (u shl 24 and -0x1000000)
        return r
    }

    fun ntoh(u: Int): Int {
        var r = u shr 24 and 0x000000FF
        r = r or (u shr 8 and 0x0000FF00)
        r = r or (u shl 8 and 0x00FF0000)
        r = r or (u shl 24 and -0x1000000)
        return r
    }
}