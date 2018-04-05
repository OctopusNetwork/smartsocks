package com.kkt.sslocal

import com.kkt.dns.DNSPacket
import com.kkt.sstunnel.SSLocalLogging
import com.kkt.sstunnel.SSVpnService
import com.kkt.tcpip.IPPacket
import java.net.DatagramPacket
import java.net.DatagramSocket
import java.nio.ByteBuffer

/**
 * Created by owen on 18-4-5.
 */
class UDPProxyServer {
    var mUDPProxyServerRunning: Boolean = false
    private lateinit var mUDPProxySocket: DatagramSocket

    val TAG = "UDPProxyServer"

    var mUDPProxyServerThread: Thread = Thread(object: Runnable {
        override fun run() {
            val RECEIVE_BUFFER = ByteArray(2000)
            val ipPacket = IPPacket(RECEIVE_BUFFER, 0)
            ipPacket.setDefault()

            var dnsBuffer = ByteBuffer.wrap(RECEIVE_BUFFER)
            dnsBuffer.position(28)
            dnsBuffer = dnsBuffer.slice()

            val packet = DatagramPacket(RECEIVE_BUFFER,
                    28, RECEIVE_BUFFER.size - 28)
            do {
                packet.length = RECEIVE_BUFFER.size - 28
                mUDPProxySocket.receive(packet)

                dnsBuffer.clear()
                dnsBuffer.limit(packet.length)
                try {
                    DNSPacket.FromBytes(dnsBuffer,
                            this@UDPProxyServer)?.
                            processResponse(ipPacket)
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            } while (mUDPProxyServerRunning && !mUDPProxySocket.isClosed)
        }
    })

    fun initialize() {
        mUDPProxyServerRunning = true
        mUDPProxySocket = DatagramSocket(0)
        SSVpnService.protectSocket(mUDPProxySocket)
        mUDPProxyServerThread.start()
    }

    fun destroy() {
        mUDPProxyServerRunning = false
        mUDPProxySocket.close()
        mUDPProxyServerThread.join()
    }

    fun send(packet: DatagramPacket) {
        mUDPProxySocket.send(packet)
    }
}