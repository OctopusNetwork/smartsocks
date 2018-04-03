package com.kkt.rtc

import android.content.Context
import org.json.JSONObject
import java.nio.ByteBuffer

/**
 * Created by owen on 18-3-31.
 */
class RtcAgentContainer {
    companion object {
        private var mRtcAgentMap: HashMap<String, RtcAgentWrapper> = HashMap()
        private var mRtcSocketProtectListener: RtcAgent.RtcSocketProtectListener? = null

        val TAG = "RtcAgentContainer"

        enum class RtcAgentDataChannelMessage {
            RTC_DATA_CHANNEL_STATE,
            RTC_DATA_CHANNEL_MESSAGE
        }

        interface RtcAgentDataChannelMessageListener {
            fun onRtcAgentDataChannelMessage(peerId: String,
                                             msg: RtcAgentDataChannelMessage,
                                             info: ByteBuffer?)
            fun onRtcAgentDataChannelMessage(peerId: String,
                                             msg: RtcAgentDataChannelMessage,
                                             info: String?)
        }

        interface RtcAgentCreateChannelListener {
            fun onRtcAgentCreateChannel(peerId: String) : RtcAgentDataChannelMessageListener
        }

        private var mRtcAgentCreateChannelListener : RtcAgentCreateChannelListener? = null

        fun setRtcAgentCreateChannelListener(listener: RtcAgentCreateChannelListener) {
            mRtcAgentCreateChannelListener = listener
        }

        class RtcAgentWrapper(rtcAgent: RtcAgent,
                              listener: RtcAgentDataChannelMessageListener?) {
            var mRtcAgent: RtcAgent = rtcAgent
            var mRtcAgentMessageListener = listener
        }

        fun setRtcSocketProtectListener(listener: RtcAgent.RtcSocketProtectListener) {
            mRtcSocketProtectListener = listener
        }

        fun handleDataChannelMessage(listener: RtcAgent.RtcDataChannelListener,
                                     msg: RtcAgentDataChannelMessage,
                                     info: ByteBuffer?) {
            for ((k, v) in mRtcAgentMap) {
                if (v.mRtcAgent.mRecvDataChannelListener == listener) {
                    v.mRtcAgentMessageListener?.onRtcAgentDataChannelMessage(k, msg, info)
                    return
                }
            }
        }

        fun handleDataChannelMessage(listener: RtcAgent.RtcDataChannelListener,
                                     msg: RtcAgentDataChannelMessage,
                                     info: String?) {
            for ((k, v) in mRtcAgentMap) {
                if (v.mRtcAgent.mRecvDataChannelListener == listener) {
                    v.mRtcAgentMessageListener?.onRtcAgentDataChannelMessage(k, msg, info)
                    return
                }
            }
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
                    rtcAgent = mRtcAgentMap[sourceid]?.mRtcAgent
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
                            handleDataChannelMessage(this,
                                    RtcAgentDataChannelMessage.RTC_DATA_CHANNEL_MESSAGE, byteBuffer)
                        }

                        override fun onStateChange(state: String) {
                            RtcLogging.debug(TAG, "RecvDataChannel" + this + " state: " + state)
                            handleDataChannelMessage(this,
                                    RtcAgentDataChannelMessage.RTC_DATA_CHANNEL_STATE, state)
                        }

                    }

                    RtcLogging.debug(TAG, "SendDataChannelListener: $sendDataChannelListener")
                    RtcLogging.debug(TAG, "RecvDataChannelListener: $recvDataChannelListener")

                    rtcAgent = RtcAgent(context, signallingSender,
                            sendDataChannelListener, recvDataChannelListener,
                            mRtcSocketProtectListener,
                            sourceid, config)
                    rtcAgent.initialize(sourceid, RtcAgent.RtcRole.RTC_ACCEPTOR, rtcMsgJsonObject)
                    mRtcAgentMap[sourceid] = RtcAgentWrapper(rtcAgent,
                            mRtcAgentCreateChannelListener?.onRtcAgentCreateChannel(sourceid))
                    return
                }
            }

            if (null != rtcMsgJsonObject) {
                if (null == rtcAgent) {
                    for ((_, v) in mRtcAgentMap) {
                        v.mRtcAgent.processMessage(rtcMsgJsonObject)
                    }
                } else {
                    rtcAgent.processMessage(rtcMsgJsonObject, sourceid)
                }
            }
        }

        fun delPeer(peer: RtcSignalling.RtcPeer) {
            var rtcAgent = mRtcAgentMap[peer.mPeerID]?.mRtcAgent
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
                    handleDataChannelMessage(this,
                            RtcAgentDataChannelMessage.RTC_DATA_CHANNEL_MESSAGE, byteBuffer)
                }

                override fun onStateChange(state: String) {
                    RtcLogging.debug(TAG, "RecvDataChannel" + this + " state: " + state)
                    handleDataChannelMessage(this,
                            RtcAgentDataChannelMessage.RTC_DATA_CHANNEL_STATE, state)
                }

            }

            RtcLogging.debug(TAG, "SendDataChannelListener: $sendDataChannelListener")
            RtcLogging.debug(TAG, "RecvDataChannelListener: $recvDataChannelListener")

            var rtcAgent = RtcAgent(context, signallingSender,
                    sendDataChannelListener, recvDataChannelListener,
                    mRtcSocketProtectListener,
                    peer?.mPeerID!!, config)
            rtcAgent.initialize(peer.mPeerID, RtcAgent.RtcRole.RTC_INITIATOR)
            mRtcAgentMap[peer.mPeerID] = RtcAgentWrapper(rtcAgent,
                    mRtcAgentCreateChannelListener?.onRtcAgentCreateChannel(peer.mPeerID))
        }

        fun broadcast(msg: String) {
            for ((_, v) in mRtcAgentMap) {
                v.mRtcAgent.send(msg)
            }
        }
    }
}