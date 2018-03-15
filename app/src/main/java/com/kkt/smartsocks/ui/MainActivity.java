package com.kkt.smartsocks.ui;

import android.annotation.SuppressLint;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.DialogInterface;
import android.content.DialogInterface.OnClickListener;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.support.v7.app.AppCompatActivity;
import android.text.InputType;
import android.text.TextUtils;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.CompoundButton;
import android.widget.CompoundButton.OnCheckedChangeListener;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.ScrollView;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;

import com.google.zxing.integration.android.IntentIntegrator;
import com.google.zxing.integration.android.IntentResult;
import com.kkt.smartsocks.R;
import com.kkt.smartsocks.core.AppInfo;
import com.kkt.smartsocks.core.AppProxyManager;
import com.kkt.smartsocks.core.LocalVpnService;
import com.kkt.smartsocks.core.ProxyConfig;
import com.kkt.smartsocks.rtc.PeerListAdapter;
import com.kkt.smartsocks.rtc.RtcPeerContainer;
import com.kkt.smartsocks.rtc.SSProxyServer;
import com.kkt.smartsocks.rtc.Utils;
import com.kkt.smartsocks.tunnel.datagram.DatagramTunnel;

import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.Calendar;

import io.smarttangle.blockchain.Utils.NetUtils;
import io.smarttangle.blockchain.Utils.StorageKey;
import io.smarttangle.blockchain.model.BCErrorEntity;
import io.smarttangle.blockchain.model.BCRequest;
import io.smarttangle.blockchain.model.PeerEntity;
import io.smarttangle.blockchain.model.RPCEntity;
import io.smarttangle.blockchain.model.ReceiptEntity;
import io.smarttangle.blockchain.model.UserStorageUtils;
import io.smarttangle.blockchain.ui.DialogUtils;
import io.smarttangle.blockchain.ui.LoginActivity;
import io.smarttangle.blockchain.ui.MineActivity;
import io.smarttangle.blockchain.ui.ProxyZonesActivity;
import io.smarttangle.blockchain.ui.TransactionActivity;

