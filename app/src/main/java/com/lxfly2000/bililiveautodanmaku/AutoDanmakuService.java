package com.lxfly2000.bililiveautodanmaku;

import android.app.Service;
import android.content.Intent;
import android.os.Binder;
import android.os.IBinder;
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

    class Params {
        JSONObject cookiesObj;
        int liveRoom, startLine, sendInterval;
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

    public void StartDanmaku(){
        nextLine= params.startLine;
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
                                    StopDanmaku();
                                }

                                @Override
                                public void onResponse(Call call, Response response) throws IOException {
                                    nextLine++;
                                    if (nextLine >= params.danmakuList.size()) {
                                        if (params.loop) {
                                            nextLine = 0;
                                        } else {
                                            StopDanmaku();
                                        }
                                    }
                                }
                            });
                }
            }, params.sendInterval, params.sendInterval);
        }catch (JSONException e){
            errorCode=-1;
            errorMsg=e.getLocalizedMessage();
        }
    }

    public boolean StopDanmaku(){
        if(timer==null){
            return false;
        }
        timer.cancel();
        timer=null;
        nextLine=-1;
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

    public AutoDanmakuService SetStartLine(int startLine){
        this.params.startLine=startLine;
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

    public int GetErrorCode(){
        return errorCode;
    }

    public String GetErrorMsg(){
        return errorMsg;
    }
}