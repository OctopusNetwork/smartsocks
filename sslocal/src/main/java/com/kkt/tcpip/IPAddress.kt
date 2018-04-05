package com.kkt.tcpip

import com.kkt.utils.EasyValue
import java.net.Inet4Address
import java.net.InetAddress
import java.net.UnknownHostException

/**
 * Created by owen on 18-4-4.
 */
class IPAddress(address: String, prefixLength: Int = 32) {
    val mAddress: String = address
    val mPrefixLength: Int = prefixLength

    override fun toString(): String {
        return String.format("%s/%d", mAddress, mPrefixLength)
    }

    override fun equals(o: Any?): Boolean {
        return if (o == null) {
            false
        } else {
            this.toString() == o.toString()
        }
    }

    fun toInt(): Int {
        val arrStrings = mAddress.split(
                "\\.".toRegex()).dropLastWhile { it.isEmpty() }
                .toTypedArray()
        return (Integer.parseInt(arrStrings[0]) shl 24
                or (Integer.parseInt(arrStrings[1]) shl 16)
                or (Integer.parseInt(arrStrings[2]) shl 8)
                or Integer.parseInt(arrStrings[3]))
    }

    companion object {
        fun hexIpToString(ip: Int): String {
            return String.format("%s.%s.%s.%s",
                    ip shr 24 and 0x00FF,
                    ip shr 16 and 0x00FF,
                    ip shr 8 and 0x00FF,
                    ip and 0x00FF)
        }

        fun hexIpToInet4Address(ip: Int): InetAddress {
            val ipAddress = ByteArray(4)
            EasyValue.writeInt(ipAddress, 0, ip)
            return try {
                Inet4Address.getByAddress(ipAddress)
            } catch (e: UnknownHostException) {
                e.printStackTrace()
                throw e
            }
        }
    }
}