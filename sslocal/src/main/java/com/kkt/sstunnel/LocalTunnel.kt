package com.kkt.sstunnel

import java.net.InetSocketAddress
import java.nio.ByteBuffer
import java.nio.channels.SelectionKey
import java.nio.channels.Selector
import java.nio.channels.SocketChannel

/**
 * Created by owen on 18-4-5.
 */
class LocalTunnel(socketChannel: SocketChannel, selector: Selector) :
        SocketChannelTunnel(socketChannel, TunnelRole.TUNNEL_ROLE_LOCAL, selector) {
    /**
     * Special Local tunnel to accept local access and write to remote tunnel
     * And read remote tunnel return data then write to local request
     */

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

    override fun connect(destAddress: InetSocketAddress) {

    }

    override fun beginReceive() {

    }

    override fun write(buffer: ByteBuffer, copyRemainData: Boolean) {

    }

    override fun onTunnelEstablished() {

    }

    override fun onConnectable() {

    }

    override fun onReadable(key: SelectionKey) {

    }

    override fun onWritable(key: SelectionKey) {

    }

    override fun dispose() {

    }
}