package com.kkt.smartsocks.tunnel;

import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.Selector;
import java.nio.channels.SocketChannel;

public class RawTunnel extends Tunnel {

    public RawTunnel(InetSocketAddress serverAddress, Selector selector, int role) throws Exception {
        super(serverAddress, selector, role);
    }

    public RawTunnel(SocketChannel innerChannel, Selector selector, int role) {
        super(innerChannel, selector, role);
        // TODO Auto-generated constructor stub
    }

    @Override
    protected void onConnected(ByteBuffer buffer) throws Exception {
        onTunnelEstablished();
    }

    @Override
    protected void beforeSend(ByteBuffer buffer) throws Exception {
        // TODO Auto-generated method stub

    }

    @Override
    public void afterReceived(ByteBuffer buffer) throws Exception {
        // TODO Auto-generated method stub

    }

    @Override
    protected boolean isTunnelEstablished() {
        return true;
    }

    @Override
    protected void onDispose() {
        // TODO Auto-generated method stub

    }

}
