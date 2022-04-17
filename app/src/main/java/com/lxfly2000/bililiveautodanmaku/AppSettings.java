package com.lxfly2000.bililiveautodanmaku;

import android.content.Context;
import android.content.SharedPreferences;

public class AppSettings {
    String appName="BililiveAutoDanmaku";
    SharedPreferences pref;
    Context ctx;

    AppSettings(Context ctx){
        this.ctx=ctx;
        pref=ctx.getSharedPreferences(appName,Context.MODE_PRIVATE);
    }

    void SetInt(String key,int i){
        pref.edit().putInt(key,i).apply();
    }

    void SetString(String key,String s){
        pref.edit().putString(key,s).apply();
    }

    int GetInt(String key){
        return pref.getInt(key,0);
    }

    String GetString(String key){
        return pref.getString(key,"");
    }
}
