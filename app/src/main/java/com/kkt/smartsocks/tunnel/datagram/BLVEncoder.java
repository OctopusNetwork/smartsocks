package com.kkt.smartsocks.tunnel.datagram;

import android.util.Log;

import com.kkt.smartsocks.rtc.Utils;

import java.nio.ByteBuffer;
import java.security.InvalidParameterException;

/**
 * Created by owen on 18-3-9.
 */
class BLVEncoder {
    private static final byte BL_PROTOCOL_ID = (byte)0x8E;

    public static final int BL_INVALID_CONN_ID = -1;

    public static void encodeProxyData(byte cmd, ByteBuffer buffer, int localConnId) {
        byte[] _buffer = new byte[buffer.limit()];

        buffer.get(_buffer);
        buffer.clear();
        buffer.put(BL_PROTOCOL_ID);
        buffer.put(cmd);
        if (BL_INVALID_CONN_ID != localConnId) {
            buffer.putInt(localConnId);
        } else {
            buffer.putInt(0);
        }
        buffer.putInt(_buffer.length);
        Log.d("BLVEncoder", "" + BL_PROTOCOL_ID + "/" +
                cmd + "/" + localConnId + "/" + _buffer.length);
        buffer.put(_buffer);
        buffer.flip();
    }

    public static byte decodeCommand(ByteBuffer buffer) {
        byte protocolId = buffer.get(0);
        if (BL_PROTOCOL_ID != protocolId) {
            throw new InvalidParameterException("Invalid BL Protocol ID");
        }
        return buffer.get(1);
    }

    public static int decodeLocalConnId(ByteBuffer buffer) {
        byte protocolId = buffer.get(0);
        if (BL_PROTOCOL_ID != protocolId) {
            throw new InvalidParameterException("Invalid BL Protocol ID");
        }
        return buffer.getInt(2);
    }

    public static ByteBuffer getBody(ByteBuffer buffer) {
        byte protocolId = buffer.get(0);
        if (BL_PROTOCOL_ID != protocolId) {
            throw new InvalidParameterException("Invalid BL Protocol ID");
        }

        int len = buffer.getInt(6);
        byte[] body = new byte[len];
        buffer.get(body, 0, 10);
        buffer.get(body, 0, len);
        ByteBuffer _buffer = ByteBuffer.wrap(body);
        return _buffer;
    }
}