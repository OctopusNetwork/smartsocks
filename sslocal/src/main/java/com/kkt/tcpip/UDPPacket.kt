package com.kkt.tcpip

import com.kkt.utils.EasyValue
import kotlin.experimental.and

/**
 * Created by owen on 18-4-4.
 */
class UDPPacket(data: ByteArray, offset: Int) {
    internal val offset_src_port: Short = 0 // Source port
    internal val offset_dest_port: Short = 2 // Destination port
    internal val offset_tlen: Short = 4 // Datagram length
    internal val offset_crc: Short = 6 // Checksum

    var mData: ByteArray = data
    var mOffset: Int = offset

    fun getSourcePort(): Short {
        return EasyValue.readShort(mData, mOffset + offset_src_port)
    }

    fun setSourcePort(value: Short) {
        EasyValue.writeShort(mData, mOffset + offset_src_port, value)
    }

    fun getDestinationPort(): Short {
        return EasyValue.readShort(mData, mOffset + offset_dest_port)
    }

    fun setDestinationPort(value: Short) {
        EasyValue.writeShort(mData, mOffset + offset_dest_port, value)
    }

    fun getTotalLength(): Int {
        return (EasyValue.readShort(mData, mOffset + offset_tlen) and 0xFFFF.toShort()).toInt()
    }

    fun setTotalLength(value: Int) {
        EasyValue.writeShort(mData, mOffset + offset_tlen, value.toShort())
    }

    fun getCrc(): Short {
        return EasyValue.readShort(mData, mOffset + offset_crc)
    }

    fun setCrc(value: Short) {
        EasyValue.writeShort(mData, mOffset + offset_crc, value)
    }

    override fun toString(): String {
        return String.format("%d->%d", getSourcePort() and 0xFFFF.toShort(),
                getDestinationPort() and 0xFFFF.toShort())
    }

    fun checksum(sum: Long, size: Int): Boolean {
        val oldCrc = getCrc()
        setCrc(0.toShort())
        val newCrc = Checksum.checksum(sum, mData, mOffset, size)
        setCrc(newCrc)
        return oldCrc == newCrc
    }
}