package com.kkt.sscontrol

/**
 * Created by owen on 18-4-5.
 */
class SSVpnControl {
    companion object {
        var mEnableRtcTunnel: Boolean = false

        fun enableRtcTunnel() {
            mEnableRtcTunnel = true
        }

        fun bypass(host: String?, port: Int): Boolean {
            return false
        }
    }
}