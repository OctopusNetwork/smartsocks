package com.kkt.dns

import com.kkt.utils.SSLocalLogging
import java.nio.ByteBuffer

/**
 * Created by owen on 18-4-5.
 */
class Resource {
    var Domain: String? = null
    var Type: Short = 0
    var Class: Short = 0
    var TTL: Int = 0
    var DataLength: Short = 0
    var Data: ByteArray? = null

    private var offset: Int = 0

    fun Offset(): Int {
        return offset
    }

    private var length: Int = 0

    fun Length(): Int {
        return length
    }

    companion object {
        val TAG = "Resource"

        fun FromBytes(buffer: ByteBuffer): Resource {
            val r = Resource()
            r.offset = buffer.arrayOffset() + buffer.position()
            r.Domain = DNSPacket.ReadDomain(buffer, buffer.arrayOffset())
            r.Type = buffer.short
            r.Class = buffer.short
            r.TTL = buffer.int
            r.DataLength = buffer.short
            r.Data = ByteArray((r.DataLength.toInt() and 0xFFFF))
            buffer.get(r.Data)
            r.length = buffer.arrayOffset() + buffer.position() - r.offset
            return r
        }
    }

    fun ToBytes(buffer: ByteBuffer) {
        if (this.Data == null) {
            this.Data = ByteArray(0)
        }
        this.DataLength = this.Data!!.size.toShort()

        this.offset = buffer.position()
        DNSPacket.WriteDomain(this.Domain, buffer)
        buffer.putShort(this.Type)
        buffer.putShort(this.Class)
        buffer.putInt(this.TTL)

        buffer.putShort(this.DataLength)
        buffer.put(this.Data)
        this.length = buffer.position() - this.offset
    }
}