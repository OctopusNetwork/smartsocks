package com.kkt.sslocal

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

        fun getDefaultLocalIP(): IPAddress {
            return IPAddress("10.8.0.2", 32)
        }
    }
}