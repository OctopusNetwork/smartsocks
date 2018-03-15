package io.smarttangle.blockchain.ui;

import android.os.Bundle;
import android.widget.TextView;

import io.smarttangle.blockchain.R;
import io.smarttangle.blockchain.Utils.StorageKey;
import io.smarttangle.blockchain.model.BCRequest;
import io.smarttangle.blockchain.model.RPCEntity;
import io.smarttangle.blockchain.model.UserStorageUtils;


/**
 * Created by haijun on 2018/3/7.
 */

public class MineActivity extends BaseActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.mine_activity);
        TextView addressView = (TextView) findViewById(R.id.tvAddress);
        final String address = UserStorageUtils.getObject(this, StorageKey.USER_ADDRESS);
        addressView.setText(address);

        BCRequest.getBalance(this, address, new BCRequest.Listener<RPCEntity>() {

            @Override
            public void onResponse(final RPCEntity response) {
                TextView tvBalance = (TextView) findViewById(R.id.tvBalance);
                tvBalance.setText(response.getResult() + " Wei");
            }
        }, null);
    }

}
