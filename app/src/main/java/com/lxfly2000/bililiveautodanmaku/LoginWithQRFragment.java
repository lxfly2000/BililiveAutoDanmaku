package com.lxfly2000.bililiveautodanmaku;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import com.google.zxing.BarcodeFormat;
import com.google.zxing.MultiFormatWriter;
import com.google.zxing.WriterException;
import com.google.zxing.common.BitMatrix;
import okhttp3.*;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;

/**
 * A simple {@link Fragment} subclass.
 * Use the {@link LoginWithQRFragment#newInstance} factory method to
 * create an instance of this fragment.
 */
public class LoginWithQRFragment extends Fragment {

    // TODO: Rename parameter arguments, choose names that match
    // the fragment initialization parameters, e.g. ARG_ITEM_NUMBER
    private static final String ARG_PARAM1 = "param1";
    private static final String ARG_PARAM2 = "param2";

    // TODO: Rename and change types of parameters
    private String mParam1;
    private String mParam2;

    public LoginWithQRFragment() {
    }

    /**
     * Use this factory method to create a new instance of
     * this fragment using the provided parameters.
     *
     * @param param1 Parameter 1.
     * @param param2 Parameter 2.
     * @return A new instance of fragment LoginWithQRFragment.
     */
    // TODO: Rename and change types and number of parameters
    public static LoginWithQRFragment newInstance(String param1, String param2) {
        LoginWithQRFragment fragment = new LoginWithQRFragment();
        Bundle args = new Bundle();
        args.putString(ARG_PARAM1, param1);
        args.putString(ARG_PARAM2, param2);
        fragment.setArguments(args);
        return fragment;
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
        return inflater.inflate(R.layout.fragment_login_with_qr, container, false);
    }

    ImageView qrView;
    TextView statusView;
    Button buttonLogin;
    OkHttpClient okHttpClient;
    JSONObject qrReturned;
    Bitmap qrBmp;
    Timer timer=new Timer();

    public void SubthreadToast(String str){
        getActivity().runOnUiThread(()->((TextView)getActivity().findViewById(R.id.textViewQRStatus)).setText(str));
    }

    @Override
    public void onActivityCreated(@Nullable Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        qrView=getActivity().findViewById(R.id.imageQRCode);
        statusView=getActivity().findViewById(R.id.textViewQRStatus);
        buttonLogin=getActivity().findViewById(R.id.buttonLoginWithQR);
        okHttpClient=new OkHttpClient();
        okHttpClient.newCall(new Request.Builder().url("https://passport.bilibili.com/qrcode/getLoginUrl").get().build()).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                Toast.makeText(getActivity(),e.getLocalizedMessage(),Toast.LENGTH_SHORT).show();
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                try {
                    qrReturned=new JSONObject(response.body().string());
                    if(qrReturned.getInt("code")!=0){
                        SubthreadToast(qrReturned.toString());
                        return;
                    }
                    BitMatrix matrix=new MultiFormatWriter().encode(qrReturned.getJSONObject("data").getString("url"), BarcodeFormat.QR_CODE,400,400);
                    qrBmp=Bitmap.createBitmap(matrix.getWidth(),matrix.getHeight(), Bitmap.Config.ARGB_8888);
                    for(int i=0;i< matrix.getHeight();i++){
                        for(int j=0;j< matrix.getWidth();j++){
                            qrBmp.setPixel(j,i,matrix.get(j,i)?0xFF000000:0xFFFFFFFF);
                        }
                    }
                    getActivity().runOnUiThread(()->{
                        qrView.setImageBitmap(qrBmp);
                        buttonLogin.setEnabled(true);
                    });
                    timer.schedule(new TimerTask() {
                        @Override
                        public void run() {
                            try {
                                okHttpClient.newCall(new Request.Builder().url("https://passport.bilibili.com/qrcode/getLoginInfo").post(new FormBody.Builder()
                                                .add("oauthKey",qrReturned.getJSONObject("data").getString("oauthKey")).build()).build())
                                        .enqueue(new Callback() {
                                            @Override
                                            public void onFailure(Call call, IOException e) {
                                                timer.cancel();
                                                SubthreadToast(e.getLocalizedMessage());
                                            }

                                            @Override
                                            public void onResponse(Call call, Response response) throws IOException {
                                                try {
                                                    JSONObject objReturn=new JSONObject(response.body().string());
                                                    if(objReturn.getBoolean("status")) {
                                                        //登录成功后的操作
                                                        //保存Cookies
                                                        List<String> cookiesList = response.headers().values("Set-Cookie");
                                                        JSONObject cookiesObj = new JSONObject();
                                                        for (String s : cookiesList) {
                                                            String key = s.substring(0, s.indexOf('='));
                                                            String value = s.substring(s.indexOf('=') + 1, s.indexOf(';'));
                                                            cookiesObj.put(key, value);
                                                        }
                                                        SettingsHelper settings = new SettingsHelper(getActivity());
                                                        settings.SetString("Cookies", cookiesObj.toString());
                                                        getActivity().runOnUiThread(()->{
                                                            if (!((LoginActivity) getActivity()).TestCookies()) {
                                                                SubthreadToast(objReturn.toString());
                                                            }
                                                        });
                                                    }else if(objReturn.getInt("data")==-4){
                                                        //Nothing
                                                    }else if(objReturn.getInt("data")==-5){
                                                        SubthreadToast(getActivity().getString(R.string.msg_qr_confirm_login));
                                                    }else{
                                                        SubthreadToast(objReturn.toString());
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
                    },3000,3000);
                }catch (JSONException e){
                    Toast.makeText(getActivity(),e.getLocalizedMessage(),Toast.LENGTH_SHORT).show();
                }catch (WriterException e){
                    Toast.makeText(getActivity(), e.getLocalizedMessage(), Toast.LENGTH_SHORT).show();
                }
            }
        });
        buttonLogin.setOnClickListener(view->SaveQR());
    }

    public static Uri GetImageContentUri(Context context, java.io.File imageFile) {
        String filePath = imageFile.getAbsolutePath();
        Cursor cursor = context.getContentResolver().query(MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
                new String[] { MediaStore.Images.Media._ID }, MediaStore.Images.Media.DATA + "=? ",
                new String[] { filePath }, null);
        if (cursor != null && cursor.moveToFirst()) {
            int id = cursor.getInt(cursor.getColumnIndex(MediaStore.MediaColumns._ID));
            Uri baseUri = Uri.parse("content://media/external/images/media");
            return Uri.withAppendedPath(baseUri, "" + id);
        } else {
            if (imageFile.exists()) {
                ContentValues values = new ContentValues();
                values.put(MediaStore.Images.Media.DATA, filePath);
                return context.getContentResolver().insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values);
            } else {
                return null;
            }
        }
    }

    private void SaveQR(){
        File dirFile=new File(getActivity().getExternalCacheDir().getPath());
        if(!dirFile.exists()){
            dirFile.mkdirs();
        }
        File file=new File(getActivity().getExternalCacheDir().getPath(),"QR.png");
        try {
            FileOutputStream fos = new FileOutputStream(file);
            qrBmp.compress(Bitmap.CompressFormat.PNG, 100, fos);
            fos.flush();
            fos.close();
            if(GetImageContentUri(getActivity(),file)!=null){
                SubthreadToast(getActivity().getString(R.string.msg_saved_to,file.getAbsolutePath()));
            }
        }catch (IOException e){
            Toast.makeText(getActivity(), e.getLocalizedMessage(), Toast.LENGTH_SHORT).show();
        }
    }
}