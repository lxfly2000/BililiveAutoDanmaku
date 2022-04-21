package com.lxfly2000.bililiveautodanmaku;

import android.app.*;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.Color;
import android.os.Binder;
import android.os.Build;
import android.os.IBinder;
import android.util.Log;
import okhttp3.*;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.util.*;

public class AutoDanmakuService extends Service {
    public AutoDanmakuService() {
    }

    public class LocalBinder extends Binder{
        AutoDanmakuService getService(){
            return AutoDanmakuService.this;
        }
    }

    private LocalBinder localBinder=new LocalBinder();

    @Override
    public IBinder onBind(Intent intent) {
        return localBinder;
    }

    public static final String ACTION_TRY_START =BuildConfig.APPLICATION_ID+".TryStart";
    public static final String ACTION_TRY_STOP=BuildConfig.APPLICATION_ID+".TryStop";
    public static final String ACTION_START =BuildConfig.APPLICATION_ID+".Start";
    public static final String ACTION_STOP=BuildConfig.APPLICATION_ID+".Stop";
    private int notifyId=0;

    private Notification BuildNotificationBar(String title,boolean runningIcon){
        Intent notificationIntent=new Intent(this,DanmakuActivity.class);
        PendingIntent pendingIntent=PendingIntent.getActivity(this,0,notificationIntent,0);
        Notification.MediaStyle style=new Notification.MediaStyle().setShowActionsInCompactView(0);
        Notification.Builder notifBuilder=new Notification.Builder(this);
        if(Build.VERSION.SDK_INT>=Build.VERSION_CODES.O)
            notifBuilder.setChannelId(notifyChannelId);
        notifBuilder.setContentText(getText(R.string.app_name));
        notifBuilder.setSmallIcon(R.mipmap.ic_launcher);
        notifBuilder.setContentTitle(title);
        notifBuilder.setContentIntent(pendingIntent);
        if(runningIcon) {
            Intent iToggleStop=new Intent(ACTION_TRY_STOP);
            PendingIntent piToggleStop=PendingIntent.getBroadcast(this,0,iToggleStop,0);
            notifBuilder.addAction(android.R.drawable.ic_media_pause, getString(R.string.label_stop), piToggleStop);
        }else {
            Intent iToggleStart=new Intent(ACTION_TRY_START);
            PendingIntent piToggleStart=PendingIntent.getBroadcast(this,0,iToggleStart,0);
            notifBuilder.addAction(android.R.drawable.ic_media_play, getString(R.string.label_start), piToggleStart);
        }
        notifBuilder.setStyle(style);
        return notifBuilder.build();
    }

