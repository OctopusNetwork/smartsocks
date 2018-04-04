package com.kkt.rtc

import android.content.Context
import org.json.JSONObject
import java.nio.ByteBuffer
import java.util.*
import kotlin.collections.HashMap

/**
 * Created by owen on 18-3-23.
 */
class RtcEngine {
    class RtcEngineInitConfig(context: Context, signallingHost: String, signallingPort: Int,
                              myPeerName: String = UUID.randomUUID().toString()) :
            RtcSignalling.RtcSignallingConfig(signallingHost, signallingPort, myPeerName) {
        val mContext: Context = context
    }

    interface RtcSignallingEventListener {
        fun onPeerListUpdate(peerMap: HashMap<String, RtcSignalling.RtcPeer>)
        fun onDelPeer(peer: RtcSignalling.RtcPeer)
        fun onAddPeer(peer: RtcSignalling.RtcPeer)
    }

    companion object {
        private var mRtcEngineInitConfig: RtcEngineInitConfig? = null
        var mRtcSignallingEventListener: RtcSignallingEventListener? = null
        var mPeerMap: HashMap<String, RtcSignalling.RtcPeer> = HashMap()

        var mSignallingRegistered: Boolean = false

        val mRtcIceConfigs: ArrayList<RtcAgent.RtcIceConfig> = ArrayList()

        const val TAG: String = "RtcEngine"

        val mRtcSignallingSender = object: RtcSignalling.RtcSignallingSender {
            override fun sendToAll(data: String) {
                RtcSignalling.sendToAll(data)
            }

            override fun sendToPeer(data: String, peerId: String) {
                RtcSignalling.sendToPeer(peerId, data)
            }
        }

        private val mRtcSignallingListener: RtcSignalling.RtcSignallingListener =
            object : RtcSignalling.RtcSignallingListener {
                override fun onMiscMsg(msgJSONObject: JSONObject) {
                    RtcAgentContainer.processMessage(msgJSONObject, mRtcEngineInitConfig?.mContext,
                            mRtcSignallingSender, RtcAgent.RtcConfig(mRtcIceConfigs))
                }

                override fun onRegistered() {
                    mSignallingRegistered = true
                }

                override fun onPeerList(peerList: ArrayList<RtcSignalling.RtcPeer>) {
                    mPeerMap.clear()
                    for (peer in peerList) {
                        mPeerMap[peer.mPeerID] = peer
                    }
                    mRtcSignallingEventListener?.onPeerListUpdate(mPeerMap)
                }

                override fun onAddPeer(peer: RtcSignalling.RtcPeer) {
                    if (!mPeerMap.contains(peer.mPeerID)) {
                        mPeerMap[peer.mPeerID] = peer
                        mRtcSignallingEventListener?.onAddPeer(peer)
                    }
                }

                override fun onDelPeer(peer: RtcSignalling.RtcPeer) {
                    mPeerMap.remove(peer.mPeerID)
                    mRtcSignallingEventListener?.onDelPeer(peer)
                    RtcAgentContainer.delPeer(peer)
                }

            }

        fun initialize(config: RtcEngineInitConfig,
                       signallingEventListener: RtcSignallingEventListener,
                       createChannelListener: RtcAgentContainer.Companion.RtcAgentCreateChannelListener,
                       protectSocketListener: RtcAgent.RtcSocketProtectListener) {
            mRtcEngineInitConfig = config
            mRtcSignallingEventListener = signallingEventListener

            mRtcIceConfigs.add(RtcAgent.RtcIceConfig(
                    RtcAgent.RtcIceServerType.RTC_ICE_TURN,
                    "192.168.199.199", 3478,
                    "192.168.199.199",
                    "kikakkz", "1qaz2wsx",
                    RtcAgent.RtcIceServerProto.RTC_ICE_PROTO_UDP))
            mRtcIceConfigs.add(RtcAgent.RtcIceConfig(
                    RtcAgent.RtcIceServerType.RTC_ICE_STUN,
                    "192.168.199.199", 3478,
                    "192.168.199.199",
                    "", "",
                    RtcAgent.RtcIceServerProto.RTC_ICE_PROTO_UDP))

            RtcLogging.enableLogging()
            RtcAgentContainer.setRtcSocketProtectListener(protectSocketListener)
            RtcAgentContainer.setRtcAgentCreateChannelListener(createChannelListener)
            RtcSignalling.initialize(config, mRtcSignallingListener)
        }

        fun destroy() {
            RtcSignalling.destroy()
        }

        fun createChannel(peer: RtcSignalling.RtcPeer?) {
            if (peer != null && mRtcEngineInitConfig != null) {
                RtcAgentContainer.createChannel(peer, mRtcEngineInitConfig?.mContext,
                        mRtcSignallingSender, RtcAgent.RtcConfig(mRtcIceConfigs))
            }
        }

        fun broadcast(msg: String) {
            RtcAgentContainer.broadcast(msg)
        }
    }
}