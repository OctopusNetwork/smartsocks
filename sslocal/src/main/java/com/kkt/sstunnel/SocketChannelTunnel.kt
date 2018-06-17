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
    var mUseExternalChannel = false

    constructor(role: TunnelRole, selector: Selector) :
            this(null, role, selector)

    constructor(socketChannel: SocketChannel?, role: TunnelRole, selector: Selector) :
            super(role, selector) {
        mUseExternalChannel = (null != socketChannel)
        mSocketChannel = socketChannel ?: SocketChannel.open()
        mSocketChannel?.configureBlocking(false)
    }

    override fun beginReceive() {
        mSocketChannel?.register(mSelector, SelectionKey.OP_READ, this)
    }

    override fun write(buffer: ByteBuffer): Int? {
        var bytesWrite = 0
        do {
            var wlen: Int?
            try {
                wlen = mSocketChannel?.write(buffer)
                when (wlen) {
                    0 -> return bytesWrite
                    else -> bytesWrite += wlen!!
                }
            } catch (e: IOException) {
                e.printStackTrace()
                return -1
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
            dispose()
        }
    }

    override fun onReadable(key: SelectionKey) {
        mChannelBuffer.clear()
        var bytesRead: Int? = 0

        try {
            bytesRead = mSocketChannel?.read(mChannelBuffer)
            if (bytesRead!! > 0) {
                mChannelBuffer.flip()
                afterReceived(mChannelBuffer)
                if (isTunnelEstablished() && mChannelBuffer.hasRemaining()) {
                    mBrotherTunnel?.beforeSend(mChannelBuffer)
                    var wlen = mBrotherTunnel?.write(mChannelBuffer)
                    if (0 <= wlen!!) {
                        if (mChannelBuffer.hasRemaining()) {
                            mBrotherTunnel?.scheduleRemainWrite(mChannelBuffer)
                            key.cancel()
                        }
                    } else {
                        dispose()
                        key.cancel()
                    }
                }
            } else if (bytesRead < 0) {
                dispose()
            }
        } catch (e: IOException) {
            SSLocalLogging.error(TAG, e.toString())
            dispose()
        }
    }

    override fun onWritable(key: SelectionKey) {
        this.beforeSend(mWriteRemainingBuffer)
        val wlen = write(mWriteRemainingBuffer)
        if (0 <= wlen!!) {
            if (mWriteRemainingBuffer.hasRemaining()) {
                key.cancel()
                if (isTunnelEstablished()) {
                    mBrotherTunnel?.beginReceive()
                } else {
                    this.beginReceive()
                }
            }
        } else {
            dispose()
        }
    }

    override fun dispose() {
        super.dispose()
        if (!mUseExternalChannel) {
            mSocketChannel?.close()
        }
    }
}