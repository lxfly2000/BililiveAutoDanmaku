package com.lxfly2000.bililiveautodanmaku;

import android.app.*;
import android.content.Intent;
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

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        RegisterNotifyIdChannel();
        Notification.Builder notifBuilder=new Notification.Builder(getApplicationContext())
                .setContentIntent(PendingIntent.getActivity(this,0,new Intent(this,DanmakuActivity.class),0))
                .setContentTitle(getString(R.string.app_name))
                .setSmallIcon(R.mipmap.ic_launcher)
                .setContentText("TODO");
        if(Build.VERSION.SDK_INT>=Build.VERSION_CODES.O) {
            notifBuilder.setChannelId(notifyChannelId);
        }
        startForeground(1,notifBuilder.build());
        //TODO:在通知中添加控制启动/暂停的按钮
        return super.onStartCommand(intent, flags, startId);
    }

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
            NotificationManager manager = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
            manager.createNotificationChannel(notificationChannel);
        }
    }

    @Override
    public void onDestroy() {
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

    public boolean StartDanmaku(){
        errorCode=0;
        errorMsg="";
        if(params.danmakuList.size()==0){
            errorCode=-1;
            errorMsg=getString(R.string.msg_no_danmaku_set);
            InvokeCallbacks(errorCode,nextLine);
            return false;
        }
        if(nextLine<0||nextLine>=params.danmakuList.size()){
            nextLine=0;
        }
        if(timer!=null){
            Log.d("Danmaku","Timer is not released.");
            timer.cancel();
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
                                }

                                @Override
                                public void onResponse(Call call, Response response) throws IOException {
                                    nextLine++;
                                    if (nextLine >= params.danmakuList.size()) {
                                        if (params.loop) {
                                            nextLine = 0;
                                            InvokeCallbacks(errorCode,nextLine);
                                        } else {
                                            errorCode=1;
                                            InvokeCallbacks(errorCode,nextLine);
                                            StopDanmaku();
                                        }
                                    }else{
                                        InvokeCallbacks(errorCode,nextLine);
                                    }
                                }
                            });
                }
            }, params.sendInterval, params.sendInterval);
            InvokeCallbacks(errorCode,nextLine);
        }catch (JSONException e){
            errorCode=-1;
            errorMsg=e.getLocalizedMessage();
            InvokeCallbacks(errorCode,nextLine);
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