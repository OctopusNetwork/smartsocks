package com.kkt.rtcdatachanneldemo

import android.content.Context
import android.os.Bundle
import android.os.Handler
import android.os.Message
import android.support.v7.app.AppCompatActivity
import android.view.View
import android.view.ViewGroup
import android.widget.AdapterView
import android.widget.BaseAdapter
import android.widget.TextView
import kotlinx.android.synthetic.main.activity_main.*

class MainActivity : AppCompatActivity() {

    var mPeerList: ArrayList<RtcPeerContainer.RtcPeer>? = null
    var mRtcPeerCon: RtcPeerContainer? = null
    var mPeerListAdapter: PeerListAdapter? = null
    var mRtcPeerListListener: RtcPeerListListener? = null
    var mRtcPeerMessageListener: RtcPeerMessageListener? = null

    class PeerServerSendHelper(activity: MainActivity) : RtcClient.RtcPeerServerSendHelper {
        val mActivity: MainActivity = activity
        override fun sendDataToPeer(data: String, peerId: Long) {
            mActivity.mRtcPeerCon?.sendToPeer(data, peerId)
        }
    }

    var mPeerServerSendHelper: PeerServerSendHelper = PeerServerSendHelper(this)

    var mRtcClient: RtcClient? = RtcClient(this, mPeerServerSendHelper)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        mPeerListAdapter = PeerListAdapter(this, mPeerList)
        mRtcPeerListListener = RtcPeerListListener(this, mHandler)
        mRtcPeerMessageListener = RtcPeerMessageListener(this)
        mRtcPeerCon = RtcPeerContainer(this,
                mRtcPeerListListener!!, mRtcPeerMessageListener!!)

        mRtcClient?.init()

        mRtcPeerCon!!.login()

        peer_list.adapter = mPeerListAdapter
        peer_list.onItemClickListener = AdapterView.OnItemClickListener {
            parent, view, position, id ->
            var holder = view.tag as PeerListItemHolder
            mRtcClient?.connectToPeer(holder.mPeer?.id)
        }
    }

    val MSG_PEER_LIST_UPDATED = 0x901

    class MainHandler(activity: MainActivity): Handler() {
        val mActivity = activity
        override fun handleMessage(msg: Message) {
            when (msg.what) {
                mActivity.MSG_PEER_LIST_UPDATED -> {
                    mActivity.mPeerList = mActivity.mRtcPeerCon?.getPeerList()
                    mActivity.mPeerListAdapter?.updatePeerList(mActivity.mPeerList!!)
                    mActivity.mPeerListAdapter?.notifyDataSetChanged()
                }
                else -> {

                }
            }
        }
    }
    val mHandler: MainHandler = MainHandler(this)

    class RtcPeerListListener(activity: MainActivity, handler: MainHandler)
        : RtcPeerContainer.RtcPeerListListener {
        val mHandler: MainHandler = handler
        val mActivity = activity

        override fun onUpdated(peerList: ArrayList<RtcPeerContainer.RtcPeer>) {
            mHandler.sendEmptyMessage(mActivity.MSG_PEER_LIST_UPDATED)
        }
    }

    class RtcPeerMessageListener(activity: MainActivity) : RtcPeerContainer.RtcPeerMessageListener {
        val mActivity = activity
        override fun onPeerMessage(peerId: Long, message: String?) {
            mActivity.mRtcClient?.processPeerMessage(peerId, message)
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

            vh.mNameView.setText(vh.mPeer?.name)

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
