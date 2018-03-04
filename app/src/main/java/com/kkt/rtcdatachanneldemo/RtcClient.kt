package com.kkt.rtcdatachanneldemo

import android.content.Context
import android.util.Log
import org.json.JSONObject
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
            mRtcClient.mRtcPeerServerSenderHelper?.
                    sendDataToPeer(str, mRtcClient.mRemotePeerId)
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
            Log.d(TAG, p0?.sdp)
            Log.d(TAG, p0?.sdpMid)
            Log.d(TAG, "" + p0?.sdpMLineIndex)

            var str: String = "{\"candidate\":\"" + p0?.sdp + "\"," +
                    "\"sdpMLineIndex\":\"" + p0?.sdpMLineIndex + "\"," +
                    "\"sdpMid\":\"" + p0?.sdpMid + "\"}"
            mRtcClient.mRtcPeerServerSenderHelper?.
                    sendDataToPeer(str, mRtcClient.mRemotePeerId)
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

    fun processSessionDescription(jsonObject: JSONObject) {
        val typeStr: String = jsonObject.getString("type")
        val sdpType = SessionDescription.Type.fromCanonicalForm(typeStr)
        var sdpStr: String? = null

        if (jsonObject.has("sdp")) {
            sdpStr = jsonObject.getString("sdp")
            val sdp = SessionDescription(sdpType, sdpStr)
            mLocalPeerConnection.setRemoteDescription(mPeerObserver, sdp)
            if (sdpType == SessionDescription.Type.OFFER) {
                mLocalPeerConnection.createAnswer(mPeerObserver, MediaConstraints())
            }
        }
    }

    fun processIceCandidate(jsonObject: JSONObject) {
        var sdpMid: String? = null
        var sdpStr: String? = null
        var sdpMLineIndex: Int? = null

        if (jsonObject.has("sdpMid")) {
            sdpMid = jsonObject.getString("sdpMid")
        }

        if (jsonObject.has("sdpMLineIndex")) {
            sdpMLineIndex = jsonObject.getInt("sdpMLineIndex")
        }

        if (jsonObject.has("candidate")) {
            sdpStr = jsonObject.getString("candidate")
        }

        val iceCandidate = IceCandidate(sdpMid, sdpMLineIndex!!, sdpStr)
        mLocalPeerConnection.addIceCandidate(iceCandidate)
    }

    fun processPeerMessage(peerId: Long, message: String?) {
        val jsonObject: JSONObject = JSONObject(message)
        if (jsonObject.has("type")) {
            processSessionDescription(jsonObject)
        } else {
            processIceCandidate(jsonObject)
        }
    }
}