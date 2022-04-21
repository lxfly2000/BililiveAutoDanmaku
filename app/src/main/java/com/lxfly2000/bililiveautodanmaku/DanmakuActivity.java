package com.lxfly2000.bililiveautodanmaku;

import android.app.ActivityManager;
import android.content.*;
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
import com.lxfly2000.utilities.AndroidUtility;

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
    /**由于列表获取文本不方便，用作列表文本存储*/
    String danmakuString;
    EditDanmakuFragment editDanmakuFragment;
    DanmakuFragment danmakuFragment;
    private BroadcastReceiver notificationReceiver;
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
        editDanmakuFragment=EditDanmakuFragment.newInstance("");
        danmakuFragment=DanmakuFragment.newInstance("");
        SetListCallback();
        IntentFilter intentFilter=new IntentFilter();
        intentFilter.addAction(AutoDanmakuService.ACTION_START);
        intentFilter.addAction(AutoDanmakuService.ACTION_STOP);
        registerReceiver(notificationReceiver=new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                if(intent.getAction().compareTo(AutoDanmakuService.ACTION_START)==0){
                    LoadServiceSettingsToActivity();
                    ConfirmDanmakuToService(true);
                    StartDanmaku(false,true);
                } else if (intent.getAction().compareTo(AutoDanmakuService.ACTION_STOP) == 0) {
                    StopDanmaku(true);
                }
            }
        },intentFilter);
        //后续操作在OnServiceBound中
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.danmaku_menu,menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        switch (item.getItemId()){
            case R.id.menu_visit_live_room:
                AndroidUtility.OpenUri(this,"https://live.bilibili.com/"+autoDanmakuService.GetRoomId());
                break;
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
        //看一下服务有没有在运行
        if(autoDanmakuService.IsStarted()) {
            StartDanmaku(false,false);
        }else{//如没有就把设置加载一下
            LoadSettingsToService();
        }
        LoadServiceSettingsToActivity();
        ConfirmDanmakuToService(false);
    }

    @Override
    protected void onDestroy() {
        if(notificationReceiver!=null){
            unregisterReceiver(notificationReceiver);
            notificationReceiver=null;
        }
        autoDanmakuService.ClearCallback();
        super.onDestroy();
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
            if(sb.length()>0){
                sb.append("\n");
            }
            sb.append(s);
        }
        return sb.toString();
    }

    private void SaveSettingsFromService(){
        settingsHelper.SetString(keyContents,LineListToString(autoDanmakuService.GetDanmakuList()));
        settingsHelper.SetInt(keyRoomId,autoDanmakuService.GetRoomId());
        int val=autoDanmakuService.GetInterval();
        if(val<5000){
            autoDanmakuService.SetSendInterval(val=5000);
        }
        settingsHelper.SetInt(keyInterval,val);
        settingsHelper.SetBoolean(keyLoop,autoDanmakuService.GetIsLooped());
    }

    private void LoadSettingsToService(){
        autoDanmakuService.SetLiveRoom(Math.max(1,settingsHelper.GetInt(keyRoomId)));
        int val=settingsHelper.GetInt(keyInterval);
        autoDanmakuService.SetSendInterval(val==0?6000:val);
        autoDanmakuService.SetLoop(settingsHelper.GetBoolean(keyLoop));
        autoDanmakuService.SetContent(StringToLineList(settingsHelper.GetString(keyContents)));
    }

    private void LoadServiceSettingsToActivity(){
        editRoomId.getEditText().setText(String.valueOf(autoDanmakuService.GetRoomId()));
        editInterval.getEditText().setText(String.valueOf(autoDanmakuService.GetInterval()));
        checkLoop.setChecked(autoDanmakuService.GetIsLooped());
        danmakuString=LineListToString(autoDanmakuService.GetDanmakuList());
    }

    private void SetServiceSettingsFromActivity(){
        autoDanmakuService.SetContent(StringToLineList(danmakuString))
                .SetLiveRoom(Integer.parseInt(editRoomId.getEditText().getText().toString()))
                .SetSendInterval(Integer.parseInt(editInterval.getEditText().getText().toString()))
                .SetLoop(checkLoop.isChecked());
    }

    private void EditDanmakuFromService(){
        danmakuIsEditing=true;
        //把服务中的弹幕放编辑框里编辑
        buttonEdit.setText(android.R.string.ok);
        editDanmakuFragment.SetEditText(LineListToString(autoDanmakuService.GetDanmakuList()));
        getSupportFragmentManager().beginTransaction().replace(R.id.fragmentDanmaku,editDanmakuFragment).commit();
    }

    private void ConfirmDanmakuToService(boolean uiOnly){
        danmakuIsEditing=false;
        //把编辑好的文本存储到服务，并且展示在列表上
        if(findViewById(R.id.editDanmaku)!=null){
            danmakuString=((EditText)findViewById(R.id.editDanmaku)).getText().toString();
        }
        List<String>list=StringToLineList(danmakuString);
        buttonEdit.setText(getString(R.string.label_edit)+getString(R.string.label_n_danmaku_set,list.size()));
        danmakuFragment.SetDanmakuString(danmakuString);
        getSupportFragmentManager().beginTransaction().replace(R.id.fragmentDanmaku,danmakuFragment).commit();
        if(!uiOnly) {
            autoDanmakuService.SetContent(list);
        }
    }

    boolean danmakuIsEditing=false;

    private void OnButtonEditClicked(){
        if(danmakuIsEditing){
            ConfirmDanmakuToService(false);
            SaveSettingsFromService();
        }else{
            EditDanmakuFromService();
        }
    }

    private void StartDanmaku(boolean saveSettings,boolean uiOnly){
        if(!uiOnly&&saveSettings) {
            SetServiceSettingsFromActivity();
            SaveSettingsFromService();
        }
        autoDanmakuService.ClearCallback();
        autoDanmakuService.AddCallback(new AutoDanmakuService.DanmakuCallback() {
            @Override
            public void OnCallback(int error, int nextLine) {
                if(error==0){
                    runOnUiThread(()-> {
                        textNextLine.setText("[" + nextLine + "]" + autoDanmakuService.GetDanmakuList().get(nextLine));
                        if(!danmakuIsEditing) {
                            danmakuFragment.SetHighlightItem(nextLine);
                        }
                    });
                }else if(error==1){
                    StopDanmaku(false);
                }else{
                    StopDanmaku(false);
                    runOnUiThread(()-> {
                        textNextLine.setText(autoDanmakuService.GetErrorMsg());
                        if(!danmakuIsEditing) {
                            danmakuFragment.SetHighlightItem(-1);
                        }
                    });
                }
            }
        });
        if(uiOnly||autoDanmakuService.StartDanmaku()) {
            buttonStartStop.setText(R.string.label_stop);
            buttonEdit.setEnabled(false);
            editRoomId.setEnabled(false);
            editInterval.setEnabled(false);
            checkLoop.setEnabled(false);
        }else{
            autoDanmakuService.ClearCallback();
            textNextLine.setText(autoDanmakuService.GetErrorMsg());
            if(!danmakuIsEditing) {
                danmakuFragment.SetHighlightItem(-1);
            }
        }
    }

    private void StopDanmaku(boolean uiOnly){
        if(uiOnly||autoDanmakuService.StopDanmaku()) {
            autoDanmakuService.ClearCallback();
            runOnUiThread(()-> {
                buttonStartStop.setText(R.string.label_start);
                textNextLine.setText(R.string.label_stopped);
                if(!danmakuIsEditing) {
                    danmakuFragment.SetHighlightItem(-1);
                }
                buttonEdit.setEnabled(true);
                editRoomId.setEnabled(true);
                editInterval.setEnabled(true);
                checkLoop.setEnabled(true);
            });
        }
    }

    private void OnButtonStartStopClicked(){
        if(danmakuIsEditing){
            ConfirmDanmakuToService(false);
        }
        if(autoDanmakuService.IsStarted()){
            StopDanmaku(false);
        }else{
            StartDanmaku(true,false);
        }
    }

    private void SetListCallback(){
        danmakuFragment.SetOnItemClickListener((adapterView, view, i, l) -> {
            textNextLine.setText("[" + i + "]" + autoDanmakuService.GetDanmakuList().get(i));
            danmakuFragment.SetHighlightItem(i);
            autoDanmakuService.SetNextLine(i);
        });
    }
}