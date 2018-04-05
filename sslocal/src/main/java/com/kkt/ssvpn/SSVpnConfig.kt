package com.kkt.ssvpn

import com.kkt.tcpip.IPAddress
import java.net.InetSocketAddress

/**
 * Created by owen on 18-4-4.
 */
class SSVpnConfig {
    companion object {
        val SS_VPN_MTU = 20000
        val SS_FAKE_IP = "172.25.0.0"
        val SS_SESSION_NAME = "SmartSocks"

        val mDnsList: ArrayList<IPAddress> = ArrayList()
        val mRouteList: ArrayList<IPAddress> = ArrayList()

        var mVpnServerAddress: InetSocketAddress? = null

        fun getDefaultLocalIP(): IPAddress {
            return IPAddress("10.8.0.2", 32)
        }
    }
}