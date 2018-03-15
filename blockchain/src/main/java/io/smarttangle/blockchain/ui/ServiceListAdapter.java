package io.smarttangle.blockchain.ui;

import android.content.Context;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import java.util.List;

import io.smarttangle.blockchain.R;
import io.smarttangle.blockchain.model.PeerEntity;

/**
 * Created by haijun on 2018/3/7.
 */

public class ServiceListAdapter extends RecyclerView.Adapter<ServiceListAdapter.ServiceViewHolder> implements View.OnClickListener {

    private List<PeerEntity.Item> datas;
    private Context context;
    private LayoutInflater inflater;


    public static interface OnItemClickListener {
        void onItemClick(View view, int position);
    }

    private OnItemClickListener onItemClickListener = null;


    public ServiceListAdapter(Context context, List<PeerEntity.Item> datas) {
        this.context = context;
        this.datas = datas;
        inflater = LayoutInflater.from(context);
    }

    @Override
    public int getItemCount() {

        return datas.size();
    }

    @Override
    public void onBindViewHolder(ServiceViewHolder holder, final int position) {
        holder.tv.setText(datas.get(position).getIp());
        holder.itemView.setTag(position);
    }

    @Override
    public ServiceViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View view = inflater.inflate(R.layout.services_list_item, parent, false);
        ServiceViewHolder holder = new ServiceViewHolder(view);
        view.setOnClickListener(this);
        return holder;
    }

    @Override
    public void onClick(View v) {
        if (onItemClickListener != null) {
            int position = (int) v.getTag();
            onItemClickListener.onItemClick(v, position);
        }
    }

    public void setOnItemClickListener(OnItemClickListener clickListener) {
        this.onItemClickListener = clickListener;
    }

    class ServiceViewHolder extends RecyclerView.ViewHolder {

        TextView tv;

        public ServiceViewHolder(View view) {
            super(view);
            tv = (TextView) view.findViewById(R.id.tvIpName);
        }


    }
}
