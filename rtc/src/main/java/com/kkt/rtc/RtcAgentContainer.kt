package com.kkt.rtc

import android.content.Context
import org.json.JSONObject
import java.nio.ByteBuffer

/**
 * Created by owen on 18-3-31.
 */
class RtcAgentContainer {
    companion object {
        var mRtcAgentMap: HashMap<String, RtcAgent> = HashMap()

        fun processMessage(msg: String?,
                           context: Context?,
                           signallingSender: RtcSignalling.RtcSignallingSender,
                           config: RtcAgent.RtcConfig) {
            val jsonObject = JSONObject(msg)
            if (jsonObject.has("targetid") &&
                    jsonObject.has("sourceid")) {
                val targetId = jsonObject.getString("targetid")
                if (targetId != RtcSignalling.mRtcSignallingConfig?.mMyPeerName) {
                    return
                }
            }

            var rtcAgent: RtcAgent? = null
            var sourceid: String? = null

            if (jsonObject.has("sourceid")) {
                sourceid = jsonObject.getString("sourceid")
                if (!mRtcAgentMap.contains(sourceid)) {
                    rtcAgent = mRtcAgentMap.get(sourceid)
                } else {
                    val sendDataChannelListener = object: RtcAgent.RtcDataChannelListener {
                        override fun onMessage(byteBuffer: ByteBuffer) {

                        }

                        override fun onStateChange(state: String) {

                        }

                    }
                    val recvDataChannelListener = object: RtcAgent.RtcDataChannelListener {
                        override fun onMessage(byteBuffer: ByteBuffer) {

                        }

                        override fun onStateChange(state: String) {

                        }

                    }
                    rtcAgent = RtcAgent(context, signallingSender,
                            sendDataChannelListener, recvDataChannelListener,
                            sourceid, config)
                    rtcAgent.initialize(sourceid, RtcAgent.RtcRole.RTC_ACCEPTOR)
                    mRtcAgentMap[sourceid] = rtcAgent
                }
            }

            if (null == rtcAgent) {
                for ((_, v) in mRtcAgentMap) {
                    v.processMessage(msg)
                }
            } else {
                rtcAgent.processMessage(msg, sourceid)
            }
        }

        fun delPeer(peer: RtcSignalling.RtcPeer) {
            var rtcAgent = mRtcAgentMap[peer.mPeerID]
            mRtcAgentMap.remove(peer.mPeerID)
            rtcAgent?.destroy(mRtcAgentMap.size == 0)
        }

        fun createChannel(peer: RtcSignalling.RtcPeer, context: Context?,
                          signallingSender: RtcSignalling.RtcSignallingSender,
                          config: RtcAgent.RtcConfig) {
            if (mRtcAgentMap.contains(peer.mPeerID)) {
                return
            }

            val sendDataChannelListener = object: RtcAgent.RtcDataChannelListener {
                override fun onMessage(byteBuffer: ByteBuffer) {

                }

                override fun onStateChange(state: String) {

                }

            }
            val recvDataChannelListener = object: RtcAgent.RtcDataChannelListener {
                override fun onMessage(byteBuffer: ByteBuffer) {

                }

                override fun onStateChange(state: String) {

                }

            }
            var rtcAgent = RtcAgent(context, signallingSender,
                    sendDataChannelListener, recvDataChannelListener,
                    peer?.mPeerID!!, config)
            rtcAgent.initialize(peer.mPeerID, RtcAgent.RtcRole.RTC_INITIATOR)
            mRtcAgentMap[peer.mPeerID] = rtcAgent
        }
    }
}