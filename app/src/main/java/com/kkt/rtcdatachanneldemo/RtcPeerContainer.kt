package com.kkt.rtcdatachanneldemo

import android.content.Context
import android.text.TextUtils
import android.util.Log
import com.android.volley.VolleyError
import com.android.volley.toolbox.HttpHeaderParser
import com.google.gson.JsonObject
import org.json.JSONObject
import java.io.BufferedReader
import java.io.InputStream
import java.io.InputStreamReader
import java.io.OutputStream
import java.net.Socket


/**
 * Created by owen on 18-3-3.
 */
class RtcPeerContainer(context: Context,
                       peerListListener: RtcPeerListListener,
                       peerMsgListener: RtcPeerMessageListener) {
    val mLocalPeerName: String = "12345678"
    var mLocalPeerId: Long = -1
    val mPeerServer: String = "192.168.198.180:8888"
    val mPeerServerHost: String = "192.168.198.180"
    val mPeerServerPort: Int = 8888

    var mPeerConnected = false
    var mLogout = false

    var mPeerServerSock: Socket? = null

    val mContext: Context = context

    var mPeerList: ArrayList<RtcPeer> = ArrayList()

    var mOutputStream: OutputStream? = null
    var mInputStream: InputStream? = null

    class RtcPeerListener(peerList: ArrayList<RtcPeer>,
                          peerListListener: RtcPeerListListener,
                          container: RtcPeerContainer)
        : HttpRequestListener {
        var mPeerList = peerList
        var mPeerListListener = peerListListener
        var mPeerContainer = container

        override fun onStringResponse(response: String) {
            val lines = response.split("\n")
            mPeerList.clear()
            for (line in lines) {
                if (!TextUtils.isEmpty(line)) {
                    val peer = RtcPeer(line,
                            mPeerContainer.mLocalPeerName,
                            mPeerContainer)
                    if (!mPeerContainer.mLocalPeerName.equals(peer.name)) {
                        mPeerList.add(peer)
                    }
                }
            }

            if (!mPeerContainer.mPeerConnected) {
                if (!(-1).equals(mPeerContainer.mLocalPeerId)) {
                    mPeerContainer.peerServerGetRequest(
                            "/wait?peer_id=" +
                                    mPeerContainer.mLocalPeerId)
                    mPeerContainer.mPeerConnected = true
                }
            }

            mPeerListListener?.onUpdated(mPeerList)
        }

        override fun onJsonObjectResponse(response: JsonObject) {

        }

        override fun onError(error: VolleyError) {

        }

    }

    var mRtcPeerListListener: RtcPeerListListener = peerListListener
    var mRtcPeerMsgListener: RtcPeerMessageListener = peerMsgListener
    val mRtcPeerListener: RtcPeerListener = RtcPeerListener(
            mPeerList, mRtcPeerListListener, this)

    private fun peerServerGetRequest(query: String) {
        var reqString = "GET " + query + " HTTP/1.1\r\n"

        reqString += "User-Agent: Dalvik/2.1.0 (Linux; U; Android 5.0.1; GT-I9500 Build/LRX22C)\r\n"
        reqString += "Host: " + mPeerServer + "\r\n"
        reqString += "Connection: Keep-Alive\r\n"
        reqString += "Accept-Encoding: *\r\n\r\n"

        Thread {
            if (mPeerServerSock == null) {
                mPeerServerSock = Socket(mPeerServerHost, mPeerServerPort)
            }
            if (mPeerServerSock?.isConnected()!!) {
                mOutputStream = mPeerServerSock?.getOutputStream()
                mOutputStream?.write(reqString.toByteArray())
                mOutputStream?.flush()
            }
        }.start()
    }

    private fun peerServerClose() {
        mPeerServerSock?.close()
    }

    fun login() {
        val loginString: String = "http://" + mPeerServer +
                "/sign_in?" + mLocalPeerName
        HttpRequest(mContext, mRtcPeerListener).get(loginString)

        Thread {
            do {
                if (mPeerServerSock == null) {
                    Thread.sleep(100)
                    continue
                }

                mInputStream = mPeerServerSock?.getInputStream()
                val isr = InputStreamReader(mInputStream)
                val br = BufferedReader(isr)
                var line: String? = null
                var bodyBeginning: Boolean = false
                var close: Boolean = false
                var pragmaId: Long = -1
                var body: String? = ""

                while (br.ready()) {
                    line = br.readLine()

                    if (TextUtils.isEmpty(line)) {
                        bodyBeginning = true
                        continue
                    }

                    if (line.contains("Connection:") &&
                            line.contains("close")) {
                        close = true
                    }

                    if (line.contains("Pragma: ")) {
                        pragmaId = line.substring("Pragma: ".length).toLong()
                    }

                    if (!bodyBeginning) {
                        continue
                    }

                    if (pragmaId == mLocalPeerId) {
                        var rtcPeer = RtcPeer(line, mLocalPeerName, this)

                        val exist: Boolean = mPeerList.any { it.id == rtcPeer.id }

                        if (!exist) {
                            mPeerList.add(rtcPeer)
                        }
                    } else {
                        // Message from another peer
                        body += line
                    }
                }

                if (close) {
                    mPeerServerSock?.close()
                    mPeerServerSock = null
                    peerServerGetRequest("/wait?peer_id=" + mLocalPeerId)
                }

                if (pragmaId == mLocalPeerId) {
                    mRtcPeerListListener?.onUpdated(mPeerList)
                } else {
                    if (!TextUtils.isEmpty(body)) {
                        mRtcPeerMsgListener?.onPeerMessage(pragmaId, body)
                    } else {
                        Thread.sleep(100)
                    }
                }
            } while (!mLogout)
        }.start()
    }

    fun logout() {
        mLogout = true
        peerServerClose()
    }

    fun sendToPeer(body: String, peerId: Long) {
        val sendString: String = "http://" + mPeerServer +
                "/message?peer_id=" + mLocalPeerId +
                "&to=" + peerId
        HttpRequest(mContext, mRtcPeerListener).
                post(sendString, JSONObject(body))
    }

    fun getPeerList(): ArrayList<RtcPeer> {
        return mPeerList
    }

    class RtcPeer(peerDesc: String, localPeerName: String, container:RtcPeerContainer) {
        var name: String = ""
        var id: Long = -1
        var connected: Boolean = false

        init {
            val elems = peerDesc.split(",")
            if (elems . size >= 3) {
                name = elems[0]
                id = elems[1].toLong()
                if (elems[2].toInt() == 1) {
                    connected = true
                }

                if (name.equals(localPeerName)) {
                    container.mLocalPeerId = id
                }
            }
        }

        override fun toString(): String {
            return name + "," + id + "," + connected;
        }
    }

    interface RtcPeerListListener {
        fun onUpdated(peerList: ArrayList<RtcPeer>)
    }

    interface RtcPeerMessageListener {
        fun onPeerMessage(peerId: Long, message: String?)
    }
}