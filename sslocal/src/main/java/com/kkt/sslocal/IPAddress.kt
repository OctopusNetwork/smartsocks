package com.kkt.sslocal

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
        val arrStrings = mAddress.split("\\.".toRegex()).dropLastWhile { it.isEmpty() }.toTypedArray()
        return (Integer.parseInt(arrStrings[0]) shl 24
                or (Integer.parseInt(arrStrings[1]) shl 16)
                or (Integer.parseInt(arrStrings[2]) shl 8)
                or Integer.parseInt(arrStrings[3]))
    }
}