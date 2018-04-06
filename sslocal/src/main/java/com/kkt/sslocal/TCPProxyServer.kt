package com.kkt.sslocal

import com.kkt.sscontrol.SSVpnControl
import com.kkt.sstunnel.LocalTunnel
import com.kkt.sstunnel.Tunnel
import com.kkt.sstunnel.datagram.DatagramTunnel
import com.kkt.sstunnel.socks5.Socks5Tunnel
import com.kkt.ssvpn.SSVpnConfig
import com.kkt.tcpip.IPPacket
import com.kkt.tcpip.PortMapping
import com.kkt.utils.SSLocalLogging
import java.io.IOException
import java.net.InetSocketAddress
import java.nio.channels.SelectionKey
import java.nio.channels.Selector
import java.nio.channels.ServerSocketChannel
import java.nio.channels.SocketChannel

/**
 * Created by owen on 18-4-5.
 */
class TCPProxyServer(port: Int) {
    var mTCPProxyServerRunning: Boolean = false
    private lateinit var mTCPProxySocketChannel: ServerSocketChannel
    private lateinit var mSelector: Selector

    val mPort = port

    val TAG = "TCPProxyServer"

    private fun getDestAddress(localChannel: SocketChannel): InetSocketAddress? {
        val port = localChannel.socket().port.toShort()
        val iport = PortMapping.get(port)
        if (iport != null) {
            return if (SSVpnControl.bypass(iport.mHost, iport.mIP)) {
                InetSocketAddress.createUnresolved(iport.mHost,
                        iport.mPort.toInt() and 0xFFFF)
            } else {
                InetSocketAddress(localChannel.socket().inetAddress,
                        iport.mPort.toInt() and 0xFFFF)
            }
        }
        return null
    }

    private fun onAcceptable() {
        var localChannel = mTCPProxySocketChannel.accept()
        var localTunnel = LocalTunnel(localChannel, mSelector)
        val destAddress = getDestAddress(localChannel)
        var remoteTunnel: Tunnel? = if (!SSVpnControl.mEnableRtcTunnel) {
            SSVpnConfig.mVpnServerAddress?.let { Socks5Tunnel(it, mSelector) }
        } else {
            DatagramTunnel(mSelector)
        }

        remoteTunnel?.setBrotherTunnel(localTunnel)
        remoteTunnel?.let { localTunnel.setBrotherTunnel(it) }
        destAddress?.let { remoteTunnel?.connect(it) }
    }

    private var mTCPProxyServerThread: Thread = Thread(Runnable {
        do {
            mSelector.select(100)
            val keyIterator = mSelector.selectedKeys().iterator()
            while (keyIterator.hasNext()) {
                val key = keyIterator.next()
                if (key.isValid) {
                    when {
                        key.isReadable -> (key.attachment() as Tunnel).onReadable(key)
                        key.isWritable -> (key.attachment() as Tunnel).onWritable(key)
                        key.isConnectable -> (key.attachment() as Tunnel).onConnectable()
                        key.isAcceptable -> onAcceptable()
                    }
                }
                keyIterator.remove()
            }
        } while (mTCPProxyServerRunning)
    })

    fun initialize() {
        mTCPProxyServerRunning = true

        mSelector = Selector.open()
        mTCPProxySocketChannel = ServerSocketChannel.open()

        mTCPProxySocketChannel.configureBlocking(false)
        mTCPProxySocketChannel.socket().bind(InetSocketAddress(mPort))
        try {
            mTCPProxySocketChannel.register(mSelector, SelectionKey.OP_ACCEPT)
        } catch (e: IOException) {
            e.printStackTrace()
        }

        IPPacket.setLocalServicePort(
                mTCPProxySocketChannel.socket().localPort.toShort())
        mTCPProxyServerThread.start()
    }

    fun destroy() {
        mTCPProxyServerRunning = false
        mTCPProxyServerThread.join()

        mTCPProxySocketChannel.close()
        mSelector.close()
    }
}