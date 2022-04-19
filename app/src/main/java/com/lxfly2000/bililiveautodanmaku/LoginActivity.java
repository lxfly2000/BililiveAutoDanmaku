package com.lxfly2000.bililiveautodanmaku;

import android.content.Intent;
import android.content.res.Configuration;
import android.os.AsyncTask;
import android.os.Looper;
import android.util.Log;
import android.widget.TextView;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import android.os.Bundle;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentContainerView;
import androidx.viewpager2.adapter.FragmentStateAdapter;
import androidx.viewpager2.widget.ViewPager2;
import com.geetest.sdk.GT3ConfigBean;
import com.geetest.sdk.GT3ErrorBean;
import com.geetest.sdk.GT3GeetestUtils;
import com.geetest.sdk.GT3Listener;
import com.geetest.sdk.views.GT3GeetestButton;
import com.github.gzuliyujiang.rsautils.RSAUtils;
import com.google.android.material.tabs.TabLayout;
import com.google.android.material.tabs.TabLayoutMediator;
import com.google.android.material.textfield.TextInputLayout;
import okhttp3.*;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

public class LoginActivity extends AppCompatActivity {
    TabLayout tabLayout;
    ViewPager2 viewPager2;
    List<Fragment>fragments=new ArrayList<>();
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);
        tabLayout=findViewById(R.id.tabLogin);
        viewPager2=findViewById(R.id.viewPager);
        fragments.add(LoginWithAccountFragment.newInstance("a", "b"));
        fragments.add(LoginWithPhoneFragment.newInstance("a","b"));
        fragments.add(LoginWithQRFragment.newInstance("a","b"));
        viewPager2.setAdapter(new FragmentStateAdapter(this) {
            @NonNull
            @Override
            public Fragment createFragment(int position) {
                return fragments.get(position);
            }

            @Override
            public int getItemCount() {
                return fragments.size();
            }
        });
        new TabLayoutMediator(tabLayout, viewPager2, true,
                (tab, position) -> tab.setText(getResources().getStringArray(R.array.label_login_methods)[position])).attach();
        TestCookies();
    }

    private void OpenDanmakuActivity(){
        startActivity(new Intent(this,DanmakuActivity.class));
        finish();
    }

    public boolean TestCookies(){
        try {
            JSONObject cookiesObj=new JSONObject(new SettingsHelper(this).GetString("Cookies"));
            String sess=cookiesObj.getString("SESSDATA");
            Toast.makeText(this,sess,Toast.LENGTH_LONG).show();
            //TODO:如果成功则调用OpenDanmakuActivity.
            /*if(false){
                OpenDanmakuActivity();
                return true;
            }*/
        }catch (JSONException e){
            Log.d("Login",e.getLocalizedMessage());
        }
        return false;
    }

    boolean canFinish=true;

    @Override
    public void onBackPressed() {
        super.onBackPressed();
        if(viewPager2.getCurrentItem()==2){
            canFinish=false;
            new AlertDialog.Builder(this)
                    .setTitle(android.R.string.dialog_alert_title)
                    .setMessage(R.string.msg_qrcode_close_warning)
                    .setOnDismissListener(dialogInterface -> canFinish=true)
                    .setNegativeButton(android.R.string.no,null)
                    .setPositiveButton(android.R.string.yes,(dialogInterface, i) -> {
                        canFinish=true;
                        finish();
                    }).show();
        }
    }

    @Override
    public void finish() {
        if(canFinish) {
            super.finish();
        }
    }
}