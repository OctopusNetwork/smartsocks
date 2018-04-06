package com.kkt.sstunnel.socks5

import com.kkt.crypto.ICrypt
import java.net.InetSocketAddress
import java.nio.ByteBuffer

/**
 * Created by owen on 18-4-5.
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