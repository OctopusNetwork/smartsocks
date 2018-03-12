package com.kkt.smartsocks.tunnel.datagram

import com.kkt.smartsocks.tunnel.Config
import org.webrtc.DataChannel

/**
 * Created by owen on 18-3-9.
 */
class DatagramConfig(ID: String, contractHash: String,
                     localConnection: Int, dataChannel: DataChannel) : Config() {
    val mLocalConnection = localConnection
    val mID = ID
    val mContractHash = contractHash
    val mDataChannel = dataChannel
}