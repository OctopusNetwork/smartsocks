package com.kkt.tcpip

import com.kkt.utils.EasyValue
import kotlin.experimental.and

/**
 * Created by owen on 18-4-4.
 */
class TCPPacket(data: ByteArray, offset: Int) {
    val FIN = 1
    val SYN = 2
    val RST = 4
    val PSH = 8
    val ACK = 16
    val URG = 32

    internal val offset_src_port: Short = 0 // 16位源端口
    internal val offset_dest_port: Short = 2 // 16位目的端口
    internal val offset_seq = 4 // 32位序列号
    internal val offset_ack = 8 // 32位确认号
    internal val offset_lenres: Byte = 12 // 4位首部长度/4位保留字
    internal val offset_flag: Byte = 13 // 6位标志位
    internal val offset_win: Short = 14 // 16位窗口大小
    internal val offset_crc: Short = 16 // 16位校验和
    internal val offset_urp: Short = 18 // 16位紧急数据偏移量

    var mData: ByteArray = data
    var mOffset: Int = offset

    fun getHeaderLength(): Int {
        val lenres: Int = mData[mOffset + offset_lenres].toInt() and 0xFF
        return (lenres shr 4) * 4
    }

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

    fun getFlags(): Byte {
        return mData[mOffset + offset_flag]
    }

    fun getCrc(): Short {
        return EasyValue.readShort(mData, mOffset + offset_crc)
    }

    fun setCrc(value: Short) {
        EasyValue.writeShort(mData, mOffset + offset_crc, value)
    }

    fun getSeqID(): Int {
        return EasyValue.readInt(mData, mOffset + offset_seq)
    }

    fun getAckID(): Int {
        return EasyValue.readInt(mData, mOffset + offset_ack)
    }

    override fun toString(): String {
        return String.format("%s%s%s%s%s%s%d->%d %s:%s",
                if ((getFlags() and SYN.toByte()).toInt() == SYN) "SYN " else "",
                if ((getFlags() and ACK.toByte()).toInt() == ACK) "ACK " else "",
                if ((getFlags() and PSH.toByte()).toInt() == PSH) "PSH " else "",
                if ((getFlags() and RST.toByte()).toInt() == RST) "RST " else "",
                if ((getFlags() and FIN.toByte()).toInt() == FIN) "FIN " else "",
                if ((getFlags() and URG.toByte()).toInt() == URG) "URG " else "",
                getSourcePort().toInt() and 0xFFFF,
                getDestinationPort().toInt() and 0xFFFF,
                getSeqID(),
                getAckID())
    }

    fun checksum(sum: Long, size: Int): Boolean {
        val oldCrc = getCrc()
        setCrc(0.toShort())
        val newCrc = Checksum.checksum(sum, mData, mOffset, size)
        setCrc(newCrc)
        return oldCrc == newCrc
    }
}