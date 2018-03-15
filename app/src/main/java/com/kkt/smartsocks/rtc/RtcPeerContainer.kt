package com.kkt.smartsocks.rtc

import android.content.Context
import android.text.TextUtils
import com.android.volley.VolleyError
import com.google.gson.JsonObject
import com.kkt.smartsocks.core.LocalVpnService
import org.json.JSONObject
import java.io.IOException
import java.io.InputStream
import java.io.InputStreamReader
import java.io.OutputStream
import java.net.Socket


/**
 * Created by owen on 18-3-3.
 */
class RtcPeerContainer(context: Context,
                       peerListListener: RtcPeerListListener,
                       peerMsgListener: RtcPeerMessageListener,
                       peerName: String) {
    var mLocalPeerName: String = peerName
    var mLocalPeerId: Long = -1
    val mPeerServer: String = "47.254.28.11:8888"
    val mPeerServerHost: String = "47.254.28.11"
    val mPeerServerPort: Int = 8888

    var mPeerConnected = false
    var mLogout = false

    var mPeerServerSock: Socket? = null

    val mContext: Context = context

    var mPeerList: ArrayList<RtcPeer> = ArrayList()

    var mOutputStream: OutputStream? = null
    var mInputStream: InputStream? = null

    var mPeerClientThread: Thread? = null

    init {
        // mLocalPeerName = Utils.getWIFILocalIpAdress(mContext)
    }

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
                if (-1.toLong() != mPeerContainer.mLocalPeerId) {
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
        var reqString = "GET $query HTTP/1.1\r\n"

        reqString += "User-Agent: Dalvik/2.1.0 (Linux; U; Android 5.0.1; GT-I9500 Build/LRX22C)\r\n"
        reqString += "Host: " + mPeerServer + "\r\n"
        reqString += "Connection: Keep-Alive\r\n"
        reqString += "Accept-Encoding: *\r\n\r\n"

        Thread {
            if (mPeerServerSock == null) {
                mPeerServerSock = Socket(mPeerServerHost, mPeerServerPort)
                LocalVpnService.protectSocket(mPeerServerSock)
            }
            if (mPeerServerSock?.isConnected!!) {
                try {
                    mOutputStream = mPeerServerSock?.getOutputStream()
                    mOutputStream?.write(reqString.toByteArray())
                    mOutputStream?.flush()
                } catch (excp: IOException) {
                    excp.printStackTrace()
                }
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

        mPeerClientThread = Thread {
            do {
                if (mPeerServerSock == null) {
                    Thread.sleep(100)
                    continue
                }

                mInputStream = mPeerServerSock?.getInputStream()

                val isr = InputStreamReader(mInputStream)
                var close: Boolean = false
                var pragmaId: Long = -1
                var body: String? = null
                var buf: CharArray = CharArray(4096)
                var len = 0

                try {
                    len = isr.read(buf)
                    isr.close()
                    if (len < 0) {
                        break;
                    }

                    val httpStr: String = String(buf, 0, len)

                    if (HttpParser.isHttpResponse(httpStr)) {
                        close = HttpParser.shouldBeClose(httpStr)
                        pragmaId = HttpParser.parsePragmaId(httpStr)
                        body = HttpParser.getBody(httpStr)
                    }

                    if (close) {
                        mPeerServerSock?.close()
                        mPeerServerSock = null
                        peerServerGetRequest("/wait?peer_id=" + mLocalPeerId)
                    }

                    if (pragmaId == mLocalPeerId) {
                        val bodyLines = body?.split("\r\n")

                        if (bodyLines != null) {
                            for (line in bodyLines) {
                                var rtcPeer = RtcPeer(line, mLocalPeerName, this)
                                val exist: Boolean = mPeerList.any { it.id == rtcPeer.id }
                                if (!exist) {
                                    mPeerList.add(rtcPeer)
                                }
                            }

                            mRtcPeerListListener?.onUpdated(mPeerList)
                        }
                    } else {
                        if (!TextUtils.isEmpty(body)) {
                            mRtcPeerMsgListener?.onPeerMessage(pragmaId, body)
                        } else {
                            Thread.sleep(100)
                        }
                    }
                } catch (error: IOException) {
                    error.printStackTrace()
                }
            } while (!mLogout)
        }

        mPeerClientThread?.start()
    }

    fun logout() {
        mLogout = true
        peerServerGetRequest("/sign_out")
        Thread.sleep(1000)
        mPeerServerSock?.close()
        mPeerServerSock = null
        mPeerClientThread?.join()
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

    class RtcPeer(peerDesc: String, localPeerName: String, container: RtcPeerContainer) {
        var name: String = ""
        var id: Long = -1
        var connected: Boolean = false
        var address: String = ""

        init {
            val elems = peerDesc.split(",")
            if (elems . size >= 3) {
                name = elems[0]
                address = name
                id = elems[1].toLong()
                if (elems[2].trim().toInt() == 1) {
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