package com.kkt.smartsocks.rtc

import android.content.Context
import android.util.Log
import org.json.JSONException
import org.json.JSONObject
import org.webrtc.*
import java.nio.ByteBuffer
import java.util.*

/**
 * Created by owen on 18-3-3.
 */
class RtcClient(context: Context,
                peerServerSendHelper: RtcPeerServerSendHelper) {
    var mLocalPeerConnection: PeerConnection? = null
    var mRemotePeerConnection: PeerConnection? = null

    lateinit var mPeerConnectionFactory: PeerConnectionFactory

    private val mIceServers = LinkedList<PeerConnection.IceServer>()
    private val mPeerConnectionConstraints = MediaConstraints()
    var mSendDataChannel: DataChannel? = null

    private var mRemotePeerId: Long = -1

    private var mContext: Context = context

    private var mCanSendData: Boolean = false

    interface RtcPeerServerSendHelper {
        fun sendDataToPeer(data: String, peerId: Long)
    }

    interface RtcDataChannelListener {
        fun onMessage(byteBuffer: ByteBuffer)
        fun onStateChange(state: String)
    }

    var mRtcSendDataChannelListener: RtcDataChannelListener? = null
    var mRtcRecvDataChannelListener: RtcDataChannelListener? = null
    var mRtcPeerServerSenderHelper: RtcPeerServerSendHelper? = peerServerSendHelper

    class PeerObserver(rtcClient: RtcClient) : SdpObserver, PeerConnection.Observer {
        override fun onIceCandidatesRemoved(p0: Array<out IceCandidate>?) {

        }

        override fun onAddTrack(p0: RtpReceiver?, p1: Array<out MediaStream>?) {

        }

        private val TAG = "RTCDCDemo"

        val mRtcClient: RtcClient = rtcClient

        override fun onIceConnectionReceivingChange(p0: Boolean) {

        }

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
            Log.d(TAG, "Send ice candidate to peer: " + str)
            mRtcClient.mRtcPeerServerSenderHelper?.
                    sendDataToPeer(str, mRtcClient.mRemotePeerId)
        }

        override fun onDataChannel(p0: DataChannel?) {
            p0?.registerObserver(object: DataChannel.Observer{
                override fun onMessage(buffer: DataChannel.Buffer?) {
                    mRtcClient.mRtcRecvDataChannelListener?.onMessage(buffer?.data!!)
                }

                override fun onBufferedAmountChange(p0: Long) {

                }

                override fun onStateChange() {
                    mRtcClient.mRtcRecvDataChannelListener?.onStateChange(p0.state().name)
                }
            })
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
        mIceServers.add(PeerConnection.IceServer.builder(
                "turn:47.254.28.11:3478?transport=udp")
                .setUsername("kkt")
                .setPassword("1qaz2wsx")
                .setHostname("47.254.28.11")
                .createIceServer())
        mIceServers.add(PeerConnection.IceServer.builder(
                "stun:47.254.28.11:3478").createIceServer())
        PeerConnectionFactory.initialize(
                PeerConnectionFactory.InitializationOptions.builder(mContext)
                        .setFieldTrials("")
                        .setEnableVideoHwAcceleration(true)
                        .setEnableInternalTracer(true)
                        .createInitializationOptions())
        mPeerConnectionFactory = PeerConnectionFactory.builder().createPeerConnectionFactory()
        Logging.enableLogToDebugOutput(Logging.Severity.LS_VERBOSE)
    }

    fun release() {
        destroyPeerConnection()
        mPeerConnectionFactory.dispose()
        PeerConnectionFactory.stopInternalTracingCapture()
        PeerConnectionFactory.shutdownInternalTracer()
    }

    fun setSendDataChannelListener(dataChannelListener: RtcDataChannelListener) {
        mRtcSendDataChannelListener = dataChannelListener
    }

    fun setRecvDataChannelListener(dataChannelListener: RtcDataChannelListener) {
        mRtcRecvDataChannelListener = dataChannelListener
    }

    fun sendString(str: String) {
        if (mCanSendData) {
            sendBinary(ByteBuffer.wrap(str.toByteArray()))
        }
    }

    fun sendBinary(buffer: ByteBuffer) {
        if (mCanSendData) {
            if (false == mSendDataChannel?.send(DataChannel.Buffer(
                    buffer, true))) {
                Log.e("RTCDataChannel", "Write error " + buffer.limit() + " bytes")
            }
        }
    }

    private fun createPeerConnection() {
        if (null == mLocalPeerConnection) {
            mLocalPeerConnection = mPeerConnectionFactory.createPeerConnection(
                    mIceServers, mPeerConnectionConstraints, mPeerObserver)
            mRemotePeerConnection = mPeerConnectionFactory.createPeerConnection(
                    mIceServers, mPeerConnectionConstraints, mPeerObserver)
        }
    }

    fun createDataChannel(label: String, needAnswer: Boolean = false, needOffer: Boolean = false) {
        mSendDataChannel = mLocalPeerConnection?.createDataChannel(
                label, DataChannel.Init())
        mSendDataChannel?.registerObserver(object: DataChannel.Observer{
            override fun onMessage(p0: DataChannel.Buffer?) {
                mRtcSendDataChannelListener?.onMessage(p0?.data!!)
            }

            override fun onBufferedAmountChange(p0: Long) {

            }

            override fun onStateChange() {
                mRtcSendDataChannelListener?.onStateChange(mSendDataChannel?.state()?.name!!)
                if (DataChannel.State.OPEN == mSendDataChannel?.state()) {
                    mCanSendData = true
                }
            }
        })

        if (needAnswer) {
            mLocalPeerConnection?.createAnswer(mPeerObserver, MediaConstraints())
        } else if (needOffer) {
            mLocalPeerConnection?.createOffer(mPeerObserver, MediaConstraints())
        }
    }

    fun destroyDataChannel() {
        mSendDataChannel?.close()
    }

    fun connectToPeer(peerId: Long?) {
        mRemotePeerId = peerId!!
        createPeerConnection()
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
            createDataChannel("ResponseChannel", (sdpType == SessionDescription.Type.OFFER))
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
        try {
            val jsonObject: JSONObject = JSONObject(message)

            if ((-1).toLong() == mRemotePeerId) {
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
        } catch (error: JSONException) {
            error.printStackTrace()
        }
    }

    fun destroyPeerConnection() {
        mLocalPeerConnection?.dispose()
        mRemotePeerConnection?.dispose()
    }
}