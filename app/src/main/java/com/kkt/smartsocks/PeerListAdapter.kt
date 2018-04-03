package com.kkt.smartsocks

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.BaseAdapter
import android.widget.TextView
import com.kkt.rtc.RtcSignalling

/**
 * Created by owen on 18-3-24.
 */
class PeerListAdapter(context: Context,
                      peerMap: HashMap<String, RtcSignalling.RtcPeer>?) :
        BaseAdapter() {
    private var mPeerList = ArrayList<RtcSignalling.RtcPeer>()
    private val mContext = context

    private fun peerMapToList(peerMap: HashMap<String, RtcSignalling.RtcPeer>?) {
        if (peerMap != null) {
            mPeerList.clear()
            for ((_, v) in peerMap) {
                mPeerList.add(v)
            }
        }
    }

    init {
        peerMapToList(peerMap)
    }

    class PeerListItemHolder(view: View, peer: RtcSignalling.RtcPeer?) {
        var mNameView: TextView = view.findViewById(R.id.tvName) as TextView
        var mPeer: RtcSignalling.RtcPeer? = peer
    }

    override fun getView(p0: Int, p1: View?, p2: ViewGroup?): View {
        val view: View?
        val vh: PeerListItemHolder
        if (p1 == null) {
            view = LayoutInflater.from(mContext).inflate(
                    R.layout.peer_list_item, p2, false)
            vh = PeerListItemHolder(view, mPeerList?.get(p0))
            view.tag = vh
        } else {
            view = p1
            vh = view.tag as PeerListItemHolder
        }

        vh.mNameView.text = vh.mPeer?.mPeerID

        return view!!
    }

    fun addPeer(peer: RtcSignalling.RtcPeer) {
        val it = mPeerList.iterator()
        while (it.hasNext()) {
            if (it.next().mPeerID == peer.mPeerID) {
                return
            }
        }

        mPeerList.add(peer)
        notifyDataSetChanged()
    }

    fun delPeer(peer: RtcSignalling.RtcPeer) {
        val it = mPeerList.iterator()
        while (it.hasNext()) {
            if (it.next().mPeerID == peer.mPeerID) {
                it.remove()
            }
        }
        notifyDataSetChanged()
    }

    fun updatePeerList(peerMap: HashMap<String, RtcSignalling.RtcPeer>) {
        peerMapToList(peerMap)
        notifyDataSetChanged()
    }

    override fun getItem(p0: Int): Any? {
        return mPeerList?.get(p0)
    }

    override fun getItemId(p0: Int): Long {
        return p0.toLong()
    }

    override fun getCount(): Int {
        return mPeerList.size
    }
}