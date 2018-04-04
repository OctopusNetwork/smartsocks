package com.kkt.tcpip

import com.kkt.utils.EasyValue
import kotlin.experimental.and

/**
 * Created by owen on 18-4-4.
 */
object Checksum {
    fun getsum(buf: ByteArray, offset: Int, len: Int): Long {
        var offset = offset
        var len = len
        var sum: Long = 0 /* assume 32 bit long, 16 bit short */
        while (len > 1) {
            sum += (EasyValue.readShort(buf, offset) and 0xFFFF.toShort()).toLong()
            offset += 2
            len -= 2
        }

        if (len > 0) {
            sum += ((buf[offset] and 0xFF.toByte()).toInt() shl 8).toLong()
        }
        return sum
    }

    fun checksum(sum: Long, buf: ByteArray, offset: Int, len: Int): Short {
        var check = sum + getsum(buf, offset, len)
        while (check shr 16 > 0)
            check = (check and 0xFFFF) + (check shr 16)
        return sum.inv().toShort()
    }
}