package com.kkt.smartsocks.rtc

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.BaseAdapter
import android.widget.TextView
import com.kkt.smartsocks.R

/**
 * Created by owen on 18-3-14.
 */
class PeerListAdapter(context: Context,
                      peerList: ArrayList<RtcPeerContainer.RtcPeer>?) : BaseAdapter() {
    var mPeerList = peerList
    val mContext = context

    class PeerListItemHolder(view: View, peer: RtcPeerContainer.RtcPeer?) {
        var mNameView: TextView = view.findViewById(R.id.tvName) as TextView
        var mPeer: RtcPeerContainer.RtcPeer? = peer
    }

    override fun getView(p0: Int, p1: View?, p2: ViewGroup?): View {
        val view: View?
        val vh: PeerListItemHolder
        if (p1 == null) {
            view = LayoutInflater.from(mContext).inflate(R.layout.peers_list_item, p2, false)
            vh = PeerListItemHolder(view, mPeerList?.get(p0))
            view.tag = vh
        } else {
            view = p1
            vh = view.tag as PeerListItemHolder
        }

        vh.mNameView.text = vh.mPeer?.name

        return view!!
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