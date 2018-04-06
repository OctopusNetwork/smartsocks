package com.kkt.sstunnel

import com.kkt.utils.SSLocalLogging
import java.io.IOException
import java.nio.ByteBuffer
import java.nio.channels.SelectionKey
import java.nio.channels.Selector
import java.nio.channels.SocketChannel

/**
 * Created by owen on 18-4-5.
 */
open class SocketChannelTunnel : Tunnel {
        var mSocketChannel: SocketChannel? = null
    companion object {
        var mChannelBuffer = ByteBuffer.allocate(20000)
        var mWriteRemainingBuffer = ByteBuffer.allocate(20000)
    }

    val TAG = "SocketChannelTunnel"

    constructor(role: TunnelRole, selector: Selector) :
            this(null, role, selector)

    constructor(socketChannel: SocketChannel?, role: TunnelRole, selector: Selector) :
            super(role, selector) {
        mSocketChannel = socketChannel ?: SocketChannel.open()
        mSocketChannel?.configureBlocking(false)
    }

    override fun beginReceive() {
        mSocketChannel?.register(mSelector, SelectionKey.OP_READ, this)
    }

    override fun write(buffer: ByteBuffer): Int? {
        var bytesWrite = 0
        do {
            val wlen = mSocketChannel?.write(buffer)
            SSLocalLogging.debug(TAG,
                    "Tunnel: " + mRole.toString() +
                        " write $bytesWrite bytes")
            when (wlen) {
                0 -> return bytesWrite
                else -> bytesWrite += wlen!!
            }
        } while (true)
    }

    override fun scheduleRemainWrite(buffer: ByteBuffer) {
        mWriteRemainingBuffer.put(buffer)
        mWriteRemainingBuffer.flip()
        mSocketChannel?.register(mSelector, SelectionKey.OP_WRITE, this)
    }

    override fun onTunnelEstablished() {
        beginReceive()
        mBrotherTunnel?.beginReceive()
    }

    override fun onConnectable() {
        try {
            if (mSocketChannel?.finishConnect()!!) onConnected()
            else dispose()
        } catch (e: IOException) {
            e.printStackTrace()
        }
    }

    override fun onReadable(key: SelectionKey) {
        mChannelBuffer.clear()
        val bytesRead: Int? = mSocketChannel?.read(mChannelBuffer)
        if (bytesRead!! > 0) {
            SSLocalLogging.debug(TAG,
                    "Tunnel " + mRole.toString() +
                        " read $bytesRead bytes")
            mChannelBuffer.flip()
            afterReceived(mChannelBuffer)
            if (isTunnelEstablished() && mChannelBuffer.hasRemaining()) {
                mBrotherTunnel?.beforeSend(mChannelBuffer)
                mBrotherTunnel?.write(mChannelBuffer)
                if (mChannelBuffer.hasRemaining()) {
                    mBrotherTunnel?.scheduleRemainWrite(mChannelBuffer)
                    key.cancel()
                }
            }
        } else if (bytesRead < 0) {
            this.dispose()
        }
    }

    override fun onWritable(key: SelectionKey) {
        this.beforeSend(mWriteRemainingBuffer)
        write(mWriteRemainingBuffer)
        if (mWriteRemainingBuffer.hasRemaining()) {
            key.cancel()
            if (isTunnelEstablished()) {
                mBrotherTunnel?.beginReceive()
            } else {
                this.beginReceive()
            }
        }
    }
}