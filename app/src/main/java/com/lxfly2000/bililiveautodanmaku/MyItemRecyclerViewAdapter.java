package com.lxfly2000.bililiveautodanmaku;

import android.graphics.Color;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.LinearLayout;
import android.widget.TextView;
import androidx.recyclerview.widget.RecyclerView;
import com.lxfly2000.bililiveautodanmaku.databinding.FragmentDanmakuBinding;

import java.util.List;

public class MyItemRecyclerViewAdapter extends RecyclerView.Adapter<MyItemRecyclerViewAdapter.ViewHolder> {

    private List<String> mValues;
    private int highlightedItem=-1;

    public MyItemRecyclerViewAdapter(List<String> items) {
        mValues = items;
    }

    public void SetValues(List<String>newValues){
        mValues=newValues;
    }

    @Override
    public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {

        return new ViewHolder(FragmentDanmakuBinding.inflate(LayoutInflater.from(parent.getContext()), parent, false));

    }

    @Override
    public void onBindViewHolder(final ViewHolder holder, int position) {
        holder.mItem = mValues.get(position);
        holder.mIdView.setText("["+position+"]");
        holder.mContentView.setText(mValues.get(position));
        holder.mIdView.setTextColor(highlightedItem==position? Color.WHITE:Color.BLACK);
        holder.mContentView.setTextColor(highlightedItem==position? Color.WHITE:Color.BLACK);
        LinearLayout layout=(LinearLayout) holder.mIdView.getRootView();
        layout.setBackgroundColor(highlightedItem==position?Color.rgb(255,0,110):Color.TRANSPARENT);
        layout.setTag(position);
        layout.setOnClickListener(getPositionClickListener);
    }

    @Override
    public int getItemCount() {
        return mValues.size();
    }

    public class ViewHolder extends RecyclerView.ViewHolder {
        public final TextView mIdView;
        public final TextView mContentView;
        public String mItem;

        public ViewHolder(FragmentDanmakuBinding binding) {
            super(binding.getRoot());
            mIdView = binding.itemNumber;
            mContentView = binding.content;
        }

        @Override
        public String toString() {
            return super.toString() + " '" + mContentView.getText() + "'";
        }
    }

    public void SetHighlightItem(int position){
        highlightedItem=position;
    }

    private AdapterView.OnItemClickListener itemClickListener;
    private View.OnClickListener getPositionClickListener=view -> {
        if(itemClickListener!=null){
            itemClickListener.onItemClick(null,view,(int)view.getTag(),0);
        }
    };

    /**注意：由于RecyclerView与AdapterView的适配器完全不同，回调函数中的adapterView将返回null*/
    public void SetOnItemClickListener(AdapterView.OnItemClickListener listener){
        itemClickListener=listener;
    }
}