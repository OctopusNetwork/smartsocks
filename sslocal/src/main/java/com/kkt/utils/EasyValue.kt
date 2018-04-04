package com.kkt.utils

import kotlin.experimental.and

/**
 * Created by owen on 18-4-4.
 */
object EasyValue {
    fun readInt(data: ByteArray, offset: Int): Int {
        return (((data[offset] and 0xFF.toByte()).toInt() shl 24)
                or ((data[offset + 1] and 0xFF.toByte()).toInt() shl 16)
                or ((data[offset + 2] and 0xFF.toByte()).toInt() shl 8)
                or ((data[offset + 3] and 0xFF.toByte()).toInt()))
    }

    fun readShort(data: ByteArray, offset: Int): Short {
        val r = (((data[offset] and 0xFF.toByte()).toInt() shl 8)
                or (data[offset + 1] and 0xFF.toByte()).toInt())
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

    // 网络字节顺序与主机字节顺序的转换

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

    // 计算校验和
    fun checksum(sum: Long, buf: ByteArray, offset: Int, len: Int): Short {
        var sum = sum
        sum += getsum(buf, offset, len)
        while (sum shr 16 > 0)
            sum = (sum and 0xFFFF) + (sum shr 16)
        return sum.inv().toShort()
    }

    fun getsum(buf: ByteArray, offset: Int, len: Int): Long {
        var offset = offset
        var len = len
        var sum: Long = 0 /* assume 32 bit long, 16 bit short */
        while (len > 1) {
            sum += (readShort(buf, offset) and 0xFFFF.toShort()).toLong()
            offset += 2
            len -= 2
        }

        if (len > 0)
        /* take care of left over byte */ {
            sum += ((buf[offset] and 0xFF.toByte()).toInt() shl 8).toLong()
        }
        return sum
    }
}