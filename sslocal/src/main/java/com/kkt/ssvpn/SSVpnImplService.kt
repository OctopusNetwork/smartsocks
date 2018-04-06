package com.kkt.ssvpn

import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.net.VpnService
import android.os.ParcelFileDescriptor
import java.io.FileInputStream
import java.io.FileOutputStream
import java.net.DatagramSocket
import java.net.Socket
import java.util.*

/**
 * Created by owen on 18-4-4.
 */
class SSVpnImplService : VpnService() {
    val TAG = "SSVpnImplService"

    init {
        mVpnServiceInstance = this
        val it = mProtectSockets.iterator()
        while (it.hasNext()) {
            mVpnServiceInstance?.protect(it.next())
            it.remove()
        }
    }

    var mLocalIpIntAddr: Int = 0
    private var mVpnParcelFileDescriptor: ParcelFileDescriptor? = null
    private var mVpnOutputStream: FileOutputStream? = null
    private var mVpnInputStream: FileInputStream? = null

    interface SSVpnImplEventListener {
        fun onVpnServiceCreate()
        fun onVpnServiceStart()
        fun onVpnServiceDestroy()
    }

    companion object {
        var mVpnServiceInstance: SSVpnImplService? = null
        private var mProtectSocketsHandle: ArrayList<Int> = ArrayList()
        private var mProtectSockets: ArrayList<Socket> = ArrayList()
        private var mProtectDatagramSockets: ArrayList<DatagramSocket> = ArrayList()
        private var mVpnEventListener: SSVpnImplEventListener? = null

        fun prepare(context: Context) : Intent? {
            return VpnService.prepare(context)
        }

        fun stopSelf() {
            mVpnServiceInstance?.stopSelf()
        }

        fun protectSocket(socket: Int): Boolean? {
            return if (null == mVpnServiceInstance) {
                mProtectSocketsHandle.add(socket)
                true
            } else {
                mVpnServiceInstance?.protect(socket)
            }
        }

        fun protectSocket(socket: Socket): Boolean? {
            return if (null == mVpnServiceInstance) {
                mProtectSockets.add(socket)
                true
            } else {
                mVpnServiceInstance?.protect(socket)
            }
        }

        fun protectSocket(socket: DatagramSocket): Boolean? {
            return if (null == mVpnServiceInstance) {
                mProtectDatagramSockets.add(socket)
                true
            } else {
                mVpnServiceInstance?.protect(socket)
            }
        }

        fun setVpnServiceEventListener(listener: SSVpnImplEventListener) {
            mVpnEventListener = listener
        }
    }

    override fun onCreate() {
        mVpnEventListener?.onVpnServiceCreate()
        super.onCreate()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        mVpnEventListener?.onVpnServiceStart()
        for (socket in mProtectSocketsHandle) {
            mVpnServiceInstance?.protect(socket)
        }
        for (socket in mProtectSockets) {
            mVpnServiceInstance?.protect(socket)
        }
        for (socket in mProtectDatagramSockets) {
            mVpnServiceInstance?.protect(socket)
        }
        return super.onStartCommand(intent, flags, startId)
    }

    override fun onDestroy() {
        cleanup()
        mVpnEventListener?.onVpnServiceDestroy()
        super.onDestroy()
    }

    override fun onRevoke() {
        cleanup()
        super.onRevoke()
    }

    fun setup(configIntent: Intent) {
        val builder = Builder()
        builder.setMtu(SSVpnConfig.SS_VPN_MTU)
        val ipAddress = SSVpnConfig.getDefaultLocalIP()

        mLocalIpIntAddr = ipAddress.toInt()
        builder.addAddress(
                ipAddress.mAddress,
                ipAddress.mPrefixLength)

        for (dns in SSVpnConfig.mDnsList) {
            builder.addDnsServer(dns.mAddress)
        }

        if (0 < SSVpnConfig.mRouteList.size) {
            for (routeAddress in SSVpnConfig.mRouteList) {
                builder.addRoute(
                        routeAddress.mAddress,
                        routeAddress.mPrefixLength)
            }
            builder.addRoute(SSVpnConfig.SS_FAKE_IP, 16)
        } else {
            builder.addRoute("0.0.0.0", 0)
        }


        val systemProperties = Class.forName("android.os.SystemProperties")
        val method = systemProperties.getMethod("get", String::class.java)
        val servers = ArrayList<String>()
        for (name in arrayOf("net.dns1", "net.dns2", "net.dns3", "net.dns4")) {
            val value = method.invoke(null, name) as String
            if (value != null && "" != value && !servers.contains(value)) {
                servers.add(value)
                if (value.replace("\\d".toRegex(), "").length == 3) {
                    builder.addRoute(value, 32)
                } else {
                    builder.addRoute(value, 128)
                }
            }
        }

        val pendingIntent = PendingIntent.getActivity(
                this, 0, configIntent, 0)
        builder.setConfigureIntent(pendingIntent)

        builder.setSession(SSVpnConfig.SS_SESSION_NAME)
        mVpnParcelFileDescriptor = builder.establish()
        mVpnOutputStream = FileOutputStream(mVpnParcelFileDescriptor?.fileDescriptor)
        mVpnInputStream = FileInputStream(mVpnParcelFileDescriptor?.fileDescriptor)
    }

    fun cleanup() {
        mVpnInputStream?.close()
        mVpnInputStream = null
        mVpnOutputStream?.close()
        mVpnOutputStream = null
        mVpnParcelFileDescriptor?.close()
        mVpnParcelFileDescriptor = null
    }

    fun read(buf: ByteArray): Int? {
        return mVpnInputStream?.read(buf)
    }

    fun write(buf: ByteArray, size: Int, offset: Int = 0) {
        mVpnOutputStream?.write(buf, offset, size)
    }
}