package com.yunos.tv.sdkdemo;

import android.app.Application;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Process;

import com.alibaba.ailabs.geniesdk.status.StatusManager;
import com.alibaba.ailabs.geniesdk.status.SystemStatusUtis;
import com.yunos.tv.alitvasr.controller.Controller;

/**
 * Created by majun on 2018/4/18.
 */

public class MainApplication extends Application {

    public Context context;
    public MainApplication(){
        context = this;
    }
    @Override
    public void onTerminate() {
        Process.killProcess(Process.myPid());
        super.onTerminate();
    }
    @Override
    public void onCreate() {
        super.onCreate();
        SdkControllerAdaptor.getInstance().initGenieSdk(this, "b76a28b8-3647-499c-9873-6dac96dd8234");
        IntentFilter filter = new IntentFilter();
        filter.addAction("android.media.VOLUME_CHANGED_ACTION");
        filter.addAction("android.media.MASTER_VOLUME_CHANGED_ACTION");

        BroadcastReceiver receiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
            if (intent.getAction() != null) {
                try {
                    if (intent.getAction().equals("android.media.VOLUME_CHANGED_ACTION")
                            || intent.getAction().equals("android.media.MASTER_VOLUME_CHANGED_ACTION")) {
                        if (Controller.getInstance() != null) {
                            SdkControllerAdaptor.getInstance().syncVolumeLevel(context);
                        }
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
            }
        };
        registerReceiver(receiver, filter);
    }

}
