package com.yunos.tv.sdkdemo;

import android.Manifest;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.support.v4.app.ActivityCompat;
import android.util.Log;

import com.alibaba.ailabs.custom.ui.INluResultListener;
import com.yunos.tv.sdkdemo.skills.call.model.ContactPhone;

/**
 * <p>gateway会返回tts语音和tts文本，语音在sdk内部解析和播报，文本需要客户在此处自行处理<br>
 */

public class NluResultListener implements INluResultListener {
    public static final String TAG = "UIManager";
    private Context context;
    private Handler uiHandler;

    public void setUiHandler(Handler handler) {
        uiHandler = handler;
    }
    /**
     * <p>特别关注<br>
     * <p>gateway会返回tts语音和tts文本，语音在sdk内部解析和播报，文本需要客户在此处自行处理<br>
     */
    @Override
    public void onNluResult(String result)
    {
        Log.e(TAG, "Nlu Result is : " + result);
        if (null == uiHandler) {
            return;
        }
        Message msg = new Message();
        Bundle bundle = new Bundle();
        bundle.putString("contant", result);
        msg.what = 1;
        msg.setData(bundle);
        uiHandler.sendMessage(msg);
    }





}
