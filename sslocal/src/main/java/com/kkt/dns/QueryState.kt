package com.kkt.dns

/**
 * Created by owen on 18-4-5.
 */
class QueryState {
    var ClientQueryID: Short = 0
    var QueryNanoTime: Long = 0
    var ClientIP: Int = 0
    var ClientPort: Short = 0
    var RemoteIP: Int = 0
    var RemotePort: Short = 0
}