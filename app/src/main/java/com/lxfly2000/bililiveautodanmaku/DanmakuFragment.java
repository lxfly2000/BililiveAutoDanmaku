package com.lxfly2000.bililiveautodanmaku;

import android.content.Context;
import android.os.Bundle;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import com.lxfly2000.bililiveautodanmaku.placeholder.PlaceholderContent;

import java.util.List;

/**
 * A fragment representing a list of Items.
 */
public class DanmakuFragment extends Fragment {

    private static final String ARG_DANMAKU_STRING = "paramDanmakuString";
    // TODO: Customize parameters
    private String danmakuString;

    /**
     * Mandatory empty constructor for the fragment manager to instantiate the
     * fragment (e.g. upon screen orientation changes).
     */
    public DanmakuFragment() {
    }

    // TODO: Customize parameter initialization
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
            recyclerView.setAdapter(new MyItemRecyclerViewAdapter(PlaceholderContent.ITEMS));
        }
        return view;
    }
}