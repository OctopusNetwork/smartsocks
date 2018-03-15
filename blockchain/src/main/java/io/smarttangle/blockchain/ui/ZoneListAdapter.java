package io.smarttangle.blockchain.ui;

import android.content.Context;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import java.util.List;

import io.smarttangle.blockchain.R;


/**
 * Created by haijun on 2018/3/7.
 */

public class ZoneListAdapter extends RecyclerView.Adapter<ZoneListAdapter.ZoneViewHolder> implements View.OnClickListener {

    private List<String> datas;
    private Context context;
    private LayoutInflater inflater;


    public static interface OnItemClickListener {
        void onItemClick(View view, int position);
    }

    private OnItemClickListener onItemClickListener = null;


    public ZoneListAdapter(Context context, List<String> datas) {
        this.context = context;
        this.datas = datas;
        inflater = LayoutInflater.from(context);
    }

    @Override
    public int getItemCount() {

        return datas.size();
    }

    @Override
    public void onBindViewHolder(ZoneViewHolder holder, final int position) {
        holder.tv.setText(datas.get(position));
        holder.itemView.setTag(position);
    }

    @Override
    public ZoneViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {

        View view = inflater.inflate(R.layout.zones_list_item, parent, false);
        ZoneViewHolder holder = new ZoneViewHolder(view);
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

    class ZoneViewHolder extends RecyclerView.ViewHolder {

        TextView tv;

        public ZoneViewHolder(View view) {
            super(view);
            tv = (TextView) view.findViewById(R.id.tvZoneName);
        }


    }

}
