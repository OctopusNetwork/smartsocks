package com.kkt.rtc

import android.content.Context
import org.json.JSONObject
import java.nio.ByteBuffer

/**
 * Created by owen on 18-3-31.
 */
class RtcAgentContainer {
    companion object {
        private var mRtcAgentMap: HashMap<String, RtcAgent> = HashMap()
        private var mRtcSocketProtectListener: RtcAgent.RtcSocketProtectListener? = null

        val TAG = "RtcAgentContainer"

        fun setRtcSocketProtectListener(listener: RtcAgent.RtcSocketProtectListener) {
            mRtcSocketProtectListener = listener
        }

        fun processMessage(msgJSONObject: JSONObject,
                           context: Context?,
                           signallingSender: RtcSignalling.RtcSignallingSender,
                           config: RtcAgent.RtcConfig) {
            if (msgJSONObject.has("targetid") &&
                    msgJSONObject.has("sourceid")) {
                val targetId = msgJSONObject.getString("targetid")
                if (targetId != RtcSignalling.mRtcSignallingConfig?.mMyPeerName) {
                    return
                }
            }

            var rtcAgent: RtcAgent? = null
            var sourceid: String? = null
            var rtcMsgJsonObject: JSONObject? = null

            if (msgJSONObject.has("msg")) {
                rtcMsgJsonObject = JSONObject(
                        msgJSONObject.getString("msg"))
            }

            if (msgJSONObject.has("sourceid")) {
                sourceid = msgJSONObject.getString("sourceid")
                if (mRtcAgentMap.contains(sourceid)) {
                    rtcAgent = mRtcAgentMap[sourceid]
                } else {
                    val sendDataChannelListener = object: RtcAgent.RtcDataChannelListener {
                        override fun onMessage(byteBuffer: ByteBuffer) {

                        }

                        override fun onStateChange(state: String) {
                            RtcLogging.debug(TAG, "SendDataChannel" + this + " state: " + state)
                        }
                    }
                    val recvDataChannelListener = object: RtcAgent.RtcDataChannelListener {
                        override fun onMessage(byteBuffer: ByteBuffer) {

                        }

                        override fun onStateChange(state: String) {
                            RtcLogging.debug(TAG, "RecvDataChannel" + this + " state: " + state)
                        }

                    }

                    RtcLogging.debug(TAG, "SendDataChannelListener: $sendDataChannelListener")
                    RtcLogging.debug(TAG, "RecvDataChannelListener: $recvDataChannelListener")

                    rtcAgent = RtcAgent(context, signallingSender,
                            sendDataChannelListener, recvDataChannelListener,
                            mRtcSocketProtectListener,
                            sourceid, config)
                    rtcAgent.initialize(sourceid, RtcAgent.RtcRole.RTC_ACCEPTOR, rtcMsgJsonObject)
                    mRtcAgentMap[sourceid] = rtcAgent
                    return
                }
            }

            if (null != rtcMsgJsonObject) {
                if (null == rtcAgent) {
                    for ((_, v) in mRtcAgentMap) {
                        v.processMessage(rtcMsgJsonObject)
                    }
                } else {
                    rtcAgent.processMessage(rtcMsgJsonObject, sourceid)
                }
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
                    RtcLogging.debug(TAG, "SendDataChannel" + this + " state: " + state)
                }

            }
            val recvDataChannelListener = object: RtcAgent.RtcDataChannelListener {
                override fun onMessage(byteBuffer: ByteBuffer) {

                }

                override fun onStateChange(state: String) {
                    RtcLogging.debug(TAG, "RecvDataChannel" + this + " state: " + state)
                }

            }

            RtcLogging.debug(TAG, "SendDataChannelListener: $sendDataChannelListener")
            RtcLogging.debug(TAG, "RecvDataChannelListener: $recvDataChannelListener")

            var rtcAgent = RtcAgent(context, signallingSender,
                    sendDataChannelListener, recvDataChannelListener,
                    mRtcSocketProtectListener,
                    peer?.mPeerID!!, config)
            rtcAgent.initialize(peer.mPeerID, RtcAgent.RtcRole.RTC_INITIATOR)
            mRtcAgentMap[peer.mPeerID] = rtcAgent
        }
    }
}