package com.lxfly2000.bililiveautodanmaku;

import android.content.res.Configuration;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Log;
import android.widget.*;
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
import com.google.android.material.textfield.TextInputLayout;
import okhttp3.*;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

/**
 * A simple {@link Fragment} subclass.
 * Use the {@link LoginWithPhoneFragment#newInstance} factory method to
 * create an instance of this fragment.
 */
public class LoginWithPhoneFragment extends Fragment {
    SettingsHelper settings;
    GT3GeetestUtils gt3GeetestUtils;
    GT3ConfigBean gt3ConfigBean;
    OkHttpClient okHttpClient;
    /**{geetest_validate,geetest_seccode}*/
    JSONObject geetestReturned;
    /**{gt,challenge,new_captcha,success}*/
    JSONObject gtData;
    private static final String ARG_PARAM1 = "param1";
    private static final String ARG_PARAM2 = "param2";

    private String mParam1;
    private String mParam2;

    HashMap<Integer,String> errorCodeMsg=new HashMap<>();
    public LoginWithPhoneFragment() {
        errorCodeMsg.put(0,"成功");
        errorCodeMsg.put(-400,"请求错误");
        errorCodeMsg.put(1006,"请输入正确的短信验证码");
        errorCodeMsg.put(1007,"短信验证码已过期");
    }

    /**
     * Use this factory method to create a new instance of
     * this fragment using the provided parameters.
     *
     * @param param1 Parameter 1.
     * @param param2 Parameter 2.
     * @return A new instance of fragment LoginWithPhoneFragment.
     */
    public static LoginWithPhoneFragment newInstance(String param1, String param2) {
        LoginWithPhoneFragment fragment = new LoginWithPhoneFragment();
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
                    SubthreadToast(e.getLocalizedMessage());
                }

                @Override
                public void onResponse(Call call, Response response) throws IOException {
                    try {
                        gtData=new JSONObject(response.body().string()).getJSONObject("data");
                        gtData.getJSONObject("geetest").put("success",1);
                        gtData.getJSONObject("geetest").put("new_captcha",true);
                        new RequestAPI1().execute(gtData.getJSONObject("geetest"));
                    }catch (JSONException e){
                        SubthreadToast(e.getLocalizedMessage());
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
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_login_with_phone, container, false);
    }

