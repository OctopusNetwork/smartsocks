package com.kkt.rtc

import android.content.Context
import org.webrtc.*
import java.nio.ByteBuffer
import java.util.*

/**
 * Created by owen on 18-3-30.
 */
class RtcAgent(context: Context?,
               signallingSender: RtcSignalling.RtcSignallingSender,
               sendDataChannelListener: RtcDataChannelListener,
               recvDataChannelListener: RtcDataChannelListener,
               socketProtectListener: RtcSocketProtectListener?,
               remotePeerId: String,
               config: RtcConfig) {
    val mContext = context
    val mSignallingSender = signallingSender
    val mSendDataChannelListener = sendDataChannelListener
    val mRecvDataChannelListener = recvDataChannelListener
    val mConfig = config
    var mIceServers = LinkedList<PeerConnection.IceServer>()
    var mPeerConnectionFactory: PeerConnectionFactory? = null
    var mRemotePeerId = remotePeerId

    var mSendDataChannel: DataChannel? = null
    var mSendDataChannelReady: Boolean = false

    private val mPeerConnectionConstraints = MediaConstraints()

    init {
        mRtcSocketProtectListener = socketProtectListener
    }

    companion object {
        var mLocalPeerConnection: PeerConnection? = null
        var mRemotePeerConnection: PeerConnection? = null
        var mPeerConnectionInited: Boolean = false
        var mRtcSocketProtectListener: RtcSocketProtectListener? = null

        @JvmStatic
        fun protectSocket(socket: Int) {
            mRtcSocketProtectListener?.onProtectSocket(socket)
        }
    }

    enum class RtcIceServerType {
        RTC_ICE_STUN,
        RTC_ICE_TURN
    }

    enum class RtcIceServerProto {
        RTC_ICE_PROTO_UDP,
        RTC_ICE_PROTO_TCP
    }

    enum class RtcRole {
        RTC_INITIATOR,
        RTC_ACCEPTOR
    }

    open class RtcIceConfig(type: RtcIceServerType, host: String,
                            port: Int, hostname: String,
                            username: String, password: String,
                            proto: RtcIceServerProto) {
        private val mType = type
        private val mHost = host
        private val mPort = port
        private val mUsername = username
        private val mPassword = password
        private val mProto = proto
        private val mHostname = hostname

        private val TAG = "RtcIceConfig"

        private fun appendUriCommon(uri: String) : String {
            var localUri = uri
            localUri += ":$mHost:$mPort?transport="
            if (RtcIceServerProto.RTC_ICE_PROTO_UDP == mProto) {
                localUri += "udp"
            } else if (RtcIceServerProto.RTC_ICE_PROTO_TCP == mProto) {
                localUri += "tcp"
            }

            return localUri
        }

        private fun turnConfigToIceServer() : PeerConnection.IceServer {
            var uri = "turn"
            uri = appendUriCommon(uri)
            RtcLogging.debug(TAG, uri)
            return PeerConnection.IceServer.builder(uri)
                    .setUsername(mUsername)
                    .setPassword(mPassword)
                    .setHostname(mHostname)
                    .createIceServer()
        }

        private fun stunConfigToIceServer() : PeerConnection.IceServer {
            var uri = "stun"
            uri = appendUriCommon(uri)
            RtcLogging.debug(TAG, uri)
            return PeerConnection.IceServer.builder(uri).createIceServer()
        }

        fun iceConfigToIceServer() : PeerConnection.IceServer {
            return when (mType) {
                RtcIceServerType.RTC_ICE_TURN -> turnConfigToIceServer()
                RtcIceServerType.RTC_ICE_STUN -> stunConfigToIceServer()
            }
        }
    }

    open class RtcConfig(iceConfigs: ArrayList<RtcIceConfig>?) {
        val mIceConfigs = iceConfigs
    }

    interface RtcDataChannelListener {
        fun onMessage(byteBuffer: ByteBuffer)
        fun onStateChange(state: String)
    }

    interface RtcSocketProtectListener {
        fun onProtectSocket(socket: Int)
    }

    inner class PeerObserver : SdpObserver, PeerConnection.Observer {
        override fun onIceCandidatesRemoved(p0: Array<out IceCandidate>?) {

        }

        override fun onAddTrack(p0: RtpReceiver?, p1: Array<out MediaStream>?) {

        }

        private val TAG = "RTCAgent"

        override fun onIceConnectionReceivingChange(p0: Boolean) {

        }

        override fun onSetSuccess() {
            RtcLogging.debug(TAG, "OnSetSuccess")
        }

        override fun onCreateSuccess(p0: SessionDescription?) {
            mLocalPeerConnection?.setLocalDescription(this, p0)
            val str: String = "{\"sdp\":\"" + p0?.description +
                    "\",\"type\":\"" + p0?.type?.canonicalForm() + "\"}"
            mSignallingSender?.sendToPeer(str, mRemotePeerId)
        }

        override fun onCreateFailure(p0: String?) {
            RtcLogging.debug(TAG, "onCreateFailure")
        }

        override fun onIceGatheringChange(p0: PeerConnection.IceGatheringState?) {
            RtcLogging.debug(TAG, "onIceGatheringChange")
        }

        override fun onAddStream(p0: MediaStream?) {
            RtcLogging.debug(TAG, "onAddStream")
        }

        override fun onIceCandidate(p0: IceCandidate?) {
            var str: String = "{\"candidate\":\"" + p0?.sdp + "\"," +
                    "\"sdpMLineIndex\":\"" + p0?.sdpMLineIndex + "\"," +
                    "\"sdpMid\":\"" + p0?.sdpMid + "\"}"
            RtcLogging.debug(TAG, "Send ice candidate to peer: " + str)
            mSignallingSender?.sendToPeer(str, mRemotePeerId)
        }

        override fun onDataChannel(p0: DataChannel?) {
            p0?.registerObserver(object: DataChannel.Observer{
                override fun onMessage(buffer: DataChannel.Buffer?) {
                    mRecvDataChannelListener?.onMessage(buffer?.data!!)
                }

                override fun onBufferedAmountChange(p0: Long) {

                }

                override fun onStateChange() {
                    mRecvDataChannelListener?.onStateChange(p0.state().name)
                }
            })
        }

        override fun onSignalingChange(p0: PeerConnection.SignalingState?) {
            RtcLogging.debug(TAG, "onSignalingChange")
        }

        override fun onRemoveStream(p0: MediaStream?) {
            RtcLogging.debug(TAG, "onRemoveStream")
        }

        override fun onIceConnectionChange(p0: PeerConnection.IceConnectionState?) {
            RtcLogging.debug(TAG, "onIceConnectionChange")
        }

        override fun onRenegotiationNeeded() {
            RtcLogging.debug(TAG, "onRenegotiationNeeded")
        }

        override fun onSetFailure(p0: String?) {
            RtcLogging.debug(TAG, "onSetFailure")
        }
    }

    private val mPeerObserver: PeerObserver = PeerObserver()

    private fun createDataChannel(label: String,
                                  needAnswer: Boolean = false,
                                  needOffer: Boolean = false) {
        mSendDataChannel = mLocalPeerConnection?.createDataChannel(
                label, DataChannel.Init())
        mSendDataChannel?.registerObserver(object: DataChannel.Observer{
            override fun onMessage(p0: DataChannel.Buffer?) {
                mSendDataChannelListener?.onMessage(p0?.data!!)
            }

            override fun onBufferedAmountChange(p0: Long) {

            }

            override fun onStateChange() {
                mSendDataChannelListener?.onStateChange(mSendDataChannel?.state()?.name!!)
                if (DataChannel.State.OPEN == mSendDataChannel?.state()) {
                    mSendDataChannelReady = true
                }
            }
        })

        if (needAnswer) {
            mLocalPeerConnection?.createAnswer(mPeerObserver, MediaConstraints())
        } else if (needOffer) {
            mLocalPeerConnection?.createOffer(mPeerObserver, MediaConstraints())
        }
    }

    fun initialize(label: String, role: RtcRole) {
        if (!mPeerConnectionInited) {
            for (iceConfig in mConfig.mIceConfigs!!) {
                mIceServers.add(iceConfig.iceConfigToIceServer())
            }

            PeerConnectionFactory.initialize(
                    PeerConnectionFactory.InitializationOptions.builder(mContext)
                            .setFieldTrials("")
                            .setEnableVideoHwAcceleration(true)
                            .setEnableInternalTracer(true)
                            .createInitializationOptions())
            mPeerConnectionFactory =
                    PeerConnectionFactory.builder()
                            .createPeerConnectionFactory()

            mLocalPeerConnection = mPeerConnectionFactory?.createPeerConnection(
                    mIceServers, mPeerConnectionConstraints, mPeerObserver)
            mRemotePeerConnection = mPeerConnectionFactory?.createPeerConnection(
                    mIceServers, mPeerConnectionConstraints, mPeerObserver)

            mPeerConnectionInited = true
        }

        createDataChannel(label,
                (RtcRole.RTC_ACCEPTOR == role),
                (RtcRole.RTC_INITIATOR == role))
    }

    fun destroy(destroyPeerConnection: Boolean) {
        mSendDataChannel?.dispose()
        mSendDataChannel = null
        if (destroyPeerConnection) {
            mRemotePeerConnection?.dispose()
            mRemotePeerConnection = null
            mLocalPeerConnection?.dispose()
            mLocalPeerConnection = null
            mPeerConnectionFactory?.dispose()
            PeerConnectionFactory.stopInternalTracingCapture()
            PeerConnectionFactory.shutdownInternalTracer()
            mPeerConnectionInited = false
        }
    }

    fun processMessage(msg: String?) {

    }

    fun processMessage(msg: String?, peerId: String?) {

    }
}