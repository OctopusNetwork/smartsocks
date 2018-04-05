package com.kkt.cryptor

import java.io.ByteArrayOutputStream

/**
 * Created by owen on 18-4-5.
 */
class NoneCryptor : ICrypt {
    override fun encrypt(data: ByteArray): ByteArray {
        return data
    }

    override fun decrypt(data: ByteArray): ByteArray {
        return data
    }

    override fun encrypt(data: ByteArray, stream: ByteArrayOutputStream) {

    }

    override fun encrypt(data: ByteArray, length: Int, stream: ByteArrayOutputStream) {

    }

    override fun decrypt(data: ByteArray, stream: ByteArrayOutputStream) {

    }

    override fun decrypt(data: ByteArray, length: Int, stream: ByteArrayOutputStream) {

    }

    override fun getIVLength(): Int {
        return 16
    }

    override fun getKeyLength(): Int {
        return 16
    }
}