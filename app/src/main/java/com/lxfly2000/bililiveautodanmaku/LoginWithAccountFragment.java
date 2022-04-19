package com.lxfly2000.bililiveautodanmaku;

import android.content.Intent;
import android.content.res.Configuration;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Log;
import android.widget.TextView;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import com.geetest.sdk.GT3ConfigBean;
import com.geetest.sdk.GT3ErrorBean;
import com.geetest.sdk.GT3GeetestUtils;
import com.geetest.sdk.GT3Listener;
import com.geetest.sdk.views.GT3GeetestButton;
import com.github.gzuliyujiang.rsautils.RSAUtils;
import com.google.android.material.textfield.TextInputLayout;
import okhttp3.*;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;

public class LoginWithAccountFragment extends Fragment {
    TextInputLayout editUsername,editPassword;
    SettingsHelper settings;
    GT3GeetestUtils gt3GeetestUtils;
    GT3ConfigBean gt3ConfigBean;
    OkHttpClient okHttpClient;
    /**{geetest_validate,geetest_seccode}*/
    JSONObject geetestReturned;
    /**{gt,challenge,new_captcha,success}*/
    JSONObject gtData;
    HashMap<Integer,String> errorCodeMsg=new HashMap<>();
    public LoginWithAccountFragment() {
        errorCodeMsg.put(0,"成功");
        errorCodeMsg.put(-400,"请求错误");
        errorCodeMsg.put(-629,"账号或密码错误");
        errorCodeMsg.put(-653,"用户名或密码不能为空");
        errorCodeMsg.put(-662,"提交超时,请重新提交");
        errorCodeMsg.put(-2001,"缺少必要的的参数");
        errorCodeMsg.put(-2100,"需验证手机号或邮箱");
        errorCodeMsg.put(2400,"登录秘钥错误");
        errorCodeMsg.put(2406,"验证极验服务出错");
        errorCodeMsg.put(86000,"RSA解密失败");
    }

    private static final String ARG_PARAM1 = "param1";
    private static final String ARG_PARAM2 = "param2";
    private String mParam1;
    private String mParam2;

    public static LoginWithAccountFragment newInstance(String param1, String param2) {
        LoginWithAccountFragment fragment = new LoginWithAccountFragment();
        Bundle args = new Bundle();
        args.putString(ARG_PARAM1, param1);
        args.putString(ARG_PARAM2, param2);
        fragment.setArguments(args);
        return fragment;
    }

    class MyGT3Listener extends GT3Listener {

        @Override
        public void onReceiveCaptchaCode(int i) {
            Log.d("GT3","onReceiveCaptchaCode: "+i);
        }

        @Override
        public void onDialogResult(String result) {
            super.onDialogResult(result);
            new RequestAPI2().execute(result);
        }

        @Override
        public void onStatistics(String s) {
            Log.d("GT3","onStatistics: "+s);
        }

        @Override
        public void onClosed(int i) {
            Log.d("GT3","onClosed: "+i);
        }

        @Override
        public void onSuccess(String s) {
            Log.d("GT3","onSuccess: "+s);
        }

        @Override
        public void onFailed(GT3ErrorBean gt3ErrorBean) {
            Log.d("GT3","onFailed: "+gt3ErrorBean.toString());
        }

        @Override
        public void onButtonClick() {
            okHttpClient.newCall(new Request.Builder().url("https://passport.bilibili.com/x/passport-login/captcha?source=main_web").get().build()).enqueue(new Callback() {
                @Override
                public void onFailure(Call call, IOException e) {
                    Toast.makeText(getActivity(),e.getLocalizedMessage(),Toast.LENGTH_SHORT).show();
                }

                @Override
                public void onResponse(Call call, Response response) throws IOException {
                    try {
                        gtData=new JSONObject(response.body().string()).getJSONObject("data");
                        gtData.getJSONObject("geetest").put("success",1);
                        gtData.getJSONObject("geetest").put("new_captcha",true);
                        new RequestAPI1().execute(gtData.getJSONObject("geetest"));
                    }catch (JSONException e){
                        Toast.makeText(getActivity(),e.getLocalizedMessage(),Toast.LENGTH_SHORT).show();
                    }
                }
            });
        }
    }

