package com.kkt.sstunnel

import com.kkt.utils.SSLocalLogging
import java.nio.ByteBuffer
import java.nio.channels.SelectionKey
import java.nio.channels.Selector
import java.nio.channels.SocketChannel

/**
 * Created by owen on 18-4-5.
 */
open class SocketChannelTunnel : Tunnel {
    var mSocketChannel: SocketChannel? = null

    val TAG = "SocketChannelTunnel"

    constructor(role: TunnelRole, selector: Selector) :
            this(null, role, selector)

    constructor(socketChannel: SocketChannel?, role: TunnelRole, selector: Selector) :
            super(role, selector) {
        mSocketChannel = socketChannel ?: SocketChannel.open()
        mSocketChannel?.configureBlocking(false)
    }

    override fun onConnected() {

    }

    override fun isTunnelEstablished(): Boolean {
        return false
    }

    override fun beforeSend(buffer: ByteBuffer) {

    }

    override fun afterReceived(buffer: ByteBuffer) {

    }

    override fun onDispose() {

    }

    override fun beginReceive() {

    }

    override fun write(buffer: ByteBuffer, copyRemainData: Boolean) {
        mSocketChannel?.write(buffer)
    }

    override fun onTunnelEstablished() {

    }

    override fun onConnectable() {
        if (mSocketChannel?.finishConnect()!!) {
            onConnected()
        } else {
            dispose()
        }
    }

    override fun onReadable(key: SelectionKey) {

    }

    override fun onWritable(key: SelectionKey) {

    }

    override fun dispose() {

    }
}