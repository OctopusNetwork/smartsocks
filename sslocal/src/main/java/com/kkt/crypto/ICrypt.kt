package com.kkt.crypto

import java.io.ByteArrayOutputStream

/**
 * Created by owen on 18-4-5.
 */
interface ICrypt {
    abstract fun encrypt(data: ByteArray): ByteArray

    abstract fun decrypt(data: ByteArray): ByteArray

    abstract fun encrypt(data: ByteArray, stream: ByteArrayOutputStream)

    abstract fun encrypt(data: ByteArray, length: Int, stream: ByteArrayOutputStream)

    abstract fun decrypt(data: ByteArray, stream: ByteArrayOutputStream)

    abstract fun decrypt(data: ByteArray, length: Int, stream: ByteArrayOutputStream)

    abstract fun getIVLength(): Int

    abstract fun getKeyLength(): Int
}