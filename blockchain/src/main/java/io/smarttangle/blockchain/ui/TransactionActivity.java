package io.smarttangle.blockchain.ui;

import android.app.Dialog;
import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import io.smarttangle.blockchain.R;
import io.smarttangle.blockchain.Utils.StorageKey;
import io.smarttangle.blockchain.model.BCRequest;
import io.smarttangle.blockchain.model.RPCEntity;
import io.smarttangle.blockchain.model.UserStorageUtils;

/**
 * Created by haijun on 2018/3/7.
 */

public class TransactionActivity extends BaseActivity {

    public final static String TO_ADDRESS = "TO_ADDRESS";
    public final static String AMOUNT = "0x10";
    private String toAddress;
    private String fromAddress;
    private Dialog loadingDialog;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.transaction_activity);

        fromAddress = UserStorageUtils.getObject(this, StorageKey.USER_ADDRESS);
        TextView textView = (TextView) findViewById(R.id.tvMyAddress);
        textView.setText(fromAddress);

        Intent intent = this.getIntent();
        if (intent != null) {
            toAddress = intent.getStringExtra(TO_ADDRESS);
            TextView toAddressView = (TextView) findViewById(R.id.tvDestAddress);
            toAddressView.setText(toAddress);
        }

        findViewById(R.id.btTransfer).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                EditText editText = (EditText) findViewById(R.id.evPassword);
                String password = editText.getText().toString();
                if (TextUtils.isEmpty(password)) {
                    Toast.makeText(TransactionActivity.this, "Please input transaction password !", Toast.LENGTH_SHORT).show();
                } else {
                    loadingDialog = DialogUtils.createLoadingDialog(TransactionActivity.this, "Trading...");
                    BCRequest.transaction(TransactionActivity.this, fromAddress, toAddress, AMOUNT, password, new BCRequest.Listener<RPCEntity>() {

                        @Override
                        public void onResponse(RPCEntity rpcEntity) {
                            UserStorageUtils.putObject(TransactionActivity.this, StorageKey.TX_HASH, rpcEntity.getResult());
                            Intent intent = new Intent();
                            intent.setAction("com.kkt.smartsocks.ACTION_MAIN");
                            intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
                            startActivity(intent);
                            DialogUtils.closeDialog(loadingDialog);
                        }
                    }, null);
                }
            }
        });
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();

        if (loadingDialog!=null) {
            DialogUtils.closeDialog(loadingDialog);
            loadingDialog = null;
        }
    }

}
