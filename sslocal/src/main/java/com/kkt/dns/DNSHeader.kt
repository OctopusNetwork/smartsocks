package com.kkt.dns

import com.kkt.utils.EasyValue
import java.nio.ByteBuffer

/**
 * Created by owen on 18-4-5.
 */
class DNSHeader(data: ByteArray, offset: Int) {
    val mData = data
    val mOffset = offset

    var mID: Short
        get() = EasyValue.readShort(mData, mOffset + offset_ID)
        set(value) {
            EasyValue.writeShort(mData, mOffset + offset_ID, value)
        }

    var mFlags: Short
        get() = EasyValue.readShort(mData, mOffset + offset_Flags)
        set(value) {
            EasyValue.writeShort(mData, mOffset + offset_Flags, value)
        }

    var mQuestionCount: Short
        get() = EasyValue.readShort(mData, mOffset + offset_QuestionCount)
        set(value) {
            EasyValue.writeShort(mData, mOffset + offset_QuestionCount, value)
        }

    var mResourceCount: Short
        get() = EasyValue.readShort(mData, mOffset + offset_ResourceCount)
        set(value) {
            EasyValue.writeShort(mData, mOffset + offset_ResourceCount, value)
        }

    var mAResourceCount: Short
        get() = EasyValue.readShort(mData, mOffset + offset_AResourceCount)
        set(value) {
            EasyValue.writeShort(mData, mOffset + offset_AResourceCount, value)
        }

    var mEResourceCount: Short
        get() = EasyValue.readShort(mData, mOffset + offset_EResourceCount)
        set(value) {
            EasyValue.writeShort(mData, mOffset + offset_EResourceCount, value)
        }

    var mDNSFlags: DNSFlags? = null

    fun ToBytes(buffer: ByteBuffer) {
        buffer.putShort(this.mID)
        this.mDNSFlags?.ToShort()?.let { buffer.putShort(it) }
        buffer.putShort(this.mQuestionCount)
        buffer.putShort(this.mResourceCount)
        buffer.putShort(this.mAResourceCount)
        buffer.putShort(this.mEResourceCount)
    }

    companion object {

        fun FromBytes(buffer: ByteBuffer): DNSHeader {
            val header = DNSHeader(buffer.array(), buffer.arrayOffset() + buffer.position())
            header.mID = buffer.short
            header.mDNSFlags = DNSFlags.Parse(buffer.short)
            header.mQuestionCount = buffer.short
            header.mResourceCount = buffer.short
            header.mAResourceCount = buffer.short
            header.mEResourceCount = buffer.short
            return header
        }

        internal val offset_ID: Short = 0
        internal val offset_Flags: Short = 2
        internal val offset_QuestionCount: Short = 4
        internal val offset_ResourceCount: Short = 6
        internal val offset_AResourceCount: Short = 8
        internal val offset_EResourceCount: Short = 10
    }
}