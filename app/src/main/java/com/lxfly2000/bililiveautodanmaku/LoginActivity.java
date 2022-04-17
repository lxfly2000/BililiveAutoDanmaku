package com.lxfly2000.bililiveautodanmaku;

import android.content.Intent;
import android.widget.Toast;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import android.os.Bundle;
import com.google.android.material.textfield.TextInputLayout;

public class LoginActivity extends AppCompatActivity {
    TextInputLayout editUsername,editPassword;
    AppSettings settings;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);
        settings=new AppSettings(this);
        if(TestCookies(settings.GetString("Cookies"))){
            OpenDanmakuActivity();
        }else {
            editUsername = findViewById(R.id.editUsername);
            editPassword = findViewById(R.id.editPassword);
            editUsername.getEditText().setText(settings.GetString("Username"));
            editPassword.getEditText().setText(settings.GetString("Password"));
            findViewById(R.id.buttonLogin).setOnClickListener(view -> OpenCaptchaActivity());
        }
    }

    private void OpenCaptchaActivity(){
        settings.SetString("Username",editUsername.getEditText().getText().toString());
        settings.SetString("Password",editPassword.getEditText().getText().toString());
        startActivityForResult(new Intent(this,CaptchaActivity.class),1);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if(requestCode==1){
            String cookies=data.getStringExtra("Cookies");
            if(TestCookies(cookies)){
                settings.SetString("Cookies",cookies);
                OpenDanmakuActivity();
            }else{
                Toast.makeText(this,R.string.msg_login_failed,Toast.LENGTH_LONG).show();
            }
        }
    }

    private void OpenDanmakuActivity(){
        startActivity(new Intent(this,DanmakuActivity.class));
    }

    private boolean TestCookies(String cookies){
        // TODO: Check Cookies
        Toast.makeText(this,"TODO: TestCookies",Toast.LENGTH_SHORT).show();
        return false;
    }
}