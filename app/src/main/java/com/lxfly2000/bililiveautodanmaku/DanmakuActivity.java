package com.lxfly2000.bililiveautodanmaku;

import android.app.ActivityManager;
import android.content.ComponentName;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.Bundle;
import android.os.IBinder;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.FragmentContainerView;
import com.google.android.material.textfield.TextInputLayout;

import java.util.ArrayList;
import java.util.List;

public class DanmakuActivity extends AppCompatActivity {
    SettingsHelper settingsHelper;
    TextInputLayout editRoomId,editInterval;
    TextView textNextLine;
    CheckBox checkLoop;
    Button buttonEdit,buttonStartStop;
    FragmentContainerView fragmentDanmaku;
    private static final String keyContents="Contents",keyRoomId="RoomId",keyInterval="Interval",keyLoop="Loop";
    String danmakuString;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_danmaku);
        settingsHelper=new SettingsHelper(this);
        StartService();
        editRoomId=findViewById(R.id.editRoomId);
        editInterval=findViewById(R.id.editInterval);
        textNextLine=findViewById(R.id.textNextLine);
        checkLoop=findViewById(R.id.checkLoop);
        buttonEdit=findViewById(R.id.buttonEdit);
        buttonEdit.setOnClickListener(view->OnButtonEditClicked());
        buttonStartStop=findViewById(R.id.buttonStartStop);
        buttonStartStop.setOnClickListener(view->OnButtonStartStopClicked());
        fragmentDanmaku=findViewById(R.id.fragmentDanmaku);
        LoadSettings();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.danmaku_menu,menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        switch (item.getItemId()){
            case R.id.menu_quit_app:
                EndService();
                finish();
                break;
            case R.id.menu_logout:
                EndService();
                DeleteCookies();
                break;
            case R.id.menu_about:
                startActivity(new Intent(this,AboutActivity.class));
                break;
        }
        return super.onOptionsItemSelected(item);
    }

    private void DeleteCookies(){
        settingsHelper.SetString("Cookies","");
        finish();
        startActivity(new Intent(this,LoginActivity.class));
    }

    AutoDanmakuService autoDanmakuService;
    ServiceConnection connection;
    private boolean IsServiceStarted(String serviceName){
        ActivityManager manager=(ActivityManager)getSystemService(ACTIVITY_SERVICE);
        for (ActivityManager.RunningServiceInfo eachService:manager.getRunningServices(Integer.MAX_VALUE)) {
            if (serviceName.equals(eachService.service.getClassName())) {
                return true;
            }
        }
        return false;
    }

    private void StartService(){
        if(autoDanmakuService!=null||connection!=null){
            return;
        }
        connection=new ServiceConnection() {
            @Override
            public void onServiceConnected(ComponentName componentName, IBinder iBinder) {
                autoDanmakuService=((AutoDanmakuService.LocalBinder)iBinder).getService();
                OnServiceBound();
            }

            @Override
            public void onServiceDisconnected(ComponentName componentName) {
                autoDanmakuService=null;
            }
        };
        Intent intent=new Intent(this,AutoDanmakuService.class);
        if(!IsServiceStarted(getPackageName()+".AutoDanmakuService")) {
            startService(intent);
        }
        bindService(intent,connection,BIND_AUTO_CREATE);
    }

    private void OnServiceBound(){
        //onCreate的后续操作
        if(autoDanmakuService.IsStarted()){
            LoadServiceSettings();
        }
    }

    private void EndService(){
        unbindService(connection);
        stopService(new Intent(this,AutoDanmakuService.class));
        connection=null;
    }

    public static List<String>StringToLineList(String string){
        List<String>list=new ArrayList<>();
        for(String s:string.split("\n")){
            if(s.length()>0){
                list.add(s);
            }
        }
        return list;
    }

    public static String LineListToString(List<String>list){
        StringBuilder sb=new StringBuilder();
        for(String s:list){
            sb.append(s).append("\n");
        }
        return sb.toString();
    }

    private void SaveSettings(){
        settingsHelper.SetString(keyContents,danmakuString);
        settingsHelper.SetInt(keyRoomId,Integer.parseInt(editRoomId.getEditText().getText().toString()));
        int val=Integer.parseInt(editInterval.getEditText().getText().toString());
        if(val<5000){
            val=5000;
            editInterval.getEditText().setText("5000");
        }
        settingsHelper.SetInt(keyInterval,val);
        settingsHelper.SetInt(keyLoop,checkLoop.isChecked()?1:0);
    }

    private void LoadSettings(){
        int val=settingsHelper.GetInt(keyRoomId);
        editRoomId.getEditText().setText(String.valueOf(val==0?1:val));
        val=settingsHelper.GetInt(keyInterval);
        editInterval.getEditText().setText(String.valueOf(val==0?6000:val));
        checkLoop.setChecked(settingsHelper.GetInt(keyLoop)==1);
        danmakuString=settingsHelper.GetString(keyContents);
        ConfirmDanmaku();
    }

    private void LoadServiceSettings(){
        editRoomId.getEditText().setText(String.valueOf(autoDanmakuService.GetRoomId()));
        editInterval.getEditText().setText(String.valueOf(autoDanmakuService.GetInterval()));
        checkLoop.setChecked(autoDanmakuService.GetIsLooped());
        danmakuString=LineListToString(autoDanmakuService.GetDanmakuList());
        ConfirmDanmaku();
    }

    private void SetServiceSettings(){
        autoDanmakuService.SetContent(StringToLineList(danmakuString))
                .SetLiveRoom(Integer.parseInt(editRoomId.getEditText().getText().toString()))
                .SetSendInterval(Integer.parseInt(editInterval.getEditText().getText().toString()))
                .SetLoop(checkLoop.isChecked());
    }

    private void EditDanmaku(){
        buttonEdit.setText(android.R.string.ok);
        getSupportFragmentManager().beginTransaction().replace(R.id.fragmentDanmaku,EditDanmakuFragment.newInstance(danmakuString)).commit();
    }

    private void ConfirmDanmaku(){
        buttonEdit.setText(getString(R.string.label_edit)+" ("+getString(R.string.label_n_danmaku_set,StringToLineList(danmakuString).size())+")");
        getSupportFragmentManager().beginTransaction().replace(R.id.fragmentDanmaku,DanmakuFragment.newInstance(danmakuString)).commit();
    }

    boolean danmakuIsEditing=false;

    private void OnButtonEditClicked(){
        danmakuIsEditing=!danmakuIsEditing;
        if(danmakuIsEditing){
            EditDanmaku();
        }else{
            danmakuString=((EditText)findViewById(R.id.editDanmaku)).getText().toString();
            SaveSettings();
            ConfirmDanmaku();
        }
    }

    private void StartDanmaku(){
        SaveSettings();
        SetServiceSettings();
        autoDanmakuService.ClearCallback();
        autoDanmakuService.AddCallback(new AutoDanmakuService.DanmakuCallback() {
            @Override
            public void OnCallback(int error, int nextLine) {
                if(error==0){
                    textNextLine.setText("["+nextLine+"]"+autoDanmakuService.GetDanmakuList().get(nextLine));
                }else if(error==1){
                    StopDanmaku();
                }else{
                    textNextLine.setText(autoDanmakuService.GetErrorMsg());
                }
            }
        });
        if(autoDanmakuService.StartDanmaku()) {
            buttonStartStop.setText(R.string.label_stop);
        }else{
            autoDanmakuService.ClearCallback();
            textNextLine.setText(autoDanmakuService.GetErrorMsg());
        }
    }

    private void StopDanmaku(){
        if(autoDanmakuService.StopDanmaku()) {
            autoDanmakuService.ClearCallback();
            buttonStartStop.setText(R.string.label_start);
            textNextLine.setText(R.string.label_stopped);
        }
    }

    private void OnButtonStartStopClicked(){
        if(autoDanmakuService.IsStarted()){
            StopDanmaku();
        }else{
            StartDanmaku();
        }
    }
}