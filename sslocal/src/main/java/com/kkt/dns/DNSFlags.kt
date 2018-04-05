package com.kkt.dns

import kotlin.experimental.and

/**
 * Created by owen on 18-4-5.
 */
class DNSFlags {
    var QR: Boolean = false//1 bits
    var OpCode: Int = 0//4 bits
    var AA: Boolean = false//1 bits
    var TC: Boolean = false//1 bits
    var RD: Boolean = false//1 bits
    var RA: Boolean = false//1 bits
    var Zero: Int = 0//3 bits
    var Rcode: Int = 0//4 bits

    fun ToShort(): Short {
        var mFlags = 0
        mFlags = mFlags or ((if (this.QR) 1 else 0) shl 7)
        mFlags = mFlags or (this.OpCode and 0x0F shl 3)
        mFlags = mFlags or ((if (this.AA) 1 else 0) shl 2)
        mFlags = mFlags or ((if (this.TC) 1 else 0) shl 1)
        mFlags = mFlags or if (this.RD) 1 else 0
        mFlags = mFlags or ((if (this.RA) 1 else 0) shl 15)
        mFlags = mFlags or (this.Zero and 0x07 shl 12)
        mFlags = mFlags or (this.Rcode and 0x0F shl 8)
        return mFlags.toShort()
    }

    companion object {

        fun Parse(value: Short): DNSFlags {
            val mFlags = value.toInt() and 0xFFFF
            val flags = DNSFlags()
            flags.QR = mFlags shr 7 and 0x01 == 1
            flags.OpCode = mFlags shr 3 and 0x0F
            flags.AA = mFlags shr 2 and 0x01 == 1
            flags.TC = mFlags shr 1 and 0x01 == 1
            flags.RD = mFlags and 0x01 == 1
            flags.RA = mFlags shr 15 == 1
            flags.Zero = mFlags shr 12 and 0x07
            flags.Rcode = mFlags shr 8 and 0xF
            return flags
        }
    }
}
