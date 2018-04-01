package com.kkt.ssserver

import java.util.*

/**
 * Created by owen on 18-3-5.
 */
class SSProxyServer(port: Int, password: String) {
    private var mServerThread: Thread? = null

    private val SS_SERVER_CORE = "ss-server"
    private val SS_SERVER_CMD = ArrayList<String>()

    interface OnProtectSocketListener {
        fun onProtectSocket(socket: Int)
    }

    private var mProtectSocketListener: OnProtectSocketListener? = null

    init {
        SS_SERVER_CMD.add(SS_SERVER_CORE)
        SS_SERVER_CMD.add("-s")
        SS_SERVER_CMD.add("127.0.0.1")
        SS_SERVER_CMD.add("-p")
        SS_SERVER_CMD.add(port.toString())
        SS_SERVER_CMD.add("-k")
        SS_SERVER_CMD.add(password)
        SS_SERVER_CMD.add("-m")
        SS_SERVER_CMD.add("aes-256-cfb")

        System.loadLibrary(SS_SERVER_CORE)
    }

    fun start() {
        mServerThread = object : Thread() {
            override fun run() {
                SSProxyServerStart(SS_SERVER_CMD.size, SS_SERVER_CMD)
            }
        }

        mServerThread!!.start()
    }

    fun stop() {
        SSProxyServerStop()
        try {
            mServerThread!!.join()
        } catch (e: InterruptedException) {
            e.printStackTrace()
        }

    }

    private external fun SSProxyServerStart(argc: Int, argv: ArrayList<String>): Int
    private external fun SSProxyServerStop(): Int

    fun setOnProtectSocketListener(listener: OnProtectSocketListener?) {
        mProtectSocketListener = listener
    }

    fun protectSocket(socket: Int) {
        mProtectSocketListener?.onProtectSocket(socket)
    }
}