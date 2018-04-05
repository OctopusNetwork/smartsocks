package com.kkt.smartsocks

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.support.design.widget.Snackbar
import android.support.v7.app.AppCompatActivity
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.widget.AdapterView
import android.widget.Toast
import com.kkt.rtc.*
import com.kkt.sstunnel.SSVpnService
import kotlinx.android.synthetic.main.activity_main.*
import kotlinx.android.synthetic.main.content_main.*
import java.nio.ByteBuffer
import java.nio.CharBuffer
import java.nio.charset.Charset
import java.nio.charset.CharsetDecoder

class MainActivity : AppCompatActivity() {

    val TAG = "MainActivity"

    var mVpnRunning = false

    private fun byteBufferToString(byteBuffer: ByteBuffer) : String? {
        var charset: Charset?
        var decoder: CharsetDecoder?
        var charBuffer: CharBuffer? = null

        try {
            charset = Charset.forName("UTF-8")
            decoder = charset.newDecoder()
            charBuffer = decoder.decode(byteBuffer.asReadOnlyBuffer())
        } catch (ex: Exception) {
            ex.printStackTrace()
        }

        return charBuffer?.toString()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        setSupportActionBar(toolbar)

        fab.setOnClickListener { view ->
            Snackbar.make(view, "Replace with your own action", Snackbar.LENGTH_LONG)
                    .setAction("Action", null).show()
        }

        var peerListAdapter = PeerListAdapter(this, null)
        peer_list.adapter = peerListAdapter
        peer_list.onItemClickListener = object: AdapterView.OnItemClickListener {
            override fun onItemClick(p0: AdapterView<*>?, p1: View?, p2: Int, p3: Long) {
                val peer = peerListAdapter.getItem(p2)
                RtcEngine.createChannel(peer as RtcSignalling.RtcPeer?)
            }
        }

        send_hello_brother.setOnClickListener(object: View.OnClickListener {
            override fun onClick(p0: View?) {
                RtcEngine.broadcast("Hello Brother")
            }
        })

        switch_vpn.setOnClickListener(object: View.OnClickListener {
            override fun onClick(p0: View?) {
                mVpnRunning = !mVpnRunning
                if (mVpnRunning) {
                    SSVpnService.initialize(this@MainActivity,
                            Intent(this@MainActivity, MainActivity::class.java),
                            object: SSVpnService.Companion.SSVpnServiceEventListener {
                                override fun onVpnServiceCrash() {

                                }

                                override fun onVpnServiceStart(vpnSessionName: String) {

                                }
                            })
                } else {
                    SSVpnService.destroy()
                }
            }
        })

        RtcEngine.initialize(RtcEngine.RtcEngineInitConfig(
                this, "192.168.199.199", 8089),
            object : RtcEngine.RtcSignallingEventListener {
                override fun onDelPeer(peer: RtcSignalling.RtcPeer) {
                    runOnUiThread { peerListAdapter.delPeer(peer) }
                }

                override fun onAddPeer(peer: RtcSignalling.RtcPeer) {
                    runOnUiThread { peerListAdapter.addPeer(peer) }
                }

                override fun onPeerListUpdate(peerMap: HashMap<String, RtcSignalling.RtcPeer>) {
                    runOnUiThread { peerListAdapter.updatePeerList(peerMap) }
                }
            }, object: RtcAgentContainer.Companion.RtcAgentCreateChannelListener {
            override fun onRtcAgentCreateChannel(peerId: String) :
                    RtcAgentContainer.Companion.RtcAgentDataChannelMessageListener {
                return object: RtcAgentContainer.Companion.RtcAgentDataChannelMessageListener {
                    override fun onRtcAgentDataChannelMessage(
                            peerId: String,
                            msg: RtcAgentContainer.Companion.RtcAgentDataChannelMessage,
                            info: String?) {
                        RtcLogging.debug(TAG, "State of: " + peerId)
                        if (null != info) {
                            RtcLogging.debug(TAG, info)
                            runOnUiThread { Toast.makeText(this@MainActivity,
                                    "State of $peerId: $info",
                                    Toast.LENGTH_LONG).show() }
                        }
                    }

                    override fun onRtcAgentDataChannelMessage(
                            peerId: String,
                            msg: RtcAgentContainer.Companion.RtcAgentDataChannelMessage,
                            info: ByteBuffer?) {
                        RtcLogging.debug(TAG, "Message from " + peerId)
                        if (null != info) {
                            RtcLogging.debug(TAG, info)
                            runOnUiThread { Toast.makeText(this@MainActivity,
                                    "Message from " + peerId + ": " + byteBufferToString(info),
                                    Toast.LENGTH_LONG).show() }
                        }
                    }
                }
            }
        }, object: RtcAgent.RtcSocketProtectListener {
            override fun onProtectSocket(socket: Int) {
                SSVpnService.protectSocket(socket)
            }
        })
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        // Inflate the menu; this adds items to the action bar if it is present.
        menuInflater.inflate(R.menu.menu_main, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        return when (item.itemId) {
            R.id.action_settings -> true
            else -> super.onOptionsItemSelected(item)
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        RtcEngine.destroy()
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        if (SSVpnService.SS_VPN_SERVICE_REQUEST == requestCode) {
            if (Activity.RESULT_OK == resultCode) {
                SSVpnService.start(this)
            }
        }
    }
}
