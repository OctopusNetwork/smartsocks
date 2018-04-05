package com.kkt.sslocal

import com.kkt.sstunnel.SSLocalLogging
import com.kkt.tcpip.IPPacket
import java.io.IOException
import java.net.InetSocketAddress
import java.nio.channels.SelectionKey
import java.nio.channels.Selector
import java.nio.channels.ServerSocketChannel

/**
 * Created by owen on 18-4-5.
 */
class TCPProxyServer(port: Int) {
    var mTCPProxyServerRunning: Boolean = false
    private lateinit var mTCPProxySocket: ServerSocketChannel
    private lateinit var mSelector: Selector

    val mPort = port

    val TAG = "TCPProxyServer"

    private var mTCPProxyServerThread: Thread = Thread(Runnable {
        do {
            val keyNum = mSelector.select(100)
            if (0 == keyNum) {
                continue
            } else {
                SSLocalLogging.debug(TAG,
                        "" + keyNum + " keys ready")
            }
            val keyIterator = mSelector.selectedKeys().iterator()
            while (keyIterator.hasNext()) {
                val key = keyIterator.next()
                if (key.isValid) {
                    when {
                        key.isReadable -> SSLocalLogging.debug(TAG, "Readable")
                        key.isWritable -> SSLocalLogging.debug(TAG, "Writable")
                        key.isConnectable -> SSLocalLogging.debug(TAG, "Connectable")
                        key.isAcceptable -> SSLocalLogging.debug(TAG, "Acceptable")
                    }
                }
                keyIterator.remove()
            }
        } while (mTCPProxyServerRunning)
    })

    fun initialize() {
        mTCPProxyServerRunning = true

        mSelector = Selector.open()
        mTCPProxySocket = ServerSocketChannel.open()

        mTCPProxySocket.configureBlocking(false)
        mTCPProxySocket.socket().bind(InetSocketAddress(mPort))
        try {
            mTCPProxySocket.register(mSelector, SelectionKey.OP_ACCEPT)
        } catch (e: IOException) {
            e.printStackTrace()
        }

        IPPacket.setLocalServicePort(
                mTCPProxySocket.socket().localPort.toShort())
        mTCPProxyServerThread.start()
    }

    fun destroy() {
        mTCPProxyServerRunning = false
        mTCPProxyServerThread.join()

        mTCPProxySocket.close()
        mSelector.close()
    }
}