package com.lxfly2000.bililiveautodanmaku;

import android.content.Context;
import android.content.SharedPreferences;

public class SettingsHelper {
    String appName="BililiveAutoDanmaku";
    SharedPreferences pref;
    Context ctx;

    SettingsHelper(Context ctx){
        this.ctx=ctx;
        pref=ctx.getSharedPreferences(appName,Context.MODE_PRIVATE);
    }

    void SetInt(String key,int i){
        pref.edit().putInt(key,i).apply();
    }

    void SetBoolean(String key, boolean b){
        pref.edit().putBoolean(key, b).apply();
    }

    boolean GetBoolean(String key){
        return pref.getBoolean(key,false);
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