public class MainActivity extends AppCompatActivity implements
        View.OnClickListener,
        OnCheckedChangeListener,
        LocalVpnService.onStatusChangedListener {

    private static String GL_HISTORY_LOGS;

    private static final String TAG = MainActivity.class.getSimpleName();

    private static final String CONFIG_URL_KEY = "CONFIG_URL_KEY";

    private static final int START_VPN_SERVICE_REQUEST_CODE = 1985;
    private static final int ACTIVITY_LOGIN_REQUEST_CODE = 1001;

    private Switch switchProxy;
    private TextView textViewLog;
    private ScrollView scrollViewLog;
    private TextView textViewProxyUrl, textViewProxyApp;
    private Calendar mCalendar;

    private SSProxyServer mSSProxyServer;
    public String address;
    public boolean isRegistProxy;
    private int peerIndex;
    private QueryRunnable queryRunnable = new QueryRunnable();
    private Handler queryHandler = new Handler();
    private Dialog loadingDialog;
    private Handler mHandler = new Handler() {

    };

    private ListView mPeerListView;
    private ArrayList<RtcPeerContainer.RtcPeer> mPeerList = new ArrayList<>();
    private PeerListAdapter mPeerListAdapter;

    @SuppressLint("WrongViewCast")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        scrollViewLog = (ScrollView) findViewById(R.id.scrollViewLog);
        textViewLog = (TextView) findViewById(R.id.textViewLog);
        findViewById(R.id.ProxyUrlLayout).setOnClickListener(this);
        findViewById(R.id.AppSelectLayout).setOnClickListener(this);

        textViewProxyUrl = (TextView) findViewById(R.id.textViewProxyUrl);
        String ProxyUrl = readProxyUrl();
        if (TextUtils.isEmpty(ProxyUrl)) {
            textViewProxyUrl.setText(R.string.config_not_set_value);
        } else {
            textViewProxyUrl.setText(ProxyUrl);
        }

        mPeerListView = (ListView) findViewById(R.id.peerListView);
        mPeerListAdapter = new PeerListAdapter(this, mPeerList);
        mPeerListView.setAdapter(mPeerListAdapter);
        mPeerListView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> adapterView, View view, int i, long l) {
                peerIndex = i;
                Intent intent;
                intent = new Intent(MainActivity.this, TransactionActivity.class);
                intent.putExtra(TransactionActivity.TO_ADDRESS, mPeerList.get(i).getName());
                startActivity(intent);
            }
        });

        GL_HISTORY_LOGS = "No online peers available\n";
        textViewLog.setText(GL_HISTORY_LOGS);
        scrollViewLog.fullScroll(ScrollView.FOCUS_DOWN);

        mCalendar = Calendar.getInstance();
        LocalVpnService.addOnStatusChangedListener(this);

        //Pre-App Proxy
        if (AppProxyManager.isLollipopOrAbove) {
            new AppProxyManager(this);
            textViewProxyApp = (TextView) findViewById(R.id.textViewAppSelectDetail);
        } else {
            ((ViewGroup) findViewById(R.id.AppSelectLayout).getParent()).removeView(findViewById(R.id.AppSelectLayout));
            ((ViewGroup) findViewById(R.id.textViewAppSelectLine).getParent()).removeView(findViewById(R.id.textViewAppSelectLine));
        }

        mSSProxyServer = new SSProxyServer(this);
        mSSProxyServer.start();
    }


    String readProxyUrl() {

        return "ss://aes-256-cfb:1qaz3wsx@192.168.0.1:10999";
        //        SharedPreferences preferences = getSharedPreferences("shadowsocksProxyUrl", MODE_PRIVATE);
        //        return preferences.getString(CONFIG_URL_KEY, "");
    }

    void setProxyUrl(String ProxyUrl) {
        SharedPreferences preferences = getSharedPreferences("shadowsocksProxyUrl", MODE_PRIVATE);
        Editor editor = preferences.edit();
        editor.putString(CONFIG_URL_KEY, ProxyUrl);
        editor.apply();
    }

    String getVersionName() {
        PackageManager packageManager = getPackageManager();
        if (packageManager == null) {
            Log.e(TAG, "null package manager is impossible");
            return null;
        }

        try {
            return packageManager.getPackageInfo(getPackageName(), 0).versionName;
        } catch (PackageManager.NameNotFoundException e) {
            Log.e(TAG, "package not found is impossible", e);
            return null;
        }
    }

    boolean isValidUrl(String url) {
        try {
            if (url == null || url.isEmpty())
                return false;

            if (url.startsWith("ss://")) {//file path
                return true;
            } else { //url
                Uri uri = Uri.parse(url);
                if (!"http".equals(uri.getScheme()) && !"https".equals(uri.getScheme()))
                    return false;
                if (uri.getHost() == null)
                    return false;
            }
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    @Override
    public void onClick(View v) {
        if (switchProxy.isChecked()) {
            return;
        }

        if (v.getTag().toString().equals("ProxyUrl")) {
            new AlertDialog.Builder(this)
                    .setTitle(R.string.config_url)
                    .setItems(new CharSequence[] {
                            getString(R.string.config_url_scan),
                            getString(R.string.config_url_manual)
                    }, new OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialogInterface, int i) {
                            switch (i) {
                                case 0:
                                    scanForProxyUrl();
                                    break;
                                case 1:
                                    showProxyUrlInputDialog();
                                    break;
                            }
                        }
                    })
                    .show();
        } else if (v.getTag().toString().equals("AppSelect")) {
            System.out.println("abc");
            startActivity(new Intent(this, AppManager.class));
        }
    }

    private void scanForProxyUrl() {
        new IntentIntegrator(this)
                .setPrompt(getString(R.string.config_url_scan_hint))
                .initiateScan(IntentIntegrator.QR_CODE_TYPES);
    }

    private void showProxyUrlInputDialog() {
        final EditText editText = new EditText(this);
        editText.setInputType(InputType.TYPE_TEXT_VARIATION_URI);
        editText.setHint(getString(R.string.config_url_hint));
        editText.setText(readProxyUrl());

        new AlertDialog.Builder(this)
                .setTitle(R.string.config_url)
                .setView(editText)
                .setPositiveButton(R.string.btn_ok, new OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        if (editText.getText() == null) {
                            return;
                        }

                        String ProxyUrl = editText.getText().toString().trim();
                        if (isValidUrl(ProxyUrl)) {
                            setProxyUrl(ProxyUrl);
                            textViewProxyUrl.setText(ProxyUrl);
                        } else {
                            Toast.makeText(MainActivity.this, R.string.err_invalid_url, Toast.LENGTH_SHORT).show();
                        }
                    }
                })
                .setNegativeButton(R.string.btn_cancel, null)
                .show();
    }

    @SuppressLint("DefaultLocale")
    @Override
    public void onLogReceived(String logString) {
        mCalendar.setTimeInMillis(System.currentTimeMillis());
        logString = String.format("[%1$02d:%2$02d:%3$02d] %4$s\n",
                mCalendar.get(Calendar.HOUR_OF_DAY),
                mCalendar.get(Calendar.MINUTE),
                mCalendar.get(Calendar.SECOND),
                logString);

        System.out.println(logString);

        if (textViewLog.getLineCount() > 200) {
            textViewLog.setText("");
        }
        textViewLog.append(logString);
        scrollViewLog.fullScroll(ScrollView.FOCUS_DOWN);
        GL_HISTORY_LOGS = textViewLog.getText() == null ? "" : textViewLog.getText().toString();
    }

    @Override
    public void onStatusChanged(String status, Boolean isRunning) {
        // switchProxy.setEnabled(true);
        // switchProxy.setChecked(isRunning);
        onLogReceived(status);
        Toast.makeText(this, status, Toast.LENGTH_SHORT).show();
    }

    private void startVPN() {
        Intent intent = LocalVpnService.prepare(MainActivity.this);
        if (intent == null) {
            startVPNService();
        } else {
            startActivityForResult(intent, START_VPN_SERVICE_REQUEST_CODE);
        }
    }

    @Override
    public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
        if (LocalVpnService.IsRunning != isChecked) {
            switchProxy.setEnabled(false);
            if (isChecked) {
                if (ProxyConfig.Instance.enableRtcTunnel) {

                } else {
                    startVPN();
                }
            } else {
                LocalVpnService.IsRunning = false;
                if (ProxyConfig.Instance.enableRtcTunnel) {
                    DatagramTunnel.destroyPeerConnection();
                }
            }
        }
    }

    private void startVPNService() {
        String ProxyUrl = readProxyUrl();
        if (!isValidUrl(ProxyUrl)) {
            Toast.makeText(this, R.string.err_invalid_url, Toast.LENGTH_SHORT).show();
            switchProxy.post(new Runnable() {
                @Override
                public void run() {
                    switchProxy.setChecked(false);
                    switchProxy.setEnabled(true);
                }
            });
            return;
        }

        textViewLog.setText("");
        GL_HISTORY_LOGS = null;
        onLogReceived("starting...");
        LocalVpnService.ProxyUrl = ProxyUrl;
        startService(new Intent(this, LocalVpnService.class));
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent intent) {
        if (requestCode == START_VPN_SERVICE_REQUEST_CODE) {
            if (resultCode == RESULT_OK) {
                startVPNService();
            } else {
                switchProxy.setChecked(false);
                switchProxy.setEnabled(true);
                onLogReceived("canceled.");
            }
            return;
        }

        IntentResult scanResult = IntentIntegrator.parseActivityResult(requestCode, resultCode, intent);
        if (scanResult != null) {
            String ProxyUrl = scanResult.getContents();
            if (isValidUrl(ProxyUrl)) {
                setProxyUrl(ProxyUrl);
                textViewProxyUrl.setText(ProxyUrl);
            } else {
                Toast.makeText(MainActivity.this, R.string.err_invalid_url, Toast.LENGTH_SHORT).show();
            }
            return;
        }

        super.onActivityResult(requestCode, resultCode, intent);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.main_activity_actions, menu);

        MenuItem menuItem = menu.findItem(R.id.menu_item_switch);
        if (menuItem == null) {
            return false;
        }

        switchProxy = (Switch) menuItem.getActionView();
        if (switchProxy == null) {
            return false;
        }

        switchProxy.setChecked(LocalVpnService.IsRunning);
        switchProxy.setOnCheckedChangeListener(this);

        return true;
    }


    @Override
    public boolean onPrepareOptionsMenu(Menu menu) {
        super.onPrepareOptionsMenu(menu);
        if (TextUtils.isEmpty(address)) {
            MenuItem menuItem = menu.findItem(R.id.menu_item_account);
            menuItem.setVisible(false);
        } else {
            MenuItem menuItem = menu.findItem(R.id.menu_item_login);
            menuItem.setVisible(false);
        }
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        Intent intent = null;
        switch (item.getItemId()) {
            case R.id.menu_item_about:
                new AlertDialog.Builder(this)
                        .setTitle(getString(R.string.app_name) + getVersionName())
                        .setMessage(R.string.about_info)
                        .setPositiveButton(R.string.btn_ok, null)
                        .setNegativeButton(R.string.btn_more, new OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                //                                startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse("https://github
                                // .com/dawei101/shadowsocks-android-java")));
                            }
                        })
                        .show();

                return true;
            case R.id.menu_item_exit:
                if (!LocalVpnService.IsRunning) {
                    finish();
                    return true;
                }

                new AlertDialog.Builder(this)
                        .setTitle(R.string.menu_item_exit)
                        .setMessage(R.string.exit_confirm_info)
                        .setPositiveButton(R.string.btn_ok, new OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                LocalVpnService.IsRunning = false;
                                LocalVpnService.Instance.disconnectVPN();
                                stopService(new Intent(MainActivity.this, LocalVpnService.class));
                                System.runFinalization();
                                System.exit(0);

                                UserStorageUtils.removeObject(MainActivity.this, StorageKey.USER_ADDRESS);
                                address = null;
                            }
                        })
                        .setNegativeButton(R.string.btn_cancel, null)
                        .show();

                return true;
            case R.id.menu_item_toggle_global:
                ProxyConfig.Instance.globalMode = !ProxyConfig.Instance.globalMode;
                if (ProxyConfig.Instance.globalMode) {
                    onLogReceived("Proxy global mode is on");
                } else {
                    onLogReceived("Proxy global mode is off");
                }
                return true;

            case R.id.menu_item_login:
                intent = new Intent(this, LoginActivity.class);
                startActivity(intent);
                return true;

            case R.id.menu_item_account:
                intent = new Intent(this, MineActivity.class);
                startActivity(intent);
                return true;

            case R.id.menu_item_proxys:
                intent = new Intent(this, ProxyZonesActivity.class);
                startActivity(intent);
                return true;

            default:
                return super.onOptionsItemSelected(item);
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (AppProxyManager.isLollipopOrAbove) {
            if (AppProxyManager.Instance.proxyAppInfo.size() != 0) {
                String tmpString = "";
                for (AppInfo app : AppProxyManager.Instance.proxyAppInfo) {
                    tmpString += app.getAppLabel() + ", ";
                }
                textViewProxyApp.setText(tmpString);
            }
        }

        //blockchains
        {
            address = UserStorageUtils.getObject(MainActivity.this, StorageKey.USER_ADDRESS);

            if (!isRegistProxy) {
                String ip = NetUtils.getLocalIp(this);

                if (ip != null && address != null) {
                    BCRequest.registerProxy(this, ip, address, new BCRequest.Listener<RPCEntity>() {
                        @Override
                        public void onResponse(RPCEntity response) {
                            isRegistProxy = true;
                            DatagramTunnel.initGlobal(MainActivity.this, new RtcPeerContainer.RtcPeerListListener() {
                                @Override
                                public void onUpdated(@NotNull ArrayList<RtcPeerContainer.RtcPeer> peerList) {
                                    mPeerList.clear();
                                    mPeerList.addAll(peerList);
                                    BCRequest.getZonesProxys(MainActivity.this, "", new BCRequest.Listener<PeerEntity>() {
                                        @Override
                                        public void onResponse(PeerEntity peerEntity) {
                                            mHandler.post(new Runnable() {
                                                @Override
                                                public void run() {
                                                    mPeerListAdapter.updatePeerList(mPeerList);
                                                    mPeerListAdapter.notifyDataSetChanged();
                                                    textViewLog.append("" + mPeerList.size() + " online peers got\n");
                                                }
                                            });
                                        }
                                    }, null);
                                    mHandler.post(new Runnable() {
                                        @Override
                                        public void run() {
                                            checkTransStatus();
                                        }
                                    });
                                }
                            }, address);
                        }
                    }, null);
                }
            }

            this.supportInvalidateOptionsMenu();
            checkTransStatus();
        }

    }

    @Override
    protected void onDestroy() {
        DatagramTunnel.finalGlobal();
        LocalVpnService.removeOnStatusChangedListener(this);
        mSSProxyServer.stop();
        super.onDestroy();

        if (loadingDialog!=null) {
            DialogUtils.closeDialog(loadingDialog);
            loadingDialog = null;
        }
    }


    private void checkTransStatus() {
        String txHash = UserStorageUtils.getObject(this, StorageKey.TX_HASH);

        if (!TextUtils.isEmpty(txHash) && mPeerList.size() > 0) {
            if (!LocalVpnService.IsRunning) {
                DatagramTunnel.destroyPeerConnection();
                DatagramTunnel.createPeerConnection(new DatagramTunnel.OnDatagramTunnelOpenListener() {
                    @Override
                    public void onDatagramTunnelOpen() {
                        mHandler.post(new Runnable() {
                            @Override
                            public void run() {
                                startVPN();
                            }
                        });
                    }
                }, mPeerList.get(peerIndex));
            }
            if (switchProxy != null) {
                switchProxy.setChecked(true);
                switchProxy.setEnabled(false);
            }
//            if (loadingDialog == null) {
//                runOnUiThread(new Runnable() {
//                    public void run() {
//                        loadingDialog = DialogUtils.createLoadingDialog(MainActivity.this, "Trade confirmation...");
//                    }
//                });
//            }
//            BCRequest.getReceiptInfo(this, txHash, new BCRequest.Listener<ReceiptEntity>() {
//                @Override
//                public void onResponse(ReceiptEntity receiptEntity) {
//                    DatagramTunnel.destroyPeerConnection();
//                    DatagramTunnel.createPeerConnection(new DatagramTunnel.OnDatagramTunnelOpenListener() {
//                        @Override
//                        public void onDatagramTunnelOpen() {
//                            mHandler.post(new Runnable() {
//                                @Override
//                                public void run() {
//                                    startVPN();
//                                }
//                            });
//                        }
//                    }, mPeerList.get(peerIndex));
//                    if (switchProxy != null) {
//                        switchProxy.setChecked(true);
//                        switchProxy.setEnabled(false);
//                    }
//                    DialogUtils.closeDialog(loadingDialog);
//                }
//            }, new BCRequest.ErrorListener() {
//                @Override
//                public void onError(BCErrorEntity error) {
//                    if (error.getError().getCode().equals("-32000")) {
//                        if (queryRunnable.getCount() < 10) {
//                            queryHandler.postDelayed(queryRunnable, 3000);
//                        } else {
//                            Toast.makeText(MainActivity.this, "Network congestion, please confirm later !", Toast.LENGTH_LONG).show();
//                            DialogUtils.closeDialog(loadingDialog);
//                        }
//                    }
//                }
//            });
        }
    }

    private class QueryRunnable implements Runnable {
        private int count;

        public int getCount() {
            return count;
        }

        @Override
        public void run() {
            count++;
            checkTransStatus();
        }
    }
}
