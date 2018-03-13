package com.kkt.smartsocks.tunnel.datagram;

import android.content.Context;
import android.util.Log;

import com.kkt.smartsocks.rtc.RtcClient;
import com.kkt.smartsocks.rtc.RtcInstance;
import com.kkt.smartsocks.rtc.RtcPeerContainer;
import com.kkt.smartsocks.rtc.Utils;
import com.kkt.smartsocks.core.LocalVpnService;
import com.kkt.smartsocks.tunnel.Tunnel;
import com.kkt.smartsocks.tunnel.shadowsocks.CryptFactory;
import com.kkt.smartsocks.tunnel.shadowsocks.ICrypt;

import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.ClosedChannelException;
import java.nio.channels.Pipe;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.SocketChannel;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.concurrent.Semaphore;

/**
 * Created by owen on 18-3-9.
 */

public class DatagramTunnel extends Tunnel {
    private static final String TAG = "DatagramTunnel";

    private static RtcInstance mRtcInstance;
    private static ArrayList<RtcPeerContainer.RtcPeer> mPeerArray = new ArrayList<>();
    private static Boolean mPeerConnected = false;
    private static Boolean mSendDataChannelReady = false;

    private static final byte PROXY_REQ_DATA = (byte)0xA1;
    private static final byte PROXY_RESP_DATA = (byte)0xA2;
    private static final byte PROXY_AUTH = (byte)0xA3;
    private static final byte PROXY_CONNECT_DONE = (byte)0xA4;
    private static final byte PROXY_DISPOSE = (byte)0xA5;

    private static Selector mSelector;
    private static Semaphore mSemaphore = new Semaphore(1, true);
    private static Boolean mRunning = false;

    private Boolean mDisposed = false;

    private ICrypt mCryptor;

    public interface OnDatagramTunnelOpenListener {
        void onDatagramTunnelOpen();
    }

    private static OnDatagramTunnelOpenListener mOnDatagramTunnelOpenListener = null;

    public static class PendingServerBuffer {
        public ByteBuffer mBuffer;
        public SocketChannel mChannel;
        public int mSenderConnId;

        public PendingServerBuffer(ByteBuffer body, SocketChannel channel, int senderConnId) {
            mBuffer = body;
            mChannel = channel;
            mSenderConnId = senderConnId;
        }
    }

    public static class PendingLocalBuffer {
        public ByteBuffer mBuffer;
        public Tunnel mLocalTunnel;
        public Tunnel mRemoteTunnel;

        public PendingLocalBuffer(ByteBuffer body, Tunnel localTunnel, Tunnel remoteTunnel) {
            mBuffer = body;
            mLocalTunnel = localTunnel;
            mRemoteTunnel = remoteTunnel;
        }
    }

    private static void remoteRemoteTunnel(Integer remoteSenderId) {
        for (RemoteTunnel tunnel: mRemoteTunnelList) {
            if (remoteSenderId == tunnel.mSenderRemoteConnId) {
                mRemoteTunnelList.remove(tunnel);
                break;
            }
        }
    }

