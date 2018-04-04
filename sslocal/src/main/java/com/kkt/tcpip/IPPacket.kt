package com.kkt.tcpip

import com.kkt.sslocal.SSLocalLogging
import com.kkt.utils.EasyValue
import java.nio.ByteBuffer
import kotlin.experimental.and

/**
 * Created by owen on 18-4-4.
 */
class IPPacket(data: ByteArray, offset: Int) {
    val TAG = "IPPacket"

    val IP: Short = 0x0800
    val ICMP: Byte = 1
    val TCP: Byte = 6
    val UDP: Byte = 17

    internal val offset_ver_ihl: Byte = 0 // 0: Version (4 bits) + Internet header length (4// bits)
    internal val offset_tos: Byte = 1 // 1: Type of service
    internal val offset_tlen: Short = 2 // 2: Total length
    internal val offset_identification: Short = 4 // :4 Identification
    internal val offset_flags_fo: Short = 6 // 6: Flags (3 bits) + Fragment offset (13 bits)
    internal val offset_ttl: Byte = 8 // 8: Time to live
    val offset_proto: Byte = 9 // 9: Protocol
    internal val offset_crc: Short = 10 // 10: Header checksum
    val offset_src_ip = 12 // 12: Source address
    val offset_dest_ip = 16 // 16: Destination address
    internal val offset_op_pad = 20 // 20: Option + Padding

    var mData: ByteArray = data
    val mOffset: Int = offset

    var mLocalServicePort: Short = 0

    private val mTCPPacket = TCPPacket(mData, 20)
    private val mUDPPacket = UDPPacket(mData, 20)
    private val mDNSBuffer = (ByteBuffer.wrap(mData)
            .position(28) as ByteBuffer).slice()

    enum class IPAccessDirection {
        IP_ACCESS_OUTCOMING,
        IP_ACCESS_INCOMING
    }

    fun setDefault() {
        setHeaderLength(20)
        setTos(0.toByte())
        setTotalLength(0)
        setIdentification(0)
        setFlagsAndOffset(0.toShort())
        setTTL(64.toByte())
    }

    fun getDataLength(): Int {
        return this.getTotalLength() - this.getHeaderLength()
    }

    fun getHeaderLength(): Int {
        return (mData[mOffset + offset_ver_ihl] and 0x0F) * 4
    }

    fun setHeaderLength(value: Int) {
        mData[mOffset + offset_ver_ihl] = (4 shl 4 or value / 4).toByte()
    }

    fun getTos(): Byte {
        return mData[mOffset + offset_tos]
    }

    fun setTos(value: Byte) {
        mData[mOffset + offset_tos] = value
    }

    fun getTotalLength(): Int {
        return (EasyValue.readShort(mData, mOffset + offset_tlen) and 0xFFFF.toShort()).toInt()
    }

    fun setTotalLength(value: Int) {
        EasyValue.writeShort(mData, mOffset + offset_tlen, value.toShort())
    }

    fun getIdentification(): Int {
        return (EasyValue.readShort(mData, mOffset + offset_identification) and 0xFFFF.toShort()).toInt()
    }

    fun setIdentification(value: Int) {
        EasyValue.writeShort(mData, mOffset + offset_identification, value.toShort())
    }

    fun getFlagsAndOffset(): Short {
        return EasyValue.readShort(mData, mOffset + offset_flags_fo)
    }

    fun setFlagsAndOffset(value: Short) {
        EasyValue.writeShort(mData, mOffset + offset_flags_fo, value)
    }

    fun getTTL(): Byte {
        return mData[mOffset + offset_ttl]
    }

    fun setTTL(value: Byte) {
        mData[mOffset + offset_ttl] = value
    }

    fun getProtocol(): Byte {
        return mData[mOffset + offset_proto]
    }

    fun setProtocol(value: Byte) {
        mData[mOffset + offset_proto] = value
    }

    fun getCrc(): Short {
        return EasyValue.readShort(mData, mOffset + offset_crc)
    }

    fun setCrc(value: Short) {
        EasyValue.writeShort(mData, mOffset + offset_crc, value)
    }

    fun getSourceIP(): Int {
        return EasyValue.readInt(mData, mOffset + offset_src_ip)
    }

