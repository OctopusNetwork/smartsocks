package com.kkt.sstunnel.datagram

import com.kkt.sstunnel.Tunnel
import java.net.InetSocketAddress
import java.nio.ByteBuffer
import java.nio.channels.SelectionKey
import java.nio.channels.Selector

/**
 * Created by owen on 18-4-5.
 */
class DatagramTunnel(selector: Selector) :
        Tunnel(TunnelRole.TUNNEL_ROLE_REMOTE, selector) {
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