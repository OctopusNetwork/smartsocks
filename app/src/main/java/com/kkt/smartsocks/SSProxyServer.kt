package com.kkt.smartsocks

import android.content.Context
import java.io.File

/**
 * Created by owen on 18-3-5.
 */
class SSProxyServer(context: Context) {
    val mContext: Context = context

    val SS_SERVER_EXEC: String = "libss-server.so"
    val SS_SERVER_CMD: ArrayList<String> = arrayListOf(
            File(mContext.applicationInfo.nativeLibraryDir, SS_SERVER_EXEC).absolutePath,
            "-s", "0.0.0.0", "-p", "10993", "-k", "1qaz2wsx", "-m", "aes-256-cfb", "-l", "10999")

    val mSSProxyServerProcess: GuardedProcess = GuardedProcess(mContext, SS_SERVER_CMD)

    fun start() {
        mSSProxyServerProcess.start()
    }

    fun stop() {
        mSSProxyServerProcess.destroy()
    }
}