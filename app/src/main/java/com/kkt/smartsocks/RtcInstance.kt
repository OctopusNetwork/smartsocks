package com.kkt.smartsocks

import android.content.Context

/**
 * Created by owen on 18-3-7.
 */
class RtcInstance(context: Context,
                  dataChannelListener: RtcClient.RtcDataChannelListener,
                  peerListListener: RtcPeerContainer.RtcPeerListListener) {
    val mContext = context
    var mRtcPeerCon: RtcPeerContainer? = null
    var mRtcPeerMessageListener: RtcPeerMessageListener? = null

    val mDataChannelListener = dataChannelListener
    val mPeerListListener = peerListListener

    var mPeerServerSendHelper: PeerServerSendHelper = PeerServerSendHelper(this)
    var mRtcClient: RtcClient? = RtcClient(mContext, mPeerServerSendHelper, mDataChannelListener)

    class RtcPeerMessageListener(instance: RtcInstance) : RtcPeerContainer.RtcPeerMessageListener {
        private val mRtcInstance = instance
        override fun onPeerMessage(peerId: Long, message: String?) {
            mRtcInstance.mRtcClient?.processPeerMessage(peerId, message)
        }
    }

    class PeerServerSendHelper(instance: RtcInstance) : RtcClient.RtcPeerServerSendHelper {
        private val mRtcInstance = instance
        override fun sendDataToPeer(data: String, peerId: Long) {
            mRtcInstance.mRtcPeerCon?.sendToPeer(data, peerId)
        }
    }

    fun initialize() {
        mRtcPeerMessageListener = RtcPeerMessageListener(this)
        mRtcPeerCon = RtcPeerContainer(mContext,
                mPeerListListener, mRtcPeerMessageListener!!)

        mRtcClient?.init()
        mRtcPeerCon!!.login()
    }

    fun final() {
        mRtcPeerCon?.logout()
        mRtcClient?.final()
    }

    fun connectToPeer(peerId: Long?) {
        mRtcClient?.connectToPeer(peerId)
    }

    fun getPeerList(): ArrayList<RtcPeerContainer.RtcPeer>? {
        return mRtcPeerCon?.getPeerList()
    }

    fun sendString(str: String) {
        mRtcClient?.sendString(str)
    }
}