    class RequestAPI1 extends AsyncTask<JSONObject, Void, JSONObject> {

        @Override
        protected JSONObject doInBackground(JSONObject... params) {
            return params[0];
        }

        @Override
        protected void onPostExecute(JSONObject params) {
            // SDK可识别格式为
            // {"success":1,"challenge":"06fbb267def3c3c9530d62aa2d56d018","gt":"019924a82c70bb123aae90d483087f94","new_captcha":true}
            gt3ConfigBean.setApi1Json(params);
            // 继续验证
            gt3GeetestUtils.getGeetest();
        }
    }

    /**
     * 请求api2
     */
    class RequestAPI2 extends AsyncTask<String, Void, String> {

        @Override
        protected String doInBackground(String... params) {
            return params[0];
        }

        @Override
        protected void onPostExecute(String result) {
            try {
                geetestReturned = new JSONObject(result);
                gt3GeetestUtils.showSuccessDialog();
            } catch (Exception e) {
                gt3GeetestUtils.showFailedDialog();
            }
        }
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null) {
            mParam1 = getArguments().getString(ARG_PARAM1);
            mParam2 = getArguments().getString(ARG_PARAM2);
        }
    }

    @Override
    public void onActivityCreated(@Nullable Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        //注意Fragment的生命周期，从这开始才能正确获取到控件
        settings=new SettingsHelper(getActivity());
        // 请在oncreate方法里初始化以获取足够手势数据来保证第一轮验证成功率
        GT3GeetestButton geetestButton = getActivity().findViewById(R.id.btn_geetest);
        gt3GeetestUtils = new GT3GeetestUtils(getActivity());
        // 配置bean文件，也可在oncreate初始化
        gt3ConfigBean = new GT3ConfigBean();
        // 设置验证模式，1：bind，2：unbind
        gt3ConfigBean.setPattern(2);
        // 设置点击灰色区域是否消失，默认不消息
        gt3ConfigBean.setCanceledOnTouchOutside(false);
        gt3ConfigBean.setLang(null);
        // 设置加载webview超时时间，单位毫秒，默认10000，仅且webview加载静态文件超时，不包括之前的http请求
        gt3ConfigBean.setTimeout(10000);
        // 设置webview请求超时(用户点选或滑动完成，前端请求后端接口)，单位毫秒，默认10000
        gt3ConfigBean.setWebviewTimeout(10000);
        // 设置回调监听
        gt3ConfigBean.setListener(new MyGT3Listener());
        gt3GeetestUtils.init(gt3ConfigBean);
        // 绑定
        geetestButton.setGeetestUtils(gt3GeetestUtils);
        okHttpClient=new OkHttpClient();
        if(TestCookies()){
            OpenDanmakuActivity();
        }else {
            editUsername = getActivity().findViewById(R.id.editUsername);
            editPassword = getActivity().findViewById(R.id.editPassword);
            editUsername.getEditText().setText(settings.GetString("Username"));
            editPassword.getEditText().setText(settings.GetString("Password"));
            getActivity().findViewById(R.id.buttonLoginWithAccount).setOnClickListener(view -> DoLogin());
        }
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (gt3GeetestUtils != null) {
            gt3GeetestUtils.destory();
        }
    }

    @Override
    public void onConfigurationChanged(@NonNull Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        if (gt3GeetestUtils != null) {
            gt3GeetestUtils.changeDialogLayout();
        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_login_with_account, container, false);
    }

    /**{hash:密码加盐值,key:RSA公钥}*/
    JSONObject bilibiliGetKey;
    private void DoLogin(){
        if(geetestReturned==null){
            Toast.makeText(getActivity(),R.string.msg_check_captcha,Toast.LENGTH_LONG).show();
            return;
        }
        okHttpClient.newCall(new Request.Builder().url("https://passport.bilibili.com/login?act=getkey").get().build()).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                Toast.makeText(getActivity(),e.getLocalizedMessage(),Toast.LENGTH_SHORT).show();
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                try {
                    bilibiliGetKey = new JSONObject(response.body().string());
                    String saltedPassword=bilibiliGetKey.getString("hash").concat(editPassword.getEditText().getText().toString());
                    String encPassword= RSAUtils.encryptToBase64(saltedPassword.getBytes(),
                            RSAUtils.generatePublicKey(bilibiliGetKey.getString("key")));
                    LoginPost(encPassword);
                }catch (JSONException e){
                    Toast.makeText(getActivity(),e.getLocalizedMessage(),Toast.LENGTH_SHORT).show();
                }
            }
        });
    }

    public void SubthreadToast(String str){
        getActivity().runOnUiThread(()->((TextView)getActivity().findViewById(R.id.textViewAccountStatus)).setText(str));
    }

    class LoginThread extends AsyncTask<String,Void,String>{
        @Override
        protected String doInBackground(String... params) {
            return params[0];
        }

        @Override
        protected void onPostExecute(String pw) {
            super.onPostExecute(pw);
            try {
                //https://github.com/SocialSisterYi/bilibili-API-collect/blob/3f31415ebe81d8b21f3840d3c3e5a896a3d151f5/login/login_action/password.md
                okHttpClient.newCall(new Request.Builder().url("https://passport.bilibili.com/x/passport-login/web/login").post(new FormBody.Builder()
                        .add("source", "main_web")
                        .add("username", editUsername.getEditText().getText().toString())
                        .add("password", pw)
                        .add("keep", "true")
                        .add("token", gtData.getString("token"))
                        .add("challenge", gtData.getJSONObject("geetest").getString("challenge"))
                        .add("validate", geetestReturned.getString("geetest_validate"))
                        .add("seccode", geetestReturned.getString("geetest_seccode")).build()).build()).enqueue(new Callback() {
                    @Override
                    public void onFailure(Call call, IOException e) {
                        SubthreadToast(e.getLocalizedMessage());
                    }

                    @Override
                    public void onResponse(Call call, Response response) throws IOException {
                        try {
                            JSONObject loginResult=new JSONObject(response.body().string());
                            if(loginResult.getInt("code")!=0){
                                int returnCode=loginResult.getInt("code");
                                SubthreadToast(returnCode +": "+errorCodeMsg.get(returnCode));
                                return;
                            }
                            //登录成功后的操作
                            settings.SetString("Username", editUsername.getEditText().getText().toString());
                            settings.SetString("Password", editPassword.getEditText().getText().toString());
                            if(loginResult.getJSONObject("data").has("isLogin")) {
                                //保存Cookies
                                List<String> cookiesList=response.headers().values("Set-Cookie");
                                JSONObject cookiesObj=new JSONObject();
                                for(String s:cookiesList){
                                    String key=s.substring(0,s.indexOf('='));
                                    String value=s.substring(s.indexOf('='),s.indexOf(';'));
                                    cookiesObj.put(key,value);
                                }
                                settings.SetString("Cookies",cookiesObj.toString());
                                TestCookies();
                            }else{
                                SubthreadToast(loginResult.toString());
                            }
                        }catch (JSONException e){
                            SubthreadToast(e.getLocalizedMessage());
                        }
                    }
                });
            }catch (JSONException e){
                SubthreadToast(e.getLocalizedMessage());
            }
        }
    }

    void LoginPost(String pw){
        new LoginThread().execute(pw);
    }

    private void OpenDanmakuActivity(){
        startActivity(new Intent(getActivity(),DanmakuActivity.class));
        getActivity().finish();
    }

    private boolean TestCookies(){
        try {
            JSONObject cookiesObj=new JSONObject(settings.GetString("Cookies"));
            //如果成功则调用OpenDanmakuActivity.
            if(/*TODO*/true){
                OpenDanmakuActivity();
            }
        }catch (JSONException e){
            Log.d("Login",e.getLocalizedMessage());
        }
        return false;
    }
}