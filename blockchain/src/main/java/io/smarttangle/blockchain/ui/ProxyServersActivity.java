package io.smarttangle.blockchain.ui;

import android.content.Intent;
import android.os.Bundle;
import android.support.v7.widget.DividerItemDecoration;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.text.TextUtils;
import android.view.View;

import java.util.List;

import io.smarttangle.blockchain.R;
import io.smarttangle.blockchain.Utils.StorageKey;
import io.smarttangle.blockchain.model.BCRequest;
import io.smarttangle.blockchain.model.PeerEntity;
import io.smarttangle.blockchain.model.UserStorageUtils;


/**
 * Created by haijun on 2018/3/7.
 */

public class ProxyServersActivity extends BaseActivity {

    private RecyclerView recyclerView;
    private List<PeerEntity.Item> datas;
    private ServiceListAdapter recycleAdapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.proxy_servers_activity);

        recyclerView = (RecyclerView) findViewById(R.id.rvServers);
        LinearLayoutManager layoutManager = new LinearLayoutManager(this);
        recyclerView.setLayoutManager(layoutManager);


        DividerItemDecoration dividerItemDecoration = new DividerItemDecoration(this, layoutManager.getOrientation());
        recyclerView.addItemDecoration(dividerItemDecoration);

        BCRequest.getZonesProxys(this, "", new BCRequest.Listener<PeerEntity>() {
            @Override
            public void onResponse(PeerEntity peerEntity) {
                datas = peerEntity.result;
                recycleAdapter = new ServiceListAdapter(ProxyServersActivity.this, datas);
                recyclerView.setAdapter(recycleAdapter);
                recycleAdapter.setOnItemClickListener(new ServiceListAdapter.OnItemClickListener() {
                    @Override
                    public void onItemClick(View view, int position) {
                        final String address = UserStorageUtils.getObject(ProxyServersActivity.this, StorageKey.USER_ADDRESS);
                        Intent intent = null;
                        if (TextUtils.isEmpty(address)) {
                            intent = new Intent(ProxyServersActivity.this, LoginActivity.class);
                        } else {
                            intent = new Intent(ProxyServersActivity.this, TransactionActivity.class);
                            intent.putExtra(TransactionActivity.TO_ADDRESS, datas.get(position).getAddress());
                        }
                        startActivity(intent);
                    }
                });
            }
        }, null);
    }
}