    fun setSourceIP(value: Int) {
        EasyValue.writeInt(mData, mOffset + offset_src_ip, value)
    }

    fun getDestinationIP(): Int {
        return EasyValue.readInt(mData, mOffset + offset_dest_ip)
    }

    fun setDestinationIP(value: Int) {
        EasyValue.writeInt(mData, mOffset + offset_dest_ip, value)
    }

    override fun toString(): String {
        return String.format("%s->%s Pro=%s,HLen=%d",
                IPAddress.hexIpToString(getSourceIP()),
                IPAddress.hexIpToString(getDestinationIP()),
                getProtocol(),
                getHeaderLength())
    }

    fun setLocalServicePort(port: Short) {
        mLocalServicePort = port
    }

    private fun checksum(): Boolean {
        val oldCrc = getCrc()
        setCrc(0.toShort())
        val newCrc = Checksum.checksum(
                0, mData, mOffset, getHeaderLength())
        setCrc(newCrc)

        if (oldCrc != newCrc) {
            return false
        }

        val bodyLen = getTotalLength() - getHeaderLength()
        if (bodyLen < 0)
            return false

        var sum = Checksum.getsum(mData, mOffset + offset_src_ip, 8)
        sum += getProtocol() and 0xFF.toByte()
        sum += bodyLen.toLong()

        return mTCPPacket.checksum(sum, bodyLen)
    }

    private fun processTCPPacket(size: Int, localIP: Int): Pair<IPAccessDirection, Int> {
        mTCPPacket.mOffset = getHeaderLength()
        if (getSourceIP() == localIP) {
            if (mTCPPacket.getSourcePort() == mLocalServicePort) {
                val iport = PortMapping.get(mTCPPacket.getDestinationPort())
                if (iport != null) {
                    setSourceIP(getDestinationIP())
                    mTCPPacket.setSourcePort(iport.mPort)
                    setDestinationIP(localIP)
                    if (!checksum()) {
                        SSLocalLogging.error(TAG,
                                "TCP/IP checksum error: " + iport.toString())
                    }
                    iport.mBytesRecv += size
                    return Pair(IPAccessDirection.IP_ACCESS_INCOMING, size)
                }
            } else {
                val port = mTCPPacket.getSourcePort()
                var iport = PortMapping.get(port)
                if (iport == null || iport.mIP != getDestinationIP() ||
                        iport.mPort != mTCPPacket.getDestinationPort()) {
                    iport = PortMapping.IPort(getDestinationIP(), mTCPPacket.getDestinationPort())
                    PortMapping.add(port, iport)
                }

                iport.mPacketsSent++

                val tcpDataSize = getDataLength() - mTCPPacket.getHeaderLength()
                if (iport.mPacketsSent == 2 && tcpDataSize == 0) {
                    return Pair(IPAccessDirection.IP_ACCESS_OUTCOMING, 0)
                }

                setSourceIP(getDestinationIP())
                setDestinationIP(localIP)
                mTCPPacket.setDestinationPort(mLocalServicePort)

                checksum()
                iport.mBytesSent += tcpDataSize
                return Pair(IPAccessDirection.IP_ACCESS_OUTCOMING, tcpDataSize)
            }
        }

        return Pair(IPAccessDirection.IP_ACCESS_OUTCOMING, 0)
    }

    private fun processUDPPacket(size: Int, localIP: Int): Pair<IPAccessDirection, Int> {
        mUDPPacket.mOffset = getHeaderLength()
        if (getSourceIP() == localIP && mUDPPacket.getDestinationPort() == 53.toShort()) {
            mDNSBuffer.clear()
            mDNSBuffer.limit(getDataLength() - 8)
        }

        return Pair(IPAccessDirection.IP_ACCESS_OUTCOMING, 0)
    }

    fun process(size: Int, localIP: Int): Pair<IPAccessDirection, Int> {
        when (getProtocol()) {
            TCP -> return processTCPPacket(size, localIP)
            UDP -> return processUDPPacket(size, localIP)
            else -> {
                SSLocalLogging.debug(TAG, "Unknow protocol packet")
                return null as Pair<IPAccessDirection, Int>
            }
        }
    }
}