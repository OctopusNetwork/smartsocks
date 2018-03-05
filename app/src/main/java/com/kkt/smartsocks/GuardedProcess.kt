package com.kkt.smartsocks

import android.annotation.TargetApi
import android.content.Context
import android.content.ContextWrapper
import android.os.Build
import android.os.SystemClock
import android.util.Log
import java.io.IOException
import java.io.InputStream
import java.util.concurrent.Semaphore

class GuardedProcess(context: Context, private val cmd: List<String>) {
    companion object {
        private const val TAG = "GuardedProcess"
    }

    private lateinit var guardThread: Thread
    @Volatile
    private var isDestroyed = false
    @Volatile
    private lateinit var process: Process

    val mContext: Context = context

    @TargetApi(24)
    class DeviceContext(context: Context) : ContextWrapper(context.createDeviceProtectedStorageContext()) {
        /**
         * Thou shalt not get the REAL underlying application context which would no longer be operating under device
         * protected storage.
         */
        override fun getApplicationContext(): Context = this
    }

    val mDeviceContext: Context by lazy { if (Build.VERSION.SDK_INT < 24) mContext else DeviceContext(mContext) }

    /**
     * Wrapper for kotlin.concurrent.thread that tracks uncaught exceptions.
     */
    fun thread(start: Boolean = true, isDaemon: Boolean = false, contextClassLoader: ClassLoader? = null,
               name: String? = null, priority: Int = -1, block: () -> Unit): Thread {
        val thread = kotlin.concurrent.thread(false, isDaemon, contextClassLoader, name, priority, block)
        if (start) thread.start()
        return thread
    }

    private fun streamLogger(input: InputStream, logger: (String, String) -> Int) = thread {
        try {
            input.bufferedReader().useLines { it.forEach { logger(TAG, it) } }
        } catch (_: IOException) { }    // ignore
    }

    fun start(onRestartCallback: (() -> Unit)? = null): GuardedProcess {
        val semaphore = Semaphore(1)
        semaphore.acquire()
        var ioException: IOException? = null
        guardThread = thread(name = "GuardThread-" + cmd.first()) {
            try {
                var callback: (() -> Unit)? = null
                while (!isDestroyed) {
                    val startTime = SystemClock.elapsedRealtime()

                    process = ProcessBuilder(cmd)
                            .redirectErrorStream(true)
                            .directory(mDeviceContext.filesDir)
                            .start()

                    streamLogger(process.inputStream, Log::i)
                    streamLogger(process.errorStream, Log::e)

                    if (callback == null) callback = onRestartCallback else callback()

                    semaphore.release()
                    process.waitFor()

                    synchronized(this) {
                        if (SystemClock.elapsedRealtime() - startTime < 1000) {
                            isDestroyed = true
                        }
                    }
                }
            } catch (_: InterruptedException) {
                destroyProcess()
            } catch (e: IOException) {
                ioException = e
            } finally {
                semaphore.release()
            }
        }
        semaphore.acquire()
        if (ioException != null) throw ioException!!
        return this
    }

    fun destroy() {
        isDestroyed = true
        guardThread.interrupt()
        destroyProcess()
        try {
            guardThread.join()
        } catch (_: InterruptedException) { }
    }

    private fun destroyProcess() {
        if (Build.VERSION.SDK_INT < 24) @Suppress("DEPRECATION") {
        }
        process.destroy()
    }
}
