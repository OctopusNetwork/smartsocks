package io.smarttangle.blockchain.ui;

import android.content.Intent;
import android.os.Bundle;
import android.support.v7.widget.DividerItemDecoration;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.OrientationHelper;
import android.support.v7.widget.RecyclerView;
import android.view.View;
import android.widget.ListAdapter;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.List;

import io.smarttangle.blockchain.R;

/**
 * Created by haijun on 2018/3/7.
 */

public class ProxyZonesActivity extends BaseActivity {

    private RecyclerView recyclerView;
    private List<String> datas;
    private ZoneListAdapter recycleAdapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.proxy_zones_activity);

        initData();

        recyclerView = (RecyclerView) findViewById(R.id.rvZonesList);
        LinearLayoutManager layoutManager = new LinearLayoutManager(this);
        recyclerView.setLayoutManager(layoutManager);
        recycleAdapter = new ZoneListAdapter(this, datas);
        recyclerView.setAdapter(recycleAdapter);

        DividerItemDecoration dividerItemDecoration = new DividerItemDecoration(this, layoutManager.getOrientation());
        recyclerView.addItemDecoration(dividerItemDecoration);

        recycleAdapter.setOnItemClickListener(new ZoneListAdapter.OnItemClickListener() {
            @Override
            public void onItemClick(View view, int position) {
                Intent intent = new Intent(ProxyZonesActivity.this, ProxyServersActivity.class);
                startActivity(intent);
            }
        });
    }

    private void initData() {
        datas = new ArrayList<String>();
//        for (int i = 0; i < 20; i++) {
            datas.add("China");
            datas.add("USA");
//        }

    }

}
