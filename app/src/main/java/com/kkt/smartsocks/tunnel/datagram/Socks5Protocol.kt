package com.kkt.smartsocks.tunnel.datagram

import android.util.Log
import com.kkt.smartsocks.rtc.Utils
import com.kkt.smartsocks.tunnel.shadowsocks.ICrypt
import java.net.InetSocketAddress
import java.nio.ByteBuffer

/**
 * Created by owen on 18-3-9.
 */
class Socks5Protocol {
    companion object {
        fun handshake(destAddress: InetSocketAddress, cryptor: ICrypt): ByteBuffer {
            val encryptor: ICrypt = cryptor
            val buffer = ByteBuffer.allocate(1024)

            buffer.clear()
            // https://shadowsocks.org/en/spec/protocol.html

            buffer.put(0x03.toByte())//domain
            val domainBytes = destAddress.hostName.toByteArray()
            buffer.put(domainBytes.size.toByte())//domain length;
            buffer.put(domainBytes)
            buffer.putShort(destAddress.port.toShort())
            buffer.flip()

            val _header = ByteArray(buffer.limit())
            buffer.get(_header)

            buffer.clear()
            buffer.put(encryptor.encrypt(_header))
            buffer.flip()

            return buffer
        }
    }
}