    private static Thread mSocksAdapterThread = new Thread() {
        @Override
        public void run() {
            do {
                try {
                    mSelector.select(10);
                    Iterator<SelectionKey> keyIterator = mSelector.selectedKeys().iterator();
                    while (keyIterator.hasNext()) {
                        SelectionKey key = keyIterator.next();
                        if (key.isValid()) {
                            try {
                                if (key.isReadable()) {
                                    // Socks server resp
                                    PendingServerBuffer conn = (PendingServerBuffer) key.attachment();
                                    if (conn.mChannel.isConnected()) {
                                        ByteBuffer buffer = ByteBuffer.allocate(900);
                                        buffer.clear();
                                        int bytesRead = conn.mChannel.read(buffer);
                                        if (bytesRead > 0) {
                                            buffer.flip();
                                            ByteBuffer _buffer = ByteBuffer.allocate(buffer.limit() + 100);
                                            _buffer.clear();
                                            _buffer.put(buffer);
                                            _buffer.flip();
                                            BLVEncoder.encodeProxyData(PROXY_RESP_DATA, _buffer, conn.mSenderConnId);
                                            Log.d(TAG, "Resp to: " + conn.mSenderConnId);
                                            mRtcInstance.sendBinary(_buffer);
                                        } else if (bytesRead < 0) {
                                            Log.d(TAG, "Error to: " + conn.mSenderConnId);
                                            conn.mChannel.close();
                                            remoteRemoteTunnel(conn.mSenderConnId);
                                        }
                                    }
                                } else if (key.isWritable()) {
                                    // Write to socks server
                                    Log.d(TAG, "Write data to VPN server");
                                } else if (key.isConnectable()) {
                                    // Socket channel connect to socks server done
                                    PendingServerBuffer conn = (PendingServerBuffer) key.attachment();
                                    if (conn.mChannel.finishConnect()) {
                                        Log.d(TAG, "Send hand shake to socks server: " + conn.mSenderConnId);
                                        mRemoteTunnelList.add(new RemoteTunnel(conn.mSenderConnId, conn.mChannel));
                                        conn.mChannel.write(conn.mBuffer);
                                        conn.mChannel.register(mSelector, SelectionKey.OP_READ,
                                                new PendingServerBuffer(null,
                                                        conn.mChannel, conn.mSenderConnId));
                                        ByteBuffer _buffer = ByteBuffer.allocate(100);
                                        _buffer.clear();
                                        _buffer.put(PROXY_CONNECT_DONE);
                                        _buffer.flip();
                                        BLVEncoder.encodeProxyData(PROXY_CONNECT_DONE, _buffer, conn.mSenderConnId);
                                        Log.d(TAG, "Resp connect to: " + conn.mSenderConnId);
                                        mRtcInstance.sendBinary(_buffer);
                                    } else {
                                        // Fail to connect
                                        conn.mChannel.close();
                                        remoteRemoteTunnel(conn.mSenderConnId);
                                        Log.e(TAG, "Fail to connect socks server " + conn.mSenderConnId);
                                    }
                                }
                            } catch (Exception e) {
                                System.out.println(e.toString());
                            }
                        }
                        keyIterator.remove();
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                }
            } while (mRunning);
        }
    };

    class LocalTunnel {
        public Tunnel mLocalTunnel;
        public Tunnel mRemoteTunnel;

        public LocalTunnel(Tunnel localTunnel, Tunnel remoteTunnel) {
            mLocalTunnel = localTunnel;
            mRemoteTunnel = remoteTunnel;
            Log.d(TAG, "local " + mLocalTunnel.m_InnerChannel +
                    "/remote " + mRemoteTunnel.m_InnerChannel +
                    "/me " + DatagramTunnel.this);
        }
    }

    private static ArrayList<LocalTunnel> mLocalTunnelList = new ArrayList<> ();

    static class RemoteTunnel {
        // Conn id from smartsocks client
        public int mSenderRemoteConnId;

        public SocketChannel mSockChannel;

        public RemoteTunnel(int remoteConnId, SocketChannel channel) {
            mSenderRemoteConnId = remoteConnId;
            mSockChannel = channel;
        }
    }

    private static ArrayList<RemoteTunnel> mRemoteTunnelList = new ArrayList<>();

    public static void createPeerConnection(OnDatagramTunnelOpenListener listener) {
        mOnDatagramTunnelOpenListener = listener;
        if (!mPeerConnected) {
            if (mPeerArray.size() > 0) {
                RtcPeerContainer.RtcPeer peer = mPeerArray.get(0);
                if (peer.getConnected()) {
                    Log.d(TAG, "Connect to peer: " + peer.getName() + "/" + peer.getId());
                    mRtcInstance.connectToPeer(peer.getId());
                    mRtcInstance.createDataChannel("DatagramChannel");
                    mPeerConnected = true;
                }
            }
        }
    }

    public static void destroyPeerConnection() {
        mRtcInstance.destroyDataChannel();
        mRtcInstance.destroyPeerConnection();
    }

    private static RtcClient.RtcDataChannelListener mSendDataChannelListener =
            new RtcClient.RtcDataChannelListener() {
        @Override
        public void onMessage(@NotNull ByteBuffer byteBuffer) {
        }

        @Override
        public void onStateChange(@NotNull String state) {
            Log.d(TAG, "Send data channel: " + state);
            if ("OPEN" == state) {
                mSendDataChannelReady = true;
                if (null != mOnDatagramTunnelOpenListener) {
                    mOnDatagramTunnelOpenListener.onDatagramTunnelOpen();
                }
            }
        }
    };

    private static RtcClient.RtcDataChannelListener mRecvDataChannelListener =
            new RtcClient.RtcDataChannelListener() {
                @Override
                public void onStateChange(@NotNull String state) {
                    Log.d(TAG, "Recv data channel: " + state);
                }

                @Override
                public void onMessage(@NotNull ByteBuffer byteBuffer) {
                    Log.d(TAG, "Message recv: ");
                    processRemoteData(byteBuffer);
                }
            };

    public DatagramTunnel(int role) throws Exception {
        super(mRtcInstance, role);
        if (null == mRtcInstance) throw new NullPointerException();

        mCryptor = CryptFactory.get("aes-256-cfb", "1qaz2wsx");
    }

    public Boolean isPeerConnected() {
        return true;
    }

    private static void processProxyRespData(ByteBuffer buffer) {
        int senderConnId = BLVEncoder.decodeLocalConnId(buffer);
        ByteBuffer body = BLVEncoder.getBody(buffer);

        Log.d(TAG, "Resp to: " + senderConnId);

        for (LocalTunnel localTunnel : mLocalTunnelList) {
            if (senderConnId == localTunnel.mLocalTunnel.m_InnerChannel.socket().getPort()) {
                try {
                    Log.d(TAG, "Resp to: " + senderConnId +
                            "/" + localTunnel.mLocalTunnel.m_InnerChannel.socket().getPort() +
                            "/" + localTunnel.mLocalTunnel.m_InnerChannel.socket().getLocalPort());
                    localTunnel.mRemoteTunnel.afterReceived(body);
                    localTunnel.mLocalTunnel.m_InnerChannel.write(body);
                } catch (ClosedChannelException e) {
                    e.printStackTrace();
                } catch (Exception e) {
                    e.printStackTrace();
                }
                break;
            }
        }
    }

    private static boolean processProxyReqData(ByteBuffer buffer) {
        int senderConnId = BLVEncoder.decodeLocalConnId(buffer);
        ByteBuffer body = BLVEncoder.getBody(buffer);

        Log.d(TAG, "Proxy req for " + senderConnId);

        for (RemoteTunnel tunnel: mRemoteTunnelList) {
            if (senderConnId == tunnel.mSenderRemoteConnId) {
                Log.d(TAG, "Connection " + senderConnId +
                        " already been connected, write to " +
                        tunnel.mSockChannel.socket().getPort() +
                        "/" + body.limit());

                if (tunnel.mSockChannel.isConnected()) {
                    // Should process part write
                    try {
                        tunnel.mSockChannel.write(body);
                    } catch (IOException e) {
                        try {
                            tunnel.mSockChannel.close();
                            mRemoteTunnelList.remove(tunnel);
                        } catch (IOException e1) {
                            e1.printStackTrace();
                        }
                        e.printStackTrace();
                    }
                }
                return true;
            }
        }

        try {
            SocketChannel channel = SocketChannel.open();
            LocalVpnService.protectSocket(channel.socket());
            channel.configureBlocking(false);
            Log.d(TAG, "Register connect event for " + senderConnId);
            channel.register(mSelector, SelectionKey.OP_CONNECT,
                    new PendingServerBuffer(body, channel, senderConnId));
            channel.connect(new InetSocketAddress("localhost", 10993));
            Log.d(TAG, "Connect to VPN server");
            return true;
        } catch (IOException e) {
            e.printStackTrace();
        }

        return true;
    }

    private static boolean processRespConnectDone(ByteBuffer buffer) {
        int senderConnId = BLVEncoder.decodeLocalConnId(buffer);

        Log.d(TAG, "Resp connect to: " + senderConnId);

        for (LocalTunnel localTunnel : mLocalTunnelList) {
            if (senderConnId == localTunnel.mLocalTunnel.m_InnerChannel.socket().getPort()) {
                try {
                    localTunnel.mLocalTunnel.beginReceive();
                } catch (Exception e) {
                    e.printStackTrace();
                }
                break;
            }
        }

        return true;
    }

    private static boolean processRespDispose(ByteBuffer buffer) {
        int senderConnId = BLVEncoder.decodeLocalConnId(buffer);

        Log.d(TAG, "Dispose for: " + senderConnId);

        for (RemoteTunnel remoteTunnel : mRemoteTunnelList) {
            if (senderConnId == remoteTunnel.mSenderRemoteConnId) {
                try {
                    remoteTunnel.mSockChannel.close();
                } catch (ClosedChannelException e) {
                    e.printStackTrace();
                } catch (IOException e) {
                    e.printStackTrace();
                }
                break;
            }
        }

        return true;
    }

    private static void processRemoteData(ByteBuffer buffer) {
        try {
            byte command = BLVEncoder.decodeCommand(buffer);

            Log.d(TAG, "Remote command: " + Integer.toHexString(command));

            switch (command) {
                case PROXY_REQ_DATA:
                    processProxyReqData(buffer);
                    mSelector.wakeup();
                    break;
                case PROXY_RESP_DATA:
                    processProxyRespData(buffer);
                    break;
                case PROXY_AUTH:
                    break;
                case PROXY_CONNECT_DONE:
                    processRespConnectDone(buffer);
                    break;
                case PROXY_DISPOSE:
                    processRespDispose(buffer);
                    break;
                default:
                    break;
            }

        } catch (Exception excp) {
            excp.printStackTrace();
        }
    }

    public static void initGlobal(Context context) {
        mRunning = true;

        mRtcInstance = new RtcInstance(context, new RtcPeerContainer.RtcPeerListListener() {
            @Override
            public void onUpdated(@NotNull ArrayList<RtcPeerContainer.RtcPeer> peerList) {
                mPeerArray.clear();
                mPeerArray.addAll(peerList);
            }
        });
        mRtcInstance.setSendDataChannelListener(mSendDataChannelListener);
        mRtcInstance.setRecvDataChannelListener(mRecvDataChannelListener);
        mRtcInstance.initialize();

        try {
            mSelector = Selector.open();
        } catch (IOException e) {
            e.printStackTrace();
        }
        mSocksAdapterThread.start();
    }

    public static void finalGlobal() {
        mRtcInstance.release();
        mRunning = false;
        mSelector.wakeup();
        try {
            mSocksAdapterThread.join();
            mSelector.close();
        } catch (InterruptedException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void connect(InetSocketAddress destAddress) {
        try {
            mSemaphore.acquire();
            mLocalTunnelList.add(new LocalTunnel(m_BrotherTunnel, this));
            mSemaphore.release();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        ByteBuffer buffer = Socks5Protocol.Companion.handshake(destAddress, mCryptor);
        BLVEncoder.encodeProxyData(PROXY_REQ_DATA, buffer,
                m_BrotherTunnel.m_InnerChannel.socket().getPort());
        Log.d(TAG, "Prepare to connect: " + mSendDataChannelReady);
        m_DestAddress = destAddress;
        if (mSendDataChannelReady) {
            Log.d(TAG, "Connect send for " + destAddress.toString() +
                    " at " + m_BrotherTunnel.m_InnerChannel.socket().getPort());
            mRtcInstance.sendBinary(buffer);
        }
    }

    @Override
    public boolean write(ByteBuffer buffer, boolean copyRemainData) throws Exception {
        Log.d(TAG, "Write to data channel for " +
                m_BrotherTunnel.m_InnerChannel.socket().getPort() +
                "/" + buffer.limit());
        ByteBuffer _buffer = ByteBuffer.allocate(buffer.limit() + 100);
        _buffer.clear();
        _buffer.put(buffer);
        _buffer.flip();
        BLVEncoder.encodeProxyData(PROXY_REQ_DATA, _buffer,
                m_BrotherTunnel.m_InnerChannel.socket().getPort());
        mRtcInstance.sendBinary(_buffer);
        return true;
    }

    @Override
    public void onReadable(SelectionKey key) {

    }

    @Override
    public void onWritable(SelectionKey key) {

    }

    @Override
    public void beginReceive() throws Exception {

    }

    @Override
    protected void onConnected(ByteBuffer buffer) throws Exception {

    }

    @Override
    protected boolean isTunnelEstablished() {
        return true;
    }

    @Override
    protected void beforeSend(ByteBuffer buffer) throws Exception {
        Log.d(TAG, "Data encryption needed");
        byte[] bytes = new byte[buffer.limit()];
        buffer.get(bytes);

        byte[] newbytes = mCryptor.encrypt(bytes);

        buffer.clear();
        buffer.put(newbytes);
        buffer.flip();
    }

    @Override
    public void afterReceived(ByteBuffer buffer) throws Exception {
        Log.d(TAG, "Data decryption needed");
        byte[] bytes = new byte[buffer.limit()];
        buffer.get(bytes);
        byte[] newbytes = mCryptor.decrypt(bytes);
        buffer.clear();
        buffer.put(newbytes);
        buffer.flip();
    }

    @Override
    protected void onDispose() {
        Log.d(TAG, "Dispose channel for " + m_DestAddress.toString());
        try {
            mSemaphore.acquire();
            Iterator<LocalTunnel> localTunnelIterator = mLocalTunnelList.iterator();
            while(localTunnelIterator.hasNext()){
                LocalTunnel localTunnel = localTunnelIterator.next();
                if(localTunnel.mLocalTunnel.m_InnerChannel.socket().getPort() ==
                        this.m_BrotherTunnel.m_InnerChannel.socket().getPort()){
                    localTunnelIterator.remove();
                }
            }
            mSemaphore.release();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        // Also we need to remove remote channel
        ByteBuffer buffer = ByteBuffer.allocate(100);
        buffer.clear();
        buffer.put(PROXY_DISPOSE);
        buffer.flip();
        BLVEncoder.encodeProxyData(PROXY_DISPOSE, buffer,
                m_BrotherTunnel.m_InnerChannel.socket().getPort());
        mRtcInstance.sendBinary(buffer);
    }

    @Override
    public void disposeInternal(boolean disposeBrother) {
        if (mDisposed) {
            return;
        } else {
            m_InnerChannel = null;
            mDisposed = true;
            SessionCount--;

            onDispose();

            if (m_BrotherTunnel != null && disposeBrother) {
                m_BrotherTunnel.disposeInternal(false);//把兄弟的资源也释放了。
            }

            m_BrotherTunnel = null;
        }
    }
}
