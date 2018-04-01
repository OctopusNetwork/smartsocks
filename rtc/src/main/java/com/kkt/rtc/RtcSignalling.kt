package com.kkt.rtc

import de.tavendo.autobahn.WebSocket
import de.tavendo.autobahn.WebSocketConnection
import org.json.JSONException
import org.json.JSONObject
import java.net.URI


/**
 * Created by owen on 18-3-23.
 */
class RtcSignalling {

    open class RtcSignallingConfig(signallingHost: String, signallingPort: Int,
                              myPeerName: String, roomId: String = "smartsocks") {
        val mRtcSignallingHost = signallingHost
        val mRtcSignallingPort = signallingPort
        val mRoomId = roomId
        val mMyPeerName = myPeerName
    }

    interface RtcSignallingListener {
        fun onRegistered()
        fun onPeerList(peerList: ArrayList<RtcPeer>)
        fun onAddPeer(peer: RtcPeer)
        fun onDelPeer(peer: RtcPeer)
        fun onMiscMsg(msg: String?)
    }

    interface RtcSignallingSender {
        fun sendToAll(data: String)
        fun sendToPeer(data: String, peerId: String)
    }

    class RtcPeer(peerID: String) {
        var mPeerID: String = peerID
    }

    companion object {
        var mRtcSignallingConfig: RtcSignallingConfig? = null
        private var mWebSocketConnection: WebSocketConnection? = null
        private var mRtcSignallingListener: RtcSignallingListener? = null
        private enum class RtcSignallingState {
            IDLE, CONNECTING, CONNECTED, REGISTERED, DISCONNECTED, CLOSED
        }
        private var mSignallingState: RtcSignallingState =
                RtcSignallingState.IDLE
        private const val TAG: String = "RTCSignalling"

        private fun register() {
            val json = JSONObject()
            json.put("cmd", "register")
            json.put("roomid", mRtcSignallingConfig?.mRoomId)
            json.put("clientid", mRtcSignallingConfig?.mMyPeerName)
            mWebSocketConnection?.sendTextMessage(json.toString())
            mSignallingState = RtcSignallingState.REGISTERED
            mRtcSignallingListener?.onRegistered()
        }

        private fun processPeerList(peerListStr: String) {
            val str = peerListStr.substring(1, peerListStr.length - 1)
            val peerList: List<String> = str.split(",")
            var rtcPeerList: ArrayList<RtcPeer> = ArrayList()

            peerList.filter { it.replace("\"", "") !=
                    mRtcSignallingConfig?.mMyPeerName }
                    .mapTo(rtcPeerList) { RtcPeer(it.replace("\"", "")) }

            if (rtcPeerList.size > 0) {
                mRtcSignallingListener?.onPeerList(rtcPeerList)
            }
        }

        private fun processAddPeer(peerID: String) {
            if (peerID.replace("\"", "") !=
                    mRtcSignallingConfig?.mMyPeerName) {
                mRtcSignallingListener?.onAddPeer(RtcPeer(
                        peerID.replace("\"", "")))
            }
        }

        private fun processDelPeer(peerID: String) {
            if (peerID.replace("\"", "") !=
                    mRtcSignallingConfig?.mMyPeerName) {
                mRtcSignallingListener?.onDelPeer(
                        RtcPeer(peerID.replace("\"", "")))
            }
        }

        private fun processSignallingCommand(cmdJson: String?) : Boolean {
            try {
                val jsonObject = JSONObject(cmdJson)
                if (jsonObject.has("msg") &&
                        jsonObject.has("error")) {
                    if (jsonObject.getString("error") == "") {
                        val msgJsonStr = jsonObject.getString("msg")
                        val msgJSONObject = JSONObject(msgJsonStr)
                        if (msgJSONObject.has("cmd")) {
                            val cmd = msgJSONObject.getString("cmd")
                            when (cmd) {
                                "peer_list" -> processPeerList(msgJSONObject.getString("peers"))
                                "add_peer" -> processAddPeer(msgJSONObject.getString("id"))
                                "del_peer" -> processDelPeer(msgJSONObject.getString("id"))
                                else -> { RtcLogging.debug(TAG, "Unknow command: " + cmdJson); return false }
                            }
                        }
                    }
                }
            } catch (excp: JSONException) {
                excp.printStackTrace()
                return false
            }

            return true
        }

        fun initialize(config: RtcSignallingConfig, listener: RtcSignallingListener) {
            if (RtcSignallingState.CONNECTED == mSignallingState ||
                    RtcSignallingState.CONNECTING == mSignallingState) {
                return
            }
            mRtcSignallingConfig = config
            mRtcSignallingListener = listener
            mWebSocketConnection = WebSocketConnection()
            val connectUri = "ws://" + config.mRtcSignallingHost + ":" +
                    config.mRtcSignallingPort + "/ws"
            mSignallingState = RtcSignallingState.CONNECTING
            mWebSocketConnection?.connect(URI(connectUri), object: WebSocket.WebSocketConnectionObserver {
                override fun onBinaryMessage(p0: ByteArray?) {
                }

                override fun onRawTextMessage(p0: ByteArray?) {
                }

                override fun onOpen() {
                    mSignallingState = RtcSignallingState.CONNECTED
                    register()
                }

                override fun onClose(code: WebSocket.WebSocketConnectionObserver.WebSocketCloseNotification?, reason: String?) {
                    mSignallingState = RtcSignallingState.CLOSED
                }

                override fun onTextMessage(payload: String?) {
                    if (processSignallingCommand(payload?.trim())) {
                        return
                    }

                    // Command processor cannot parse, let upper layer to parse
                    mRtcSignallingListener?.onMiscMsg(payload)
                }
            })
        }

        fun destroy() {
            if (RtcSignallingState.DISCONNECTED == mSignallingState ||
                    RtcSignallingState.IDLE == mSignallingState) {
                return
            }

            if (RtcSignallingState.REGISTERED == mSignallingState) {
                mSignallingState = RtcSignallingState.CONNECTED

                val json = JSONObject()
                json.put("cmd", "send")
                json.put("msg", "{\"type\": \"bye\"}")
                mWebSocketConnection?.sendTextMessage(json.toString())

                val deleteUrl = "http://" + mRtcSignallingConfig?.mRtcSignallingHost +
                        ":" + mRtcSignallingConfig?.mRtcSignallingPort +
                        "/" + mRtcSignallingConfig?.mRoomId +
                        "/" + mRtcSignallingConfig?.mMyPeerName
                val httpConnection = AsyncHttpURLConnection("DELETE", deleteUrl, "",
                        object : AsyncHttpURLConnection.AsyncHttpEvents {
                    override fun onHttpError(errorMessage: String) {
                        RtcLogging.debug(TAG, "Delete client session error " + errorMessage)
                    }

                    override fun onHttpComplete(response: String) {}
                })
                httpConnection.send()
            }

            if (RtcSignallingState.CONNECTED == mSignallingState) {
                mWebSocketConnection?.disconnect()
                mSignallingState = RtcSignallingState.DISCONNECTED
            }

            if (RtcSignallingState.CLOSED != mSignallingState) {
                mSignallingState = RtcSignallingState.CLOSED
            }
        }

        fun sendToPeer(peerID: String?, msg: String?) {
            if (mSignallingState != RtcSignallingState.CONNECTED) {
                return
            }
            val json = JSONObject()
            json.put("cmd", "send")
            json.put("targetid", peerID)
            json.put("sourceid", mRtcSignallingConfig?.mMyPeerName)
            json.put("msg", msg)
            mWebSocketConnection?.sendTextMessage(json.toString())
        }

        fun sendToAll(msg: String?) {
            if (mSignallingState != RtcSignallingState.CONNECTED) {
                return
            }
            val json = JSONObject()
            json.put("cmd", "send")
            json.put("sourceid", mRtcSignallingConfig?.mMyPeerName)
            json.put("msg", msg)
            mWebSocketConnection?.sendTextMessage(json.toString())
        }
    }
}