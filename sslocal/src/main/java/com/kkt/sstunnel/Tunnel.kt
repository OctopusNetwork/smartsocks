package com.kkt.sstunnel

import java.net.InetSocketAddress
import java.nio.ByteBuffer
import java.nio.channels.SelectionKey
import java.nio.channels.Selector

/**
 * Created by owen on 18-4-5.
 */
abstract class Tunnel(role: TunnelRole, selector: Selector) {
    private val TAG = "Tunnel"

    enum class TunnelRole {
        /**
         *  Read local access data then write to remote tunnel
         *  Read remote tunnel return data then write to local request
         */
        TUNNEL_ROLE_LOCAL,
        /**
         *  Read local tunel data then write to VPN server or RTC channel
         * Read VPN server or RTC channel return data then write to local tunnel
         */
        TUNNEL_ROLE_REMOTE
    }

    var mRole = role
    var mBrotherTunnel: Tunnel? = null
    lateinit var mDestAddress: InetSocketAddress
    var mSelector: Selector = selector

    fun setBrotherTunnel(tunnel: Tunnel) { mBrotherTunnel = tunnel }

    protected open fun onConnected() { }
    protected open fun isTunnelEstablished(): Boolean { return true }
    open fun beforeSend(buffer: ByteBuffer) { }
    open fun afterReceived(buffer: ByteBuffer) { }
    protected open fun onDispose() {
        val brotherTunnel = mBrotherTunnel
        mBrotherTunnel = null
        brotherTunnel?.dispose()
    }

    open fun connect(destAddress: InetSocketAddress) {
        mDestAddress = destAddress
    }

    abstract fun beginReceive()
    abstract fun write(buffer: ByteBuffer): Int?
    abstract fun scheduleRemainWrite(buffer: ByteBuffer)
    abstract fun onTunnelEstablished()
    abstract fun onConnectable()
    abstract fun onReadable(key: SelectionKey)
    abstract fun onWritable(key: SelectionKey)
    open fun dispose() { }
}