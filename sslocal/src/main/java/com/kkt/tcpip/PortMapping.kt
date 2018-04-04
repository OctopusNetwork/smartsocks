package com.kkt.tcpip

import android.support.v4.app.INotificationSideChannel

/**
 * Created by owen on 18-4-4.
 */
object PortMapping {
    class IPort(ip: Int, port: Short) {
        val mIP: Int = ip
        val mPort: Short = port

        var mPacketsSent: Int = 0
        var mBytesSent: Int = 0
        var mBytesRecv: Int = 0

        override fun toString(): String {
            return "" + IPAddress.hexIpToString(mIP) + "/" + mPort
        }
    }
    var mSessionMap: HashMap<Short, IPort> = HashMap()

    fun add(port: Short, iport: IPort) {
        mSessionMap[port] = iport
    }

    fun get(port: Short) : IPort? {
        return mSessionMap[port]
    }
}