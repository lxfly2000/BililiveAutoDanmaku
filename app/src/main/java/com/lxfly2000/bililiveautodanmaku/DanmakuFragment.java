package com.lxfly2000.bililiveautodanmaku;

import android.content.Context;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

/**
 * A fragment representing a list of Items.
 */
public class DanmakuFragment extends Fragment {

    private static final String ARG_DANMAKU_STRING = "paramDanmakuString";
    private String danmakuString;

    /**
     * Mandatory empty constructor for the fragment manager to instantiate the
     * fragment (e.g. upon screen orientation changes).
     */
    public DanmakuFragment() {
    }

    @SuppressWarnings("unused")
    public static DanmakuFragment newInstance(String danmakuString) {
        DanmakuFragment fragment = new DanmakuFragment();
        Bundle args = new Bundle();
        args.putString(ARG_DANMAKU_STRING, danmakuString);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        if (getArguments() != null) {
            danmakuString = getArguments().getString(ARG_DANMAKU_STRING);
        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_danmaku_list, container, false);

        // Set the adapter
        if (view instanceof RecyclerView) {
            Context context = view.getContext();
            RecyclerView recyclerView = (RecyclerView) view;
            recyclerView.setLayoutManager(new LinearLayoutManager(context));
            if(danmakuString==null) {
                danmakuString="";
            }
            MyItemRecyclerViewAdapter adapter=new MyItemRecyclerViewAdapter(DanmakuActivity.StringToLineList(danmakuString));
            adapter.SetOnItemClickListener(itemClickListener);
            recyclerView.setAdapter(adapter);
        }
        return view;
    }

    public void SetDanmakuString(String s){
        Bundle bundle=new Bundle();
        bundle.putString(ARG_DANMAKU_STRING,s);
        setArguments(bundle);
    }

    public void SetHighlightItem(int position){
        View view=getView();
        if(view!=null) {
            RecyclerView recyclerView = view.findViewById(R.id.list);
            if (recyclerView != null) {
                MyItemRecyclerViewAdapter adapter = (MyItemRecyclerViewAdapter) recyclerView.getAdapter();
                adapter.SetHighlightItem(position);
                adapter.notifyDataSetChanged();
                if(position>=0&&position<adapter.getItemCount()) {
                    recyclerView.scrollToPosition(position);
                }
            }
        }
    }

    AdapterView.OnItemClickListener itemClickListener;

    public void SetOnItemClickListener(AdapterView.OnItemClickListener listener){
        itemClickListener=listener;
    }
}