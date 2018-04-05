package com.kkt.dns

import java.nio.ByteBuffer

/**
 * Created by owen on 18-4-5.
 */
class Question {
    var Domain: String? = null
    var Type: Short = 0
    var Class: Short = 0

    private var offset: Int = 0

    fun Offset(): Int {
        return offset
    }

    private var length: Int = 0

    fun Length(): Int {
        return length
    }

    companion object {
        fun FromBytes(buffer: ByteBuffer): Question {
            val q = Question()
            q.offset = buffer.arrayOffset() + buffer.position()
            q.Domain = DNSPacket.ReadDomain(buffer, buffer.arrayOffset())
            q.Type = buffer.short
            q.Class = buffer.short
            q.length = buffer.arrayOffset() + buffer.position() - q.offset
            return q
        }
    }

    fun ToBytes(buffer: ByteBuffer) {
        this.offset = buffer.position()
        DNSPacket.WriteDomain(this.Domain, buffer)
        buffer.putShort(this.Type)
        buffer.putShort(this.Class)
        this.length = buffer.position() - this.offset
    }
}