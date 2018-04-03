package com.kkt.smartsocks

import android.os.Bundle
import android.support.design.widget.Snackbar
import android.support.v7.app.AppCompatActivity
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.widget.AdapterView
import com.kkt.rtc.RtcEngine
import com.kkt.rtc.RtcSignalling
import kotlinx.android.synthetic.main.activity_main.*
import kotlinx.android.synthetic.main.content_main.*

class MainActivity : AppCompatActivity() {

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
        RtcEngine.initialize(RtcEngine.RtcEngineInitConfig(
                this, "192.168.199.199", 8089),
            object : RtcEngine.RtcSignallingEventListener {
                override fun onDelPeer(peer: RtcSignalling.RtcPeer) {

                }

                override fun onAddPeer(peer: RtcSignalling.RtcPeer) {

                }

                override fun onPeerListUpdate(peerMap: HashMap<String, RtcSignalling.RtcPeer>) {
                    runOnUiThread { peerListAdapter.updatePeerList(peerMap) }
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
}
