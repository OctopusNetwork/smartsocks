package com.kkt.rtcdatachanneldemo

import android.content.Context
import android.util.Log
import org.json.JSONObject
import org.webrtc.*
import java.nio.ByteBuffer
import java.util.*

/**
 * Created by owen on 18-3-3.
 */
class RtcClient(context: Context,
                peerServerSendHelper: RtcPeerServerSendHelper,
                rtcDataChannelListener: RtcDataChannelListener) {
    var mLocalPeerConnection: PeerConnection? = null
    lateinit var mRemotePeerConnection: PeerConnection

    lateinit var mPeerConnectionFactory: PeerConnectionFactory

    private val mIceServers = LinkedList<PeerConnection.IceServer>()
    private val mPeerConnectionConstraints = MediaConstraints()
    var mSendDataChannel: DataChannel? = null

    private var mRemotePeerId: Long = -1

    private var mContext: Context = context

    interface RtcPeerServerSendHelper {
        fun sendDataToPeer(data: String, peerId: Long)
    }

    interface RtcDataChannelListener {
        fun onMessage(byteBuffer: ByteBuffer)
        fun onStateChange(state: String)
    }

    val mRtcDataChannelListener: RtcDataChannelListener = rtcDataChannelListener
    var mRtcPeerServerSenderHelper: RtcPeerServerSendHelper? = peerServerSendHelper

    class PeerObserver(rtcClient: RtcClient) : SdpObserver, PeerConnection.Observer {
        override fun onIceConnectionReceivingChange(p0: Boolean) {
            TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
        }

        private val TAG = "RTCDCDemo"

        val mRtcClient: RtcClient = rtcClient

        override fun onSetSuccess() {
            Log.d(TAG, "OnSetSuccess")
        }

        override fun onCreateSuccess(p0: SessionDescription?) {
            mRtcClient.mLocalPeerConnection?.setLocalDescription(this, p0)
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
            var str: String = "{\"candidate\":\"" + p0?.sdp + "\"," +
                    "\"sdpMLineIndex\":\"" + p0?.sdpMLineIndex + "\"," +
                    "\"sdpMid\":\"" + p0?.sdpMid + "\"}"
            mRtcClient.mRtcPeerServerSenderHelper?.
                    sendDataToPeer(str, mRtcClient.mRemotePeerId)
        }

        override fun onDataChannel(p0: DataChannel?) {
            p0?.registerObserver(mRtcClient.mDataChannelObserver)
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
        PeerConnectionFactory.initializeAndroidGlobals(mContext, true, true, true)
        mIceServers.add(PeerConnection.IceServer("stun:192.168.196.230:9999"))
        mPeerConnectionFactory = PeerConnectionFactory()
        Logging.enableTracing("logcat:",
                EnumSet.of(Logging.TraceLevel.TRACE_ALL),
                Logging.Severity.LS_INFO)
    }

    class DataChannelObserver(rtcClient: RtcClient): DataChannel.Observer {
        override fun onBufferedAmountChange(p0: Long) {
            TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
        }

        val mRtcClient = rtcClient

        override fun onMessage(p0: DataChannel.Buffer?) {
            mRtcClient.mRtcDataChannelListener.onMessage(p0?.data!!)
        }

        override fun onStateChange() {
            mRtcClient.mRtcDataChannelListener.onStateChange(
                    mRtcClient.mSendDataChannel?.state()?.name!!)
        }
    }

    val mDataChannelObserver: DataChannelObserver = DataChannelObserver(this)

    private fun createPeerConnection() {
        if (null == mLocalPeerConnection) {
            mLocalPeerConnection = mPeerConnectionFactory.createPeerConnection(
                    mIceServers, mPeerConnectionConstraints, mPeerObserver)
            mSendDataChannel = mLocalPeerConnection?.createDataChannel(
                    "SendDataChannel", DataChannel.Init())

            mSendDataChannel?.registerObserver(mDataChannelObserver)

            mRemotePeerConnection = mPeerConnectionFactory.createPeerConnection(
                    mIceServers, mPeerConnectionConstraints, mPeerObserver)
        }
    }

    fun connectToPeer(peerId: Long?) {
        mRemotePeerId = peerId!!
        createPeerConnection()
        mLocalPeerConnection?.createOffer(mPeerObserver, MediaConstraints())
    }

    fun processSessionDescription(jsonObject: JSONObject) {
        val typeStr: String = jsonObject.getString("type")
        val sdpType = SessionDescription.Type.fromCanonicalForm(typeStr)
        var sdpStr: String? = null

        if (jsonObject.has("sdp")) {
            sdpStr = jsonObject.getString("sdp")
            val sdp = SessionDescription(sdpType, sdpStr)
            createPeerConnection()
            mLocalPeerConnection?.setRemoteDescription(mPeerObserver, sdp)
            if (sdpType == SessionDescription.Type.OFFER) {
                mLocalPeerConnection?.createAnswer(mPeerObserver, MediaConstraints())
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
        mLocalPeerConnection?.addIceCandidate(iceCandidate)
    }

    fun processPeerMessage(peerId: Long, message: String?) {
        val jsonObject: JSONObject = JSONObject(message)

        if (-1.toLong() == mRemotePeerId) {
            mRemotePeerId = peerId
        }

        if (mRemotePeerId != peerId) {
            // Error
        }

        if (jsonObject.has("type")) {
            processSessionDescription(jsonObject)
        } else {
            processIceCandidate(jsonObject)
        }
    }
}