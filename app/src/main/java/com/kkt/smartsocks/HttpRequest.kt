package com.kkt.smartsocks

import android.content.Context
import android.util.Log
import com.android.volley.Request
import com.android.volley.RequestQueue
import com.android.volley.Response
import com.android.volley.toolbox.JsonObjectRequest
import com.android.volley.toolbox.StringRequest
import com.android.volley.toolbox.Volley
import org.json.JSONObject


/**
 * Created by owen on 18-3-3.
 */

class HttpRequest(context: Context, listener: HttpRequestListener) {
    private var mRequestQueue: RequestQueue = Volley.newRequestQueue(context)
    private var mListener: HttpRequestListener = listener

    private val TAG = "HTTPREQ"

    fun get(url: String) {
        val stringRequest = StringRequest(Request.Method.GET, url,
                Response.Listener { response ->
                    mListener?.onStringResponse(response)
                }, Response.ErrorListener { error ->
                    mListener?.onError(error)
                })

        mRequestQueue.add(stringRequest)
    }

    fun post(url: String, body: JSONObject) {
        val jsonRequest = JsonObjectRequest(Request.Method.POST, url, body,
                Response.Listener { response ->
                    Log.d(TAG, response.toString())
                }, Response.ErrorListener { error -> })

        mRequestQueue.add(jsonRequest)
    }
}