package com.kkt.rtcdatachanneldemo

import android.content.Context
import android.util.Log
import org.webrtc.*
import java.util.*

/**
 * Created by owen on 18-3-3.
 */
class RtcClient(context: Context, peerServerSendHelper: RtcPeerServerSendHelper) {
    lateinit var mLocalPeerConnection: PeerConnection
    lateinit var mRemotePeerConnection: PeerConnection

    lateinit var mPeerConnectionFactory: PeerConnectionFactory

    private val mIceServers = LinkedList<PeerConnection.IceServer>()
    private val mPeerConnectionConstraints = MediaConstraints()
    lateinit var mSendDataChannel: DataChannel

    private var mRemotePeerId: Long = 0

    private var mContext: Context = context

    interface RtcPeerServerSendHelper {
        fun sendDataToPeer(data: String, peerId: Long)
    }

    var mRtcPeerServerSenderHelper: RtcPeerServerSendHelper? = peerServerSendHelper

    class PeerObserver(rtcClient: RtcClient) : SdpObserver, PeerConnection.Observer {
        private val TAG = "RTCDCDemo"

        val mRtcClient: RtcClient = rtcClient

        override fun onSetSuccess() {
            Log.d(TAG, "OnSetSuccess")
        }

        override fun onCreateSuccess(p0: SessionDescription?) {
            mRtcClient.mLocalPeerConnection.setLocalDescription(this, p0)
            val str: String = "{\"sdp\":\"" + p0?.description +
                    "\",\"type\":\"" + p0?.type?.canonicalForm() + "\"}"
            mRtcClient.mRtcPeerServerSenderHelper?.sendDataToPeer(str, mRtcClient.mRemotePeerId)
        }

        override fun onCreateFailure(p0: String?) {
            Log.d(TAG, "onCreateFailure")
        }

        override fun onIceGatheringChange(p0: PeerConnection.IceGatheringState?) {
            Log.d(TAG, "onIceGatheringChange")
        }

        override fun onAddStream(p0: MediaStream?) {
            Log.d(TAG, "onAddStream")
        }

        override fun onIceCandidate(p0: IceCandidate?) {
            Log.d(TAG, "onIceCandidate")
        }

        override fun onDataChannel(p0: DataChannel?) {
            Log.d(TAG, "onDataChannel")
        }

        override fun onSignalingChange(p0: PeerConnection.SignalingState?) {
            Log.d(TAG, "onSignalingChange")
        }

        override fun onRemoveStream(p0: MediaStream?) {
            Log.d(TAG, "onRemoveStream")
        }

        override fun onIceConnectionChange(p0: PeerConnection.IceConnectionState?) {
            Log.d(TAG, "onIceConnectionChange")
        }

        override fun onRenegotiationNeeded() {
            Log.d(TAG, "onRenegotiationNeeded")
        }

        override fun onSetFailure(p0: String?) {
            Log.d(TAG, "onSetFailure")
        }

    }

    private val mPeerObserver: PeerObserver = PeerObserver(this)

    fun init() {
        PeerConnectionFactory.initializeAndroidGlobals(mContext, true, true, true, true)
        mIceServers.add(PeerConnection.IceServer("stun:192.168.198.180:9999"))
        mPeerConnectionFactory = PeerConnectionFactory()
    }

    fun connectToPeer(peerId: Long?) {
        mRemotePeerId = peerId!!

        mLocalPeerConnection = mPeerConnectionFactory.createPeerConnection(
                mIceServers, mPeerConnectionConstraints, mPeerObserver)
        mSendDataChannel = mLocalPeerConnection.createDataChannel(
                "SendDataChannel", DataChannel.Init())
        mLocalPeerConnection.createOffer(mPeerObserver, MediaConstraints())

        mRemotePeerConnection = mPeerConnectionFactory.createPeerConnection(
                mIceServers, mPeerConnectionConstraints, mPeerObserver)
    }
}