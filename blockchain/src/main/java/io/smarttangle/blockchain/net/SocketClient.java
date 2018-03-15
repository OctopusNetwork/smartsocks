package io.smarttangle.blockchain.net;

import android.net.VpnService;
import android.util.Log;

import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.Socket;
import java.net.URL;
import java.util.List;

import io.smarttangle.blockchain.model.BCErrorEntity;
import io.smarttangle.blockchain.model.Entity;

/**
 * Created by haijun on 2018/3/6.
 */

public class SocketClient {

    private final String END = "\r\n";
    private Socket socket;
    private InputStream is;
    private OutputStream os;
    private String host;
    private String path;
    private int port;

    public SocketClient(String url, VpnService vpnService, boolean byPass) {
        try {
            URL uri = new URL(url);
            host = uri.getHost();
            path = uri.getPath();
            port = uri.getPort();
            socket = new VPNSocket(host, port, vpnService, byPass);
            socket.setSoTimeout(20000);
            is = socket.getInputStream();
            os = socket.getOutputStream();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public String buildPostData(String method, List<String> paramsList, String jsonParams, String id) {
        StringBuffer params = new StringBuffer();
        params.append("{\"jsonrpc\":\"2.0\",\"method\":\"");
        params.append(method);
        params.append("\",\"params\":[");
        if (jsonParams != null) {
            params.append(jsonParams);
            params.append(",");
        }
        for (String param : paramsList) {
            params.append("\"");
            params.append(param);
            params.append("\"");
            params.append(",");
        }
        if (paramsList.size() > 0 || jsonParams != null) {
            params.deleteCharAt(params.length() - 1);
        }
        params.append("],\"id\":");
        params.append(id);
        params.append("}");
        return params.toString();
    }

    public <T extends Entity> T call(String postData, Class<T> clazz) throws IOException {
        StringBuffer sb = new StringBuffer();
        sb.append("POST " + path + " HTTP/1.1" + END);
        sb.append("Host: " + host + ":" + port + END);
        sb.append("User-Agent:Android TangleVPN" + END);
        sb.append("Accept-Language:zh-cn" + END);
        sb.append("Accept-Encoding:deflate" + END);
        sb.append("Cache-Control: no-cache" + END);
        sb.append("Accept: application/json" + END);
        sb.append("Connection:Keep-Alive" + END);
        sb.append("Content-Type: application/json; charset=UTF-8" + END);
        sb.append("Content-Length: " + postData.length() + END);
        sb.append(END);
        sb.append(postData);
        String requestString = sb.toString();
        Log.d("SocketClient", requestString);
        os.write(requestString.getBytes());
        os.flush();

        byte[] rBuf = new byte[3000];
        int len = is.read(rBuf);
        String response = new String(rBuf).substring(0, len);
        is.close();
        socket.close();

        T object = null;
        if (response.startsWith("HTTP/1.1 200")) {
            int bodyIndex = response.indexOf(END + END);
            String body = response.substring(bodyIndex + 4, len - 1);
            Log.d("SocketClient", body);
            ObjectMapper mapper = new ObjectMapper();
            try {
                if (body.indexOf("error") > 0) {
                    BCErrorEntity errorEnrity = mapper.readValue(body, BCErrorEntity.class);
                    errorEnrity.setStatus(200);
                    object = (T) errorEnrity;
                } else {
                    object = mapper.readValue(body, clazz);
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        } else {
            BCErrorEntity errorEnrity = new BCErrorEntity();
            int index = response.indexOf(END);
            String line = response.substring(0, index);
            String[] element = line.split(" ");
            String status = element[1];
            errorEnrity.setStatus(Integer.parseInt(status));
            object = (T) errorEnrity;
        }
        return object;
    }

}
