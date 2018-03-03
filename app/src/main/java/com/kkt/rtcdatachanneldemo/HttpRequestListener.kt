package com.kkt.rtcdatachanneldemo

import com.android.volley.VolleyError
import com.google.gson.JsonObject

/**
 * Created by owen on 18-3-3.
 */
interface HttpRequestListener {
    fun onStringResponse(response: String)
    fun onJsonObjectResponse(response: JsonObject)
    fun onError(error: VolleyError)
}