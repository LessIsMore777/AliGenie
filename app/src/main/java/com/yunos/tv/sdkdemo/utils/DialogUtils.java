package com.yunos.tv.sdkdemo.utils;

import android.app.Activity;
import android.view.Gravity;
import android.view.Window;
import android.view.WindowManager;

import com.yunos.tv.sdkdemo.ui.loadingdialog.LoadingDialog;


public class DialogUtils {

    public static LoadingDialog getDialod(Activity baseActivity, String text) {

        LoadingDialog.Builder loadBuilder=new LoadingDialog.Builder(baseActivity)
                .setMessage(text)
                .setCancelable(false)
                .setCancelOutside(false);
        LoadingDialog dialog=loadBuilder.create();
        Window window = dialog.getWindow();
        WindowManager.LayoutParams lp = window.getAttributes();
        window.setGravity(Gravity.TOP);
        lp.dimAmount = 0f;
        lp.x = 10;
        lp.y = 50;
        lp.width = 200;
        lp.height = 200;
        lp.alpha = 0.6f;
        window.setAttributes(lp);

        return dialog;

    }
}