    Spinner spinnerCRCode;
    TextInputLayout editPhone,editVerificationCode;
    Button buttonLogin,buttonSend;
    TextView textViewStatus;

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        //注意Fragment的生命周期，此处才能获取到控件
        spinnerCRCode=view.findViewById(R.id.spinnerCRCode);
        editPhone=view.findViewById(R.id.editPhone);
        editVerificationCode=view.findViewById(R.id.editVerificationCode);
        GT3GeetestButton geetestButton=view.findViewById(R.id.btn_geetest_phone);
        buttonLogin=view.findViewById(R.id.buttonLoginWithPhone);
        buttonSend=view.findViewById(R.id.buttonSend);
        textViewStatus=view.findViewById(R.id.textViewSMSStatus);
        //注意Fragment的生命周期，从这开始才能正确获取到控件
        settings=new SettingsHelper(view.getContext());
        // 请在oncreate方法里初始化以获取足够手势数据来保证第一轮验证成功率
        gt3GeetestUtils = new GT3GeetestUtils(view.getContext());
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
        okHttpClient.newCall(new Request.Builder().url("https://passport.bilibili.com/web/generic/country/list").get().build()).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                SubthreadToast(e.getLocalizedMessage());
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                try {
                    JSONObject crData=new JSONObject(response.body().string());
                    JSONArray arrayObj=crData.getJSONObject("data").getJSONArray("common");
                    List<String>crList=new ArrayList<>();
                    String savedCR=settings.GetString("CR");
                    int sel=0;
                    for(int i=0;i<arrayObj.length();i++){
                        JSONObject obj=arrayObj.getJSONObject(i);
                        String str=obj.getInt("country_id")+"-"+obj.getString("cname");
                        if(str.compareTo(savedCR)==0){
                            sel=crList.size();
                        }
                        crList.add(str);
                    }
                    arrayObj=crData.getJSONObject("data").getJSONArray("others");
                    for(int i=0;i<arrayObj.length();i++){
                        JSONObject obj=arrayObj.getJSONObject(i);
                        String str=obj.getInt("country_id")+"-"+obj.getString("cname");
                        if(str.compareTo(savedCR)==0){
                            sel=crList.size();
                        }
                        crList.add(str);
                    }
                    final int csel=sel;
                    getActivity().runOnUiThread(()->{
                        spinnerCRCode.setAdapter(new ArrayAdapter<>(getActivity(), android.R.layout.simple_spinner_item,crList));
                        spinnerCRCode.setSelection(csel);
                    });
                }catch (JSONException e){
                    SubthreadToast(e.getLocalizedMessage());
                }
            }
        });
        editPhone.getEditText().setText(settings.GetString("Phone"));
        buttonSend.setOnClickListener(view2->DoSend());
        buttonLogin.setOnClickListener(view2->DoLogin());
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        if(gt3GeetestUtils!=null){
            gt3GeetestUtils.destory();
        }
    }

    @Override
    public void onConfigurationChanged(@NonNull Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        if(gt3GeetestUtils!=null){
            gt3GeetestUtils.changeDialogLayout();
        }
    }

    public void SubthreadToast(String str){
        getActivity().runOnUiThread(()->textViewStatus.setText(str));
    }

    JSONObject smsReceived;

    private void DoSend(){
        if(geetestReturned==null){
            Toast.makeText(getActivity(),R.string.msg_check_captcha,Toast.LENGTH_LONG).show();
            return;
        }
        if(editPhone.getEditText().getText().length()==0){
            Toast.makeText(getActivity(),R.string.msg_empty_phone,Toast.LENGTH_LONG).show();
            return;
        }
        try {
            okHttpClient.newCall(new Request.Builder().url("https://passport.bilibili.com/x/passport-login/web/sms/send").post(new FormBody.Builder()
                            .add("tel",editPhone.getEditText().getText().toString())
                            .add("cid",((String)spinnerCRCode.getSelectedItem()).split("-")[0])
                            .add("source","main_web")
                            .add("token",gtData.getString("token"))
                            .add("challenge",gtData.getJSONObject("geetest").getString("challenge"))
                            .add("validate",geetestReturned.getString("geetest_validate"))
                            .add("seccode",geetestReturned.getString("geetest_seccode")).build()).build())
                    .enqueue(new Callback() {
                        @Override
                        public void onFailure(Call call, IOException e) {
                            SubthreadToast(e.getLocalizedMessage());
                        }

                        @Override
                        public void onResponse(Call call, Response response) throws IOException {
                            try {
                                smsReceived=new JSONObject(response.body().string());
                                getActivity().runOnUiThread(()->{
                                    SubthreadToast(getString(R.string.msg_verification_code_sent));
                                    editVerificationCode.requestFocus();
                                    buttonLogin.setEnabled(true);
                                });
                            }catch (JSONException e){
                                SubthreadToast(e.getLocalizedMessage());
                            }
                        }
                    });
        }catch (JSONException e){
            Toast.makeText(getActivity(),e.getLocalizedMessage(),Toast.LENGTH_SHORT).show();
        }
    }

    private void DoLogin(){
        if(editVerificationCode.getEditText().getText().length()==0){
            Toast.makeText(getActivity(),R.string.msg_empty_verification_code,Toast.LENGTH_LONG).show();
            return;
        }
        try {
            okHttpClient.newCall(new Request.Builder().url("https://passport.bilibili.com/x/passport-login/web/login/sms").post(new FormBody.Builder()
                    .add("cid",((String)spinnerCRCode.getSelectedItem()).split("-")[0])
                    .add("tel",editPhone.getEditText().getText().toString())
                    .add("code",editVerificationCode.getEditText().getText().toString())
                    .add("source","main_web")
                    .add("captcha_key",smsReceived.getJSONObject("data").getString("captcha_key"))
                    .build()).build()).enqueue(new Callback() {
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
                        settings.SetString("CR",(String)spinnerCRCode.getSelectedItem());
                        settings.SetString("Phone",editPhone.getEditText().getText().toString());
                        //保存Cookies
                        List<String> cookiesList=response.headers().values("Set-Cookie");
                        JSONObject cookiesObj=new JSONObject();
                        for(String s:cookiesList){
                            String key=s.substring(0,s.indexOf('='));
                            String value=s.substring(s.indexOf('=')+1,s.indexOf(';'));
                            cookiesObj.put(key,value);
                        }
                        settings.SetString("Cookies",cookiesObj.toString());
                        ((LoginActivity)getActivity()).TestCookies(new LoginActivity.OnTestCookiesCallback() {
                            @Override
                            public void onFailed() {
                                SubthreadToast(loginResult.toString());
                            }
                        });
                    }catch (JSONException e){
                        SubthreadToast(e.getLocalizedMessage());
                    }
                }
            });
        }catch (JSONException e){
            Toast.makeText(getActivity(),e.getLocalizedMessage(),Toast.LENGTH_SHORT).show();
        }
    }
}