package com.kkt.sslocal

import android.app.Activity
import android.content.Context
import android.content.Intent
import com.kkt.tcpip.IPPacket

/**
 * Created by owen on 18-4-4.
 */
class SSVpnService {
    companion object {
        val SS_VPN_SERVICE_REQUEST: Int = 0x8001
        private val TAG = "SSVpnService"
        private var mVpnThreadRunning = false

        private val mPacket: ByteArray = ByteArray(SSVpnConfig.SS_VPN_MTU)
        private val mIPPacket = IPPacket(mPacket, 0)

        private var mRecvBytes: Long = 0
        private var mSendBytes: Long = 0

        fun getNetworkFlow(): Pair<Long, Long> {
            return Pair(mRecvBytes, mSendBytes)
        }

        private fun waitVpnServicePrepared(context: Context) {
            while (null != SSVpnImplService.prepare(context)) {
                SSLocalLogging.debug(TAG, "VPN prepare wait")
                Thread.sleep(100)
            }
        }

        interface SSVpnServiceEventListener {
            fun onVpnServiceCrash()
            fun onVpnServiceStart(vpnSessionName: String)
        }

        var mSSVpnServiceEventListener: SSVpnServiceEventListener? = null

        private fun initVpnInterface(configIntent: Intent) {
            SSVpnImplService.mVpnServiceInstance?.setup(configIntent)
        }

        class VpnThread(activity: Activity, configIntent: Intent) : Thread() {
            private val mActivity: Activity = activity
            private val mConfigIntent: Intent = configIntent
            override fun run() {
                waitVpnServicePrepared(mActivity)
                initVpnInterface(mConfigIntent)
                do {
                    val size: Int = SSVpnImplService.mVpnServiceInstance?.read(mPacket)!!
                    when (size) {
                        -1 -> {
                            SSLocalLogging.error(TAG, "VPN interface corrupted")
                            mSSVpnServiceEventListener?.onVpnServiceCrash()
                        }
                        0 -> sleep(10)
                        else -> {
                            val (direction, bytes) = mIPPacket.process(size,
                                    SSVpnImplService.mVpnServiceInstance?.mLocalIpIntAddr!!)
                            when (direction) {
                                IPPacket.IPAccessDirection.IP_ACCESS_INCOMING -> mRecvBytes += bytes
                                IPPacket.IPAccessDirection.IP_ACCESS_OUTCOMING -> mSendBytes += bytes
                            }
                            SSVpnImplService.mVpnServiceInstance?.write(mPacket, size)
                        }
                    }

                    if (size < 0) break
                } while (mVpnThreadRunning)
            }
        }

        private var mVpnThread: VpnThread? = null

        fun start(activity: Activity) {
            if (null == activity.startService(
                            Intent(activity,
                                    SSVpnImplService::class.java))) {
                SSLocalLogging.error(TAG, "Fail to start VPN service")
            }
        }

        fun initialize(activity: Activity, configIntent: Intent,
                       vpnServiceEventListener: SSVpnServiceEventListener) {
            SSLocalLogging.enableLogging()
            mSSVpnServiceEventListener = vpnServiceEventListener

            SSVpnImplService.setVpnServiceEventListener(
                    object: SSVpnImplService.SSVpnImplEventListener {
                        override fun onVpnServiceDestroy() {
                            SSLocalLogging.debug(TAG, "VPN Service Destroy")
                        }

                        override fun onVpnServiceCreate() {
                            SSLocalLogging.debug(TAG, "VPN Service Create")
                        }

                        override fun onVpnServiceStart() {
                            if (!mVpnThreadRunning) {
                                mVpnThreadRunning = true
                                mVpnThread = VpnThread(activity, configIntent)
                                mVpnThread?.start()
                                mSSVpnServiceEventListener?.onVpnServiceStart(
                                        SSVpnConfig.SS_SESSION_NAME)
                            }
                        }
                    })
            val intent: Intent? = SSVpnImplService.prepare(activity)
            if (null == intent) {
                start(activity)
            } else {
                activity.startActivityForResult(intent, SS_VPN_SERVICE_REQUEST)
            }
        }

        fun destroy() {
            mVpnThreadRunning = false
            mVpnThread?.join()
            SSVpnImplService.mVpnServiceInstance?.cleanup()
            SSVpnImplService.stopSelf()
        }

        fun protectSocket(socket: Int) {
            SSVpnImplService.protectSocket(socket)
        }
    }
}