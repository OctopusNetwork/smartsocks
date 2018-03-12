package com.kkt.smartsocks.rtc;

import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.widget.Button;

import com.kkt.smartsocks.R;
import com.kkt.smartsocks.tunnel.datagram.DatagramTunnel;

import java.nio.ByteBuffer;

/**
 * Created by owen on 18-3-11.
 */

public class DTMainActivity extends AppCompatActivity {
    private Boolean mRtcRunning = false;
    private Boolean mRtcConnected = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_dt_main);

        final Button btn = (Button) findViewById(R.id.button);
        btn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                mRtcRunning = !mRtcRunning;
                if (!mRtcRunning) {
                    DatagramTunnel.finalGlobal();
                    btn.setText("DTInit: CLOSE");
                } else {
                    DatagramTunnel.initGlobal(DTMainActivity.this);
                    btn.setText("DTInit: OPEN");
                }
            }
        });

        final Button btn2 = (Button) findViewById(R.id.button2);
        btn2.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                mRtcConnected = !mRtcConnected;
                if (!mRtcConnected) {
                    btn2.setText("DTConn: CLOSE");
                } else {
                    DatagramTunnel.createPeerConnection(new DatagramTunnel.OnDatagramTunnelOpenListener() {
                        @Override
                        public void onDatagramTunnelOpen() {
                            Log.d("DTMain", "Datagram tunnel is opened");
                            String str = "Hello Brother";
                            try {
                                new DatagramTunnel(DatagramTunnel.TUNNEL_ROLE_LOCAL)
                                        .write(ByteBuffer.wrap(str.getBytes()), false);
                            } catch (Exception e) {
                                e.printStackTrace();
                            }
                        }
                    });
                    btn2.setText("DTConn: OPEN");
                }
            }
        });
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
    }
}
