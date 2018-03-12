package com.kkt.smartsocks.rtc

import android.content.Context
import android.os.Bundle
import android.os.Handler
import android.os.Message
import android.support.v7.app.AppCompatActivity
import android.util.Log
import android.view.View
import android.view.ViewGroup
import android.widget.AdapterView
import android.widget.BaseAdapter
import android.widget.TextView
import com.kkt.smartsocks.R
import com.kkt.smartsocks.ui.MainActivity
import kotlinx.android.synthetic.main.activity_rtc_main.*
import java.nio.ByteBuffer

class RtcMainActivity : AppCompatActivity() {

    var mPeerList: ArrayList<RtcPeerContainer.RtcPeer>? = null
    var mPeerListAdapter: PeerListAdapter? = null
    var mRtcPeerListListener: RtcPeerListListener? = null

    open var mRtcInstance: RtcInstance? = null

    var mSSProxyServer: SSProxyServer? = null

    open var mRunning: Boolean = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_rtc_main)

        mPeerListAdapter = PeerListAdapter(this, mPeerList)
        mRtcPeerListListener = RtcPeerListListener(this, mHandler)

        mRunning = true

        mRtcInstance = RtcInstance(this, mRtcPeerListListener!!)
        mRtcInstance?.setSendDataChannelListener(object: RtcClient.RtcDataChannelListener{
            override fun onMessage(byteBuffer: ByteBuffer) {
            }

            override fun onStateChange(state: String) {
                Log.d("XXXX", "Send data channel: " + state)
                if ("OPEN" == state) {
                    Thread {
                        do {
                            mRtcInstance?.sendString("Hello Brother")
                            Thread.sleep(100)
                        } while (mRunning)
                    }.start()
                }
            }

        })
        mRtcInstance?.setRecvDataChannelListener(object: RtcClient.RtcDataChannelListener{
            override fun onMessage(byteBuffer: ByteBuffer) {
                Utils.log(byteBuffer)
            }

            override fun onStateChange(state: String) {
                Log.d("XXXX", "Recv data channel: " + state)
            }

        })
        mRtcInstance?.initialize()

        peer_list.adapter = mPeerListAdapter
        peer_list.onItemClickListener = AdapterView.OnItemClickListener {
            parent, view, position, id ->
            var holder = view.tag as PeerListItemHolder
            mRtcInstance?.connectToPeer(holder.mPeer?.id)
            mRtcInstance?.createDataChannel("TestSendChannel")
        }

        mSSProxyServer = SSProxyServer(this)
        mSSProxyServer?.start()
    }

    override fun onDestroy() {
        super.onDestroy()
        mRunning = false
        mSSProxyServer?.stop()
        mRtcInstance?.release()
    }

    val MSG_PEER_LIST_UPDATED = 0x901

    class MainHandler(activity: RtcMainActivity): Handler() {
        private val mActivity = activity
        override fun handleMessage(msg: Message) {
            when (msg.what) {
                mActivity.MSG_PEER_LIST_UPDATED -> {
                    mActivity.mPeerList = mActivity.mRtcInstance?.getPeerList()
                    mActivity.mPeerListAdapter?.updatePeerList(mActivity.mPeerList!!)
                    mActivity.mPeerListAdapter?.notifyDataSetChanged()
                }
                else -> {

                }
            }
        }
    }
    val mHandler: MainHandler = MainHandler(this)

    class RtcPeerListListener(activity: RtcMainActivity, handler: MainHandler)
        : RtcPeerContainer.RtcPeerListListener {
        private val mHandler: MainHandler = handler
        private val mActivity = activity

        override fun onUpdated(peerList: ArrayList<RtcPeerContainer.RtcPeer>) {
            mHandler.sendEmptyMessage(mActivity.MSG_PEER_LIST_UPDATED)
        }
    }

    class PeerListItemHolder(view: View, peer: RtcPeerContainer.RtcPeer?) {
        var mNameView: TextView = view as TextView
        var mPeer: RtcPeerContainer.RtcPeer? = peer
    }

    class PeerListAdapter(context: Context,
                          peerList: ArrayList<RtcPeerContainer.RtcPeer>?): BaseAdapter() {
        var mPeerList = peerList
        val mContext = context

        override fun getView(p0: Int, p1: View?, p2: ViewGroup?): View {
            val view: View?
            val vh: PeerListItemHolder
            if (p1 == null) {
                view = TextView(mContext)
                vh = PeerListItemHolder(view, mPeerList?.get(p0))
                view.tag = vh
            } else {
                view = p1
                vh = view.tag as PeerListItemHolder
            }

            vh.mNameView.text = vh.mPeer?.name

            return view
        }

        fun updatePeerList(peerList: ArrayList<RtcPeerContainer.RtcPeer>) {
            mPeerList = peerList
        }

        override fun getItem(p0: Int): Any? {
            return mPeerList?.get(p0)
        }

        override fun getItemId(p0: Int): Long {
            return mPeerList?.get(p0)!!.id
        }

        override fun getCount(): Int {
            return if (mPeerList != null) {
                mPeerList!!.size
            } else {
                0
            }
        }
    }
}
