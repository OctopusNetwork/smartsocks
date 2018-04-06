package com.kkt.sstunnel.socks5

import com.kkt.crypto.CryptFactory
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

    private var mServerAddress = serverAddress
    private var mTunnelEstablished = false
    private var mCryptor = CryptFactory.get(
            "aes-256-cfb", "1qaz2wsx")

    override fun onConnected() {
        val hsBuffer = Socks5Protocol.handshake(mDestAddress, mCryptor)
        write(hsBuffer)
        mTunnelEstablished = true
        onTunnelEstablished()
    }

    override fun isTunnelEstablished(): Boolean {
        return mTunnelEstablished
    }

    override fun beforeSend(buffer: ByteBuffer) {
        /**
         * Encrypt data
         */
        val bytes = ByteArray(buffer.limit())
        buffer.get(bytes)

        val newbytes = mCryptor.encrypt(bytes)

        buffer.clear()
        buffer.put(newbytes)
        buffer.flip()
    }

    override fun afterReceived(buffer: ByteBuffer) {
        /**
         * Decrypt data
         */
        val bytes = ByteArray(buffer.limit())
        buffer.get(bytes)
        val newbytes = mCryptor.decrypt(bytes)
        buffer.clear()
        buffer.put(newbytes)
        buffer.flip()
    }

    override fun onDispose() {
        mTunnelEstablished = false
    }

    override fun connect(destAddress: InetSocketAddress) {
        super.connect(destAddress)
        if (mSocketChannel?.socket()?.let { SSVpnService.protectSocket(it) }!!) {
            mSocketChannel?.register(mSelector, SelectionKey.OP_CONNECT, this)
            mSocketChannel?.connect(mServerAddress)
        }
    }

    override fun dispose() {
        mTunnelEstablished = false
    }
}