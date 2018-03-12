package com.kkt.smartsocks.rtc

/**
 * Created by owen on 18-3-4.
 */
class HttpParser {
    companion object {
        fun isHttpResponse(response: String): Boolean {
            return response.startsWith("HTTP/")
        }

        fun shouldBeClose(response: String): Boolean {
            val respStrs = response.split("\r\n")

            for (str in respStrs) {
                if (str.contains("Connection:")) {
                    return str.contains("close")
                }
            }

            return false
        }

        fun parsePragmaId(response: String): Long {
            val respStrs = response.split("\r\n")

            for (str in respStrs) {
                if (str.contains("Pragma")) {
                    return str.substring("Pragma: ".length).toLong()
                }
            }

            return -1
        }

        fun getBody(response: String): String? {
            val respStrs = response.split("\r\n")
            var contentLen: Long = 0

            for (str in respStrs) {
                if (str.contains("Content-Length: ")) {
                    contentLen = str.substring("Content-Length: ".length).toLong()
                    break
                }
            }

            val bodyLen: Int = (response.length - contentLen).toInt()
            val body = response.substring(bodyLen)

            return body
        }
    }
}