    private BroadcastReceiver notificationReceiver;

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        notifyId=startId;
        notificationManager=(NotificationManager) getSystemService(NOTIFICATION_SERVICE);
        RegisterNotifyIdChannel();
        startForeground(notifyId,BuildNotificationBar(getString(R.string.label_stopped),false));
        IntentFilter fiNotification=new IntentFilter();
        fiNotification.addAction(ACTION_TRY_START);
        fiNotification.addAction(ACTION_TRY_STOP);
        registerReceiver(notificationReceiver=new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                if(intent.getAction().compareTo(ACTION_TRY_START)==0) {
                    if(StartDanmaku()){
                        sendBroadcast(new Intent(ACTION_START));
                    }
                }else if(intent.getAction().compareTo(ACTION_TRY_STOP)==0) {
                    InvokeCallbacks(errorCode,nextLine);
                    if(StopDanmaku()){
                        sendBroadcast(new Intent(ACTION_STOP));
                    }
                }
            }
        },fiNotification);
        return super.onStartCommand(intent, flags, startId);
    }

    private NotificationManager notificationManager;
    private static final String notifyChannelId=BuildConfig.APPLICATION_ID;

    private void RegisterNotifyIdChannel(){
        //https://blog.csdn.net/qq_15527709/article/details/78853048
        String notifyChannelName = "AutoDanmaku Notification Channel";
        NotificationChannel notificationChannel = null;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            notificationChannel = new NotificationChannel(notifyChannelId,
                    notifyChannelName, NotificationManager.IMPORTANCE_DEFAULT);
            notificationChannel.enableLights(false);
            notificationChannel.setLightColor(Color.RED);
            notificationChannel.setShowBadge(false);
            notificationChannel.enableVibration(false);
            notificationChannel.setSound(null,null);
            notificationChannel.setLockscreenVisibility(Notification.VISIBILITY_PUBLIC);
            notificationManager.createNotificationChannel(notificationChannel);
        }
    }

    @Override
    public void onDestroy() {
        if(notificationReceiver!=null){
            unregisterReceiver(notificationReceiver);
            notificationReceiver=null;
        }
        stopForeground(true);
        super.onDestroy();
    }

    class Params {
        JSONObject cookiesObj;
        int liveRoom, sendInterval;
        boolean loop;
        List<String> danmakuList = new ArrayList<>();
    }
    Params params=new Params();
    Timer timer;
    OkHttpClient okHttpClient=new OkHttpClient();
    int nextLine=-1;
    int errorCode=0;
    String errorMsg="";
    String cookieString,bili_jct;

    private String GetDanmakuNotifyString(int index){
        return "["+index+"]"+params.danmakuList.get(index);
    }

    public boolean StartDanmaku(){
        errorCode=0;
        errorMsg="";
        if(params.danmakuList.size()==0){
            errorCode=-1;
            errorMsg=getString(R.string.msg_no_danmaku_set);
            InvokeCallbacks(errorCode,nextLine);
            notificationManager.notify(notifyId,BuildNotificationBar(errorMsg,false));
            return false;
        }
        if(nextLine<0||nextLine>=params.danmakuList.size()){
            nextLine=0;
        }
        if(timer!=null){
            InvokeCallbacks(errorCode,nextLine);
            notificationManager.notify(notifyId,BuildNotificationBar(GetDanmakuNotifyString(nextLine),true));
            return true;
        }
        timer=new Timer();
        try {
            JSONObject cookieObj = new JSONObject(new SettingsHelper(this).GetString("Cookies"));
            //https://github.com/SocialSisterYi/bilibili-API-collect/issues/320
            bili_jct=cookieObj.getString("bili_jct");
            cookieString="";
            for(Iterator<String>iter=cookieObj.keys();iter.hasNext();){
                String k=iter.next();
                cookieString+=k+"="+cookieObj.getString(k)+";";
            }
            timer.schedule(new TimerTask() {
                @Override
                public void run() {
                    okHttpClient.newCall(new Request.Builder().url("https://api.live.bilibili.com/msg/send").post(new FormBody.Builder()
                                            .add("roomid", String.valueOf(params.liveRoom))
                                            .add("color", "16777215")
                                            .add("fontsize", "25")
                                            .add("mode", "1")
                                            .add("msg", params.danmakuList.get(nextLine))
                                            .add("rnd", String.valueOf(System.currentTimeMillis() / 1000))
                                            .add("bubble", "0")
                                            .add("csrf", bili_jct)
                                            .add("csrf_token", bili_jct).build())
                                    .addHeader("Accept", "application/json, text/javascript, */*; q=0.01")
                                    .addHeader("Content-Type", "application/x-www-form-urlencoded; charset=UTF-8")
                                    .addHeader("Origin", "https://live.bilibili.com")
                                    .addHeader("Cookie", cookieString)
                                    .addHeader("Referer", "https://live.bilibili.com/" + params.liveRoom)
                                    .addHeader("User-Agent", "Mozilla/5.0 (Windows NT 6.1; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/74.0.3729.169 Safari/537.36").build())
                            .enqueue(new Callback() {
                                @Override
                                public void onFailure(Call call, IOException e) {
                                    errorCode = -1;
                                    errorMsg = e.getLocalizedMessage();
                                    InvokeCallbacks(errorCode,nextLine);
                                    StopDanmaku();
                                    notificationManager.notify(notifyId,BuildNotificationBar(errorMsg,false));
                                }

                                @Override
                                public void onResponse(Call call, Response response) throws IOException {
                                    nextLine++;
                                    if (nextLine >= params.danmakuList.size()) {
                                        if (params.loop) {
                                            nextLine = 0;
                                            InvokeCallbacks(errorCode,nextLine);
                                            notificationManager.notify(notifyId,BuildNotificationBar(GetDanmakuNotifyString(nextLine),true));
                                        } else {
                                            errorCode=1;
                                            InvokeCallbacks(errorCode,nextLine);
                                            StopDanmaku();
                                        }
                                    }else{
                                        InvokeCallbacks(errorCode,nextLine);
                                        notificationManager.notify(notifyId,BuildNotificationBar(GetDanmakuNotifyString(nextLine),true));
                                    }
                                }
                            });
                }
            }, params.sendInterval, params.sendInterval);
            InvokeCallbacks(errorCode,nextLine);
            notificationManager.notify(notifyId,BuildNotificationBar(GetDanmakuNotifyString(nextLine),true));
        }catch (JSONException e){
            errorCode=-1;
            errorMsg=e.getLocalizedMessage();
            InvokeCallbacks(errorCode,nextLine);
            notificationManager.notify(notifyId,BuildNotificationBar(errorMsg,false));
            return false;
        }
        return true;
    }

    public boolean StopDanmaku(){
        if(timer==null){
            return false;
        }
        timer.cancel();
        timer=null;
        notificationManager.notify(notifyId,BuildNotificationBar(getString(R.string.label_stopped),false));
        return true;
    }

    public AutoDanmakuService SetCookies(JSONObject cookiesObj){
        this.params.cookiesObj=cookiesObj;
        return this;
    }

    public AutoDanmakuService SetContent(List<String> content){
        params.danmakuList=content;
        return this;
    }

    public AutoDanmakuService SetLiveRoom(int liveRoom){
        this.params.liveRoom=liveRoom;
        return this;
    }

    public AutoDanmakuService SetNextLine(int line){
        nextLine=line;
        return this;
    }

    public AutoDanmakuService SetSendInterval(int interval){
        params.sendInterval=interval;
        return this;
    }

    public AutoDanmakuService SetLoop(boolean enable){
        params.loop=enable;
        return this;
    }

    public String GetNextDanmaku(){
        if(nextLine==-1){
            return "";
        }
        return params.danmakuList.get(nextLine);
    }

    public boolean IsStarted(){
        return timer!=null;
    }

    /**@return 0:成功，-1:发生错误，1:弹幕已全部发送*/
    public int GetErrorCode(){
        return errorCode;
    }

    public String GetErrorMsg(){
        return errorMsg;
    }

    public JSONObject GetCookies(){
        return params.cookiesObj;
    }

    public int GetRoomId(){
        return params.liveRoom;
    }

    public int GetInterval(){
        return params.sendInterval;
    }

    public int GetNextLine(){
        return nextLine;
    }

    public boolean GetIsLooped(){
        return params.loop;
    }

    public List<String>GetDanmakuList(){
        return params.danmakuList;
    }

    private List<DanmakuCallback>callbacks=new ArrayList<>();
    public abstract static class DanmakuCallback{
        public abstract void OnCallback(int error,int nextLine);
    }
    public void AddCallback(DanmakuCallback callback){
        callbacks.add(callback);
    }

    public void ClearCallback(){
        callbacks.clear();
    }

    private void InvokeCallbacks(int error,int nextLine){
        for(int i=0;i<callbacks.size();i++){
            callbacks.get(i).OnCallback(error,nextLine);
        }
    }
}