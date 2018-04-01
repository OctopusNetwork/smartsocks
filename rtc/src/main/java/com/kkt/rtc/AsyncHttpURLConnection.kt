/*
 *  Copyright 2015 The WebRTC Project Authors. All rights reserved.
 *
 *  Use of this source code is governed by a BSD-style license
 *  that can be found in the LICENSE file in the root of the source
 *  tree. An additional intellectual property rights grant can be found
 *  in the file PATENTS.  All contributing project authors may
 *  be found in the AUTHORS file in the root of the source tree.
 */

package com.kkt.rtc

import java.io.IOException
import java.io.InputStream
import java.io.OutputStream
import java.net.HttpURLConnection
import java.net.SocketTimeoutException
import java.net.URL
import java.util.Scanner

/**
 * Asynchronous http requests implementation.
 */
class AsyncHttpURLConnection(private val method: String, private val url: String, private val message: String?, private val events: AsyncHttpEvents) {
    private var contentType: String? = null

    /**
     * Http requests callbacks.
     */
    interface AsyncHttpEvents {
        fun onHttpError(errorMessage: String)
        fun onHttpComplete(response: String)
    }

    fun setContentType(contentType: String) {
        this.contentType = contentType
    }

    fun send() {
        Thread(Runnable { this.sendHttpMessage() }).start()
    }

    private fun sendHttpMessage() {
        try {
            val connection = URL(url).openConnection() as HttpURLConnection
            var postData = ByteArray(0)
            if (message != null) {
                postData = message.toByteArray(charset("UTF-8"))
            }
            connection.requestMethod = method
            connection.useCaches = false
            connection.doInput = true
            connection.connectTimeout = HTTP_TIMEOUT_MS
            connection.readTimeout = HTTP_TIMEOUT_MS
            // TODO(glaznev) - query request origin from pref_room_server_url_key preferences.
            connection.addRequestProperty("origin", HTTP_ORIGIN)
            var doOutput = false
            if (method == "POST") {
                doOutput = true
                connection.doOutput = true
                connection.setFixedLengthStreamingMode(postData.size)
            }
            if (contentType == null) {
                connection.setRequestProperty("Content-Type", "text/plain; charset=utf-8")
            } else {
                connection.setRequestProperty("Content-Type", contentType)
            }

            // Send POST request.
            if (doOutput && postData.size > 0) {
                val outStream = connection.outputStream
                outStream.write(postData)
                outStream.close()
            }

            // Get response.
            val responseCode = connection.responseCode
            if (responseCode != 200) {
                events.onHttpError("Non-200 response to " + method + " to URL: " + url + " : "
                        + connection.getHeaderField(null))
                connection.disconnect()
                return
            }
            val responseStream = connection.inputStream
            val response = drainStream(responseStream)
            responseStream.close()
            connection.disconnect()
            events.onHttpComplete(response)
        } catch (e: SocketTimeoutException) {
            events.onHttpError("HTTP $method to $url timeout")
        } catch (e: IOException) {
            events.onHttpError("HTTP " + method + " to " + url + " error: " + e.message)
        }

    }

    companion object {
        private val HTTP_TIMEOUT_MS = 8000
        private val HTTP_ORIGIN = "https://appr.tc"

        // Return the contents of an InputStream as a String.
        private fun drainStream(`in`: InputStream): String {
            val s = Scanner(`in`, "UTF-8").useDelimiter("\\A")
            return if (s.hasNext()) s.next() else ""
        }
    }
}
