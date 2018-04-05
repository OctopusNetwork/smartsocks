package com.kkt.dns

import com.kkt.sslocal.UDPProxyServer
import com.kkt.utils.SSLocalLogging
import com.kkt.ssvpn.SSVpnService
import com.kkt.tcpip.IPAddress
import com.kkt.tcpip.IPPacket
import java.net.DatagramPacket
import java.net.InetSocketAddress
import java.nio.ByteBuffer

/**
 * Created by owen on 18-4-5.
 */
class DNSPacket(udpProxyServer: UDPProxyServer?) {
    lateinit var Header: DNSHeader
    var Questions: Array<Question?>? = null
    var Resources: Array<Resource?>? = null
    var AResources: Array<Resource?>? = null
    var EResources: Array<Resource?>? = null

    val TAG = "DNSPacket"

    var Size: Int = 0

    var mQueryID: Short = 0
    var mQueryMap: HashMap<Short, QueryState> = HashMap()

    val mUDPProxyServer = udpProxyServer

    fun ToBytes(buffer: ByteBuffer) {
        Header.mQuestionCount = 0
        Header.mResourceCount = 0
        Header.mAResourceCount = 0
        Header.mEResourceCount = 0

        if (Questions != null)
            Header.mQuestionCount = Questions!!.size.toShort()
        if (Resources != null)
            Header.mResourceCount = Resources!!.size.toShort()
        if (AResources != null)
            Header.mAResourceCount = AResources!!.size.toShort()
        if (EResources != null)
            Header.mEResourceCount = EResources!!.size.toShort()

        this.Header.ToBytes(buffer)

        for (i in 0 until Header.mQuestionCount!!) {
            this.Questions!![i]?.ToBytes(buffer)
        }

        for (i in 0 until Header.mResourceCount!!) {
            this.Resources!![i]?.ToBytes(buffer)
        }

        for (i in 0 until Header.mAResourceCount!!) {
            this.AResources!![i]?.ToBytes(buffer)
        }

        for (i in 0 until Header.mEResourceCount!!) {
            this.EResources!![i]?.ToBytes(buffer)
        }
    }

    fun processRequest(ipPacket: IPPacket) {
        val state = QueryState()
        state.ClientQueryID = Header.mID
        state.QueryNanoTime = System.nanoTime()
        state.ClientIP = ipPacket.getSourceIP()
        state.ClientPort = ipPacket.mUDPPacket.getSourcePort()
        state.RemoteIP = ipPacket.getDestinationIP()
        state.RemotePort = ipPacket.mUDPPacket.getDestinationPort()

        mQueryID++
        Header.mID = mQueryID
        mQueryMap[mQueryID] = state

        val remoteAddress = InetSocketAddress(
                IPAddress.hexIpToInet4Address(state.RemoteIP),
                state.RemotePort.toInt())
        val packet = DatagramPacket(ipPacket.mUDPPacket.mData,
                ipPacket.mUDPPacket.mOffset + 8, Size)
        packet.socketAddress = remoteAddress

        SSLocalLogging.debug(TAG, "DNS Query " + Questions!![0]?.Domain +
                " -> " + packet.address.canonicalHostName)
        mUDPProxyServer?.send(packet)
    }

    fun processResponse(ipPacket: IPPacket) {
        val state = mQueryMap[Header.mID]
        mQueryMap.remove(Header.mID)

        if (state != null) {
            Header.mID = state.ClientQueryID
            ipPacket.setSourceIP(state.RemoteIP)
            ipPacket.setDestinationIP(state.ClientIP)
            ipPacket.setProtocol(IPPacket.UDP)
            ipPacket.setTotalLength(20 + 8 + Size)
            ipPacket.mUDPPacket.setSourcePort(state.RemotePort)
            ipPacket.mUDPPacket.setDestinationPort(state.ClientPort)
            ipPacket.mUDPPacket.setTotalLength(8 + Size)

            ipPacket.setupResponseUDPIPHeader()

            SSVpnService.write(ipPacket.mData,
                    ipPacket.mOffset, ipPacket.getTotalLength())
        }
    }

    companion object {

        fun FromBytes(buffer: ByteBuffer,
                      udpProxyServer: UDPProxyServer?):
                DNSPacket? {
            if (buffer.limit() < 12)
                return null
            if (buffer.limit() > 512)
                return null

            val packet = DNSPacket(udpProxyServer)
            packet.Size = buffer.limit()
            packet.Header = DNSHeader.FromBytes(buffer)

            if (packet.Header.mQuestionCount > 2 ||
                    packet.Header.mResourceCount > 50 ||
                    packet.Header.mAResourceCount > 50 ||
                    packet.Header.mEResourceCount > 50) {
                return null
            }

            packet.Questions = arrayOfNulls(packet.Header.mQuestionCount.toInt())
            packet.Resources = arrayOfNulls(packet.Header.mResourceCount.toInt())
            packet.AResources = arrayOfNulls(packet.Header.mAResourceCount.toInt())
            packet.EResources = arrayOfNulls(packet.Header.mEResourceCount.toInt())

            for (i in packet.Questions!!.indices) {
                packet.Questions!![i] = Question.FromBytes(buffer)
            }

            for (i in packet.Resources!!.indices) {
                packet.Resources!![i] = Resource.FromBytes(buffer)
            }

            for (i in packet.AResources!!.indices) {
                packet.AResources!![i] = Resource.FromBytes(buffer)
            }

            for (i in packet.EResources!!.indices) {
                packet.EResources!![i] = Resource.FromBytes(buffer)
            }

            return packet
        }

        fun ReadDomain(buffer: ByteBuffer, dnsHeaderOffset: Int): String {
            val sb = StringBuilder()
            var len = buffer.get().toInt() and 0xFF
            while (buffer.hasRemaining() && len > 0) {
                if (len and 0xc0 == 0xc0) {
                    // pointer 高2位为11表示是指针。如：1100 0000
                    // 指针的取值是前一字节的后6位加后一字节的8位共14位的值。
                    var pointer: Int = buffer.get().toInt() and 0xFF // 低8位
                    pointer = pointer or ((len and 0x3F) shl 8)      // 高6位

                    val newBuffer = ByteBuffer.wrap(buffer.array(),
                            dnsHeaderOffset + pointer,
                            dnsHeaderOffset + buffer.limit())
                    sb.append(ReadDomain(newBuffer, dnsHeaderOffset))
                    return sb.toString()
                } else {
                    while (len > 0 && buffer.hasRemaining()) {
                        sb.append((buffer.get().toInt() and 0xFF).toChar())
                        len--
                    }
                    sb.append('.')
                }
            }

            if (len == 0 && sb.isNotEmpty()) {
                sb.deleteCharAt(sb.length - 1)
            }
            return sb.toString()
        }

        fun WriteDomain(domain: String?, buffer: ByteBuffer) {
            if (domain == null || domain == "") {
                buffer.put(0.toByte())
                return
            }

            val arr = domain.split("\\.".toRegex())
                    .dropLastWhile { it.isEmpty() }.toTypedArray()
            for (item in arr) {
                if (arr.size > 1) {
                    buffer.put(item.length.toByte())
                }

                for (i in 0 until item.length) {
                    buffer.put(item.codePointAt(i).toByte())
                }
            }
        }
    }
}