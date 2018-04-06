package com.kkt.sstunnel

import java.nio.channels.Selector
import java.nio.channels.SocketChannel

/**
 * Created by owen on 18-4-5.
 */
class LocalTunnel(socketChannel: SocketChannel, selector: Selector) :
        SocketChannelTunnel(socketChannel, TunnelRole.TUNNEL_ROLE_LOCAL, selector) {

    /**
     * Special Local tunnel to accept local access and write to remote tunnel
     * And read remote tunnel return data then write to local request
     */
}