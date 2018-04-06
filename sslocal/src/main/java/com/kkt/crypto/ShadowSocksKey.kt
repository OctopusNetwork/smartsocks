package com.kkt.crypto

import java.io.UnsupportedEncodingException
import java.security.MessageDigest
import java.util.logging.Logger
import javax.crypto.SecretKey

/**
 * Created by owen on 18-4-6.
 */
class ShadowSocksKey(password: String, length: Int) : SecretKey {
    private val logger = Logger.getLogger(ShadowSocksKey::class.java.name)
    private val KEY_LENGTH = 32
    private var _key: ByteArray? = null
    private var _length: Int = 0

    init {
        _length = length
        _key = init(password)
    }

    private fun init(password: String): ByteArray? {
        var md: MessageDigest? = null
        val keys = ByteArray(KEY_LENGTH)
        var temp: ByteArray? = null
        var hash: ByteArray? = null
        var passwordBytes: ByteArray? = null
        var i = 0

        try {
            md = MessageDigest.getInstance("MD5")
            passwordBytes = password.toByteArray(charset("UTF-8"))
        } catch (e: UnsupportedEncodingException) {
            logger.info("ShadowSocksKey: Unsupported string encoding")
        } catch (e: Exception) {
            return null
        }

        while (i < keys.size) {
            if (i == 0) {
                hash = md!!.digest(passwordBytes)
                temp = ByteArray(passwordBytes!!.size + hash!!.size)
            } else {
                System.arraycopy(hash!!, 0, temp!!, 0, hash.size)
                System.arraycopy(passwordBytes!!, 0, temp, hash.size, passwordBytes.size)
                hash = md!!.digest(temp)
            }
            System.arraycopy(hash!!, 0, keys, i, hash.size)
            i += hash.size
        }

        if (_length != KEY_LENGTH) {
            val keysl = ByteArray(_length)
            System.arraycopy(keys, 0, keysl, 0, _length)
            return keysl
        }
        return keys
    }

    override fun getAlgorithm(): String {
        return "shadowsocks"
    }

    override fun getFormat(): String {
        return "RAW"
    }

    override fun getEncoded(): ByteArray? {
        return _key
    }
}