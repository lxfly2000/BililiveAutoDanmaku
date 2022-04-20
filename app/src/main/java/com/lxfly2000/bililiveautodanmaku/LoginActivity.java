package com.lxfly2000.bililiveautodanmaku;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;
import androidx.viewpager2.adapter.FragmentStateAdapter;
import androidx.viewpager2.widget.ViewPager2;
import com.google.android.material.tabs.TabLayout;
import com.google.android.material.tabs.TabLayoutMediator;
import okhttp3.*;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.util.ArrayList;
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
        TestCookies(new OnTestCookiesCallback() {
            @Override
            public void onFailed() {
                //Nothing.
            }
        });
    }

    private void OpenDanmakuActivity(){
        startActivity(new Intent(this,DanmakuActivity.class));
        finish();
    }

    public abstract static class OnTestCookiesCallback{
        public abstract void onFailed();
    }

    public void TestCookies(OnTestCookiesCallback callback){
        try {
            JSONObject cookiesObj=new JSONObject(new SettingsHelper(this).GetString("Cookies"));
            String sess=cookiesObj.getString("SESSDATA");
            new OkHttpClient().newCall(new Request.Builder().url("https://api.bilibili.com/x/space/myinfo").get()
                    .addHeader("Cookie","SESSDATA="+sess).build()).enqueue(new Callback() {
                @Override
                public void onFailure(Call call, IOException e) {
                    runOnUiThread(()->Toast.makeText(LoginActivity.this,e.getLocalizedMessage(),Toast.LENGTH_SHORT).show());
                    callback.onFailed();
                }

                @Override
                public void onResponse(Call call, Response response) throws IOException {
                    try {
                        JSONObject objReturned=new JSONObject(response.body().string());
                        if(objReturned.getInt("code")!=0){
                            runOnUiThread(()->Toast.makeText(LoginActivity.this,objReturned.toString(),Toast.LENGTH_SHORT).show());
                            callback.onFailed();
                            return;
                        }
                        runOnUiThread(()->OpenDanmakuActivity());
                    }catch (JSONException e){
                        runOnUiThread(()->Toast.makeText(LoginActivity.this,e.getLocalizedMessage(),Toast.LENGTH_SHORT).show());
                        callback.onFailed();
                    }
                }
            });
        }catch (JSONException e){
            Log.d("Login",e.getLocalizedMessage());
            callback.onFailed();
        }
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