package com.kkt.sstunnel.socks5

import com.kkt.cryptor.NoneCryptor
import com.kkt.sstunnel.SocketChannelTunnel
import com.kkt.ssvpn.SSVpnService
import java.net.InetSocketAddress
import java.nio.ByteBuffer
import java.nio.channels.SelectionKey
import java.nio.channels.Selector

/**
 * Created by owen on 18-4-5.
 */
class Socks5Tunnel(serverAddress: InetSocketAddress, selector: Selector) :
        SocketChannelTunnel(TunnelRole.TUNNEL_ROLE_REMOTE, selector) {

    var mServerAddress = serverAddress

    override fun onConnected() {
        Socks5Protocol.handshake(mServerAddress, NoneCryptor())
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
        super.connect(destAddress)
        if (mSocketChannel?.socket()?.let { SSVpnService.protectSocket(it) }!!) {
            mSocketChannel?.register(mSelector, SelectionKey.OP_CONNECT, this)
            mSocketChannel?.connect(mServerAddress)
        }
    }

    override fun beginReceive() {

    }

    override fun onTunnelEstablished() {

    }

    override fun onReadable(key: SelectionKey) {

    }

    override fun onWritable(key: SelectionKey) {

    }

    override fun dispose() {

    }
}