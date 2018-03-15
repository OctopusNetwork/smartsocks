package io.smarttangle.blockchain.model;

import android.app.Activity;
import android.net.VpnService;
import android.widget.Toast;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import io.smarttangle.blockchain.Utils.NetUtils;
import io.smarttangle.blockchain.net.SocketClient;

/**
 * Created by haijun on 2018/3/7.
 */

public class BCRequest {

    private static VpnService vpnService ;

    public interface Listener<T> {
        public void onResponse(T response);
    }

    public interface ErrorListener {
        public void onError(BCErrorEntity error);
    }

    public static VpnService getVpnService() {
        return vpnService;
    }

    public static void setVpnService(VpnService service) {
        vpnService = service;
    }

    private static void dispenseResponse(final Activity context, final Entity entity, final Listener responseListener, final ErrorListener
            errorListener) {
        context.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                if (entity == null) {
                    Toast.makeText(context, "Response Data Error!", Toast.LENGTH_LONG).show();
                    return;
                }
                if (entity instanceof BCErrorEntity) {
                    if (errorListener == null) {
                        String messages = null;
                        BCErrorEntity errorEntity = (BCErrorEntity) entity;
                        if (errorEntity.getStatus() == 200) {
                            messages = errorEntity.getError().getMessage();
                        } else {
                            messages = "Network Error ,Status :" + errorEntity.getStatus();
                        }
                        Toast.makeText(context, messages, Toast.LENGTH_LONG).show();
                    } else {
                        errorListener.onError((BCErrorEntity) entity);
                    }
                } else {
                    responseListener.onResponse(entity);
                }
            }
        });
    }

    public static RPCEntity getBalance(final Activity context, final String address, final Listener responseListener, final ErrorListener
            errorListener) {
        new Thread(new Runnable() {
            @Override
            public void run() {
                SocketClient client = new SocketClient(NetUtils.URL, vpnService, true);
                try {
                    List<String> params = new ArrayList<>();
                    params.add(address);
                    params.add("latest");
                    String postData = client.buildPostData("eth_getBalance", params, null, "1");
                    final Entity entity = client.call(postData, RPCEntity.class);
                    dispenseResponse(context, entity, responseListener, errorListener);
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }).start();

        return null;
    }

    public static RPCEntity newAccount(final Activity context, final String password, final Listener responseListener, final ErrorListener
            errorListener) {
        new Thread(new Runnable() {
            @Override
            public void run() {
                SocketClient client = new SocketClient(NetUtils.URL, vpnService, true);
                try {
                    List<String> params = new ArrayList<>();
                    params.add(password);
                    String postData = client.buildPostData("personal_newAccount", params, null, "1");
                    final Entity entity = client.call(postData, RPCEntity.class);
                    dispenseResponse(context, entity, responseListener, errorListener);
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }).start();

        return null;
    }

    public static RPCEntity getZonesProxys(final Activity context, final String zone, final Listener responseListener, final ErrorListener
            errorListener) {
        new Thread(new Runnable() {
            @Override
            public void run() {
                SocketClient client = new SocketClient(NetUtils.URL, vpnService, true);
                try {
                    List<String> params = new ArrayList<>();
                    String postData = client.buildPostData("eth_getProxys", params, null, "1");
                    final Entity entity = client.call(postData, PeerEntity.class);
                    dispenseResponse(context, entity, responseListener, errorListener);
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }).start();

        return null;
    }

    public static void registerProxy(final Activity context, final String ip, final String address, final Listener responseListener, final ErrorListener
            errorListener) {
        new Thread(new Runnable() {
            @Override
            public void run() {
                SocketClient client = new SocketClient(NetUtils.URL, vpnService, true);
                try {
                    List<String> params = new ArrayList<>();
                    JSONObject jsonObject = new JSONObject();
                    try {
                        jsonObject.put("ip", ip);
                        jsonObject.put("address", address);
                    } catch (JSONException e) {
                        e.printStackTrace();
                    }
                    String postData = client.buildPostData("eth_registerProxy", params, jsonObject.toString(), "1");
                    final Entity entity = client.call(postData, RPCEntity.class);
                    dispenseResponse(context, entity, responseListener, errorListener);
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }).start();
    }


    public static void transaction(final Activity context, final String from, final String to, final String value, final String password, final Listener
            responseListener, final ErrorListener
                                           errorListener) {
        new Thread(new Runnable() {
            @Override
            public void run() {
                SocketClient client = new SocketClient(NetUtils.URL, vpnService, true);
                try {
                    List<String> params = new ArrayList<>();
                    JSONObject jsonObject = new JSONObject();
                    try {
                        jsonObject.put("from", from);
                        jsonObject.put("to", to);
                        jsonObject.put("value", value);
                    } catch (JSONException e) {
                        e.printStackTrace();
                    }
                    params.add(password);
                    String postData = client.buildPostData("personal_signAndSendTransaction", params, jsonObject.toString(), "1");
                    final Entity entity = client.call(postData, RPCEntity.class);
                    dispenseResponse(context, entity, responseListener, errorListener);
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }).start();
    }


    //"0xbc0067fbdba5d1b46757bb9b887701a8d38d01a5e3c7c378d85f40c938796908"
    public static void getReceiptInfo(final Activity context, final String txHash, final Listener
            responseListener, final ErrorListener
                                              errorListener) {
        new Thread(new Runnable() {
            @Override
            public void run() {
                SocketClient client = new SocketClient(NetUtils.URL, vpnService, true);
                try {
                    List<String> params = new ArrayList<>();
                    params.add(txHash);
                    String postData = client.buildPostData("eth_getTransactionReceipt", params, null, "1");
                    final Entity entity = client.call(postData, ReceiptEntity.class);
                    dispenseResponse(context, entity, responseListener, errorListener);
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }).start();
    }

}
