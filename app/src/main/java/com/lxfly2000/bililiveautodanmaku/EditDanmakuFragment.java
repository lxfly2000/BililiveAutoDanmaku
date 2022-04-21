package com.lxfly2000.bililiveautodanmaku;

import android.os.Bundle;
import android.widget.EditText;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

/**
 * A simple {@link Fragment} subclass.
 * Use the {@link EditDanmakuFragment#newInstance} factory method to
 * create an instance of this fragment.
 */
public class EditDanmakuFragment extends Fragment {

    private static final String ARG_DANMAKU_STRING = "paramDanmakuString";

    private String danmakuString;

    public EditDanmakuFragment() {
        // Required empty public constructor
    }

    public static EditDanmakuFragment newInstance(String danmakuString) {
        EditDanmakuFragment fragment = new EditDanmakuFragment();
        Bundle args = new Bundle();
        args.putString(ARG_DANMAKU_STRING, danmakuString);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null) {
            danmakuString=getArguments().getString(ARG_DANMAKU_STRING);
        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_edit_danmaku, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        ((EditText)view.findViewById(R.id.editDanmaku)).setText(danmakuString);
    }

    public void SetEditText(String s){
        Bundle bundle=new Bundle();
        bundle.putString(ARG_DANMAKU_STRING,s);
        setArguments(bundle);
    }
}