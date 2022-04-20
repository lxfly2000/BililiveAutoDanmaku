package com.lxfly2000.bililiveautodanmaku;

import androidx.appcompat.app.AppCompatActivity;
import android.os.Bundle;
import com.lxfly2000.utilities.AndroidUtility;

public class AboutActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_about);
        findViewById(R.id.buttonViewGithub).setOnClickListener(view -> AndroidUtility.OpenUri(getBaseContext(),"https://github.com/lxfly2000/BililiveAutoDanmaku"));
    }
}