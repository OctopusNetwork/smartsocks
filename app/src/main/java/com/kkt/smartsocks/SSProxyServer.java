package com.kkt.smartsocks;

import android.content.Context;
import java.io.File;
import java.util.ArrayList;

/**
 * Created by owen on 18-3-5.
 */
public class SSProxyServer {
    private Context mContext = null;
    private Thread mServerThread;

    private final String SS_SERVER_EXEC = "ss-server";
    private ArrayList<String> SS_SERVER_CMD = new ArrayList<String>();

    public SSProxyServer(Context context) {
        mContext = context;
        SS_SERVER_CMD.add(SS_SERVER_EXEC);
        SS_SERVER_CMD.add("-s");
        SS_SERVER_CMD.add("0.0.0.0");
        SS_SERVER_CMD.add("-p");
        SS_SERVER_CMD.add("10993");
        SS_SERVER_CMD.add("-k");
        SS_SERVER_CMD.add("1qaz2wsx");
        SS_SERVER_CMD.add("-m");
        SS_SERVER_CMD.add("aes-256-cfb");
        SS_SERVER_CMD.add("-l");
        SS_SERVER_CMD.add("10999");

        System.loadLibrary(SS_SERVER_EXEC);
    }

    public void start() {
        mServerThread = new Thread() {
            @Override
            public void run() {
                SSProxyServerStart(SS_SERVER_CMD.size(), SS_SERVER_CMD);
            }
        };

        mServerThread.start();
    }

    public void stop() {
        SSProxyServerStop();
        try {
            mServerThread.join();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    private native int SSProxyServerStart(int argc, ArrayList<String> argv);
    private native int SSProxyServerStop();
}