package com.kkt.smartsocks.rtc

import android.content.Context
import android.util.Log
import java.nio.ByteBuffer

/**
 * Created by owen on 18-3-7.
 */
class RtcInstance(context: Context,
                  peerListListener: RtcPeerContainer.RtcPeerListListener,
                  peerName: String) {
    val mContext = context
    var mRtcPeerCon: RtcPeerContainer? = null
    var mRtcPeerMessageListener: RtcPeerMessageListener? = null
    val mMyPeerName: String = peerName

    val mPeerListListener = peerListListener

    var mPeerServerSendHelper: PeerServerSendHelper = PeerServerSendHelper(this)
    var mRtcClient: RtcClient? = RtcClient(mContext, mPeerServerSendHelper)

    class RtcPeerMessageListener(instance: RtcInstance) : RtcPeerContainer.RtcPeerMessageListener {
        private val mRtcInstance = instance
        val TAG: String = "RTCInstance"
        override fun onPeerMessage(peerId: Long, message: String?) {
            Log.d(TAG, "Message from peer $peerId: $message")
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
                mPeerListListener, mRtcPeerMessageListener!!, mMyPeerName)

        mRtcClient?.init()
        mRtcPeerCon!!.login()
    }

    fun release() {
        mRtcPeerCon?.logout()
        mRtcClient?.release()
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

    fun sendBinary(buffer: ByteBuffer) {
        mRtcClient?.sendBinary(buffer)
    }

    fun createDataChannel(label: String) {
        mRtcClient?.createDataChannel(label, false, true)
    }

    fun destroyDataChannel() {
        mRtcClient?.destroyDataChannel()
    }

    fun destroyPeerConnection() {
        mRtcClient?.destroyPeerConnection();
    }

    fun setSendDataChannelListener(listener: RtcClient.RtcDataChannelListener) {
        mRtcClient?.setSendDataChannelListener(listener)
    }

    fun setRecvDataChannelListener(listener: RtcClient.RtcDataChannelListener) {
        mRtcClient?.setRecvDataChannelListener(listener)
    }
}