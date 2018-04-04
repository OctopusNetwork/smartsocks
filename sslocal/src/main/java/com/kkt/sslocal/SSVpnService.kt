package com.kkt.sslocal

import android.app.Activity
import android.content.Context
import android.content.Intent

/**
 * Created by owen on 18-4-4.
 */
class SSVpnService {
    companion object {
        val SS_VPN_SERVICE_REQUEST: Int = 0x8001
        private val TAG = "SSVpnService"
        private var mVpnThreadRunning = false

        private val mPacket: ByteArray = ByteArray(4096)

        private fun waitVpnServicePrepared(context: Context) {
            while (null != SSVpnImplService.prepare(context)) {
                SSLocalLogging.debug(TAG, "VPN prepare wait")
                Thread.sleep(100)
            }
        }

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
                    val size = SSVpnImplService.mVpnServiceInstance?.read(mPacket)
                    if (-1 == size) {
                        SSLocalLogging.error(TAG, "VPN interface corrupted")
                        break
                    }

                    if (0 == size) {
                        sleep(10)
                        continue
                    }

                    SSLocalLogging.debug(TAG, "VPN data $size bytes")
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

        fun initialize(activity: Activity, configIntent: Intent) {
            SSLocalLogging.enableLogging()

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