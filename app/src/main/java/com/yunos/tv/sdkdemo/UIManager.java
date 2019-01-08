package com.yunos.tv.sdkdemo;

import android.app.Application;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.media.MediaPlayer;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;

import com.alibaba.ailabs.custom.core.Constants;
import com.alibaba.ailabs.custom.util.LogUtils;
import com.alibaba.ailabs.custom.util.SystemInfo;
import com.alibaba.fastjson.JSONArray;
import com.hankcs.hanlp.seg.common.Term;
import com.hankcs.hanlp.tokenizer.StandardTokenizer;
import com.yunos.tv.alitvasr.controller.IUIListener;
import com.yunos.tv.alitvasr.controller.protocol.ProtocolData;
import com.yunos.tv.alitvasr.controller.protocol.ReturnCode;
import com.yunos.tv.alitvasr.ui.interfaces.IBaseView;
import com.yunos.tv.alitvasr.ui.interfaces.IUiManager;
import com.yunos.tv.sdkdemo.skills.call.model.ContactPhone;

import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

import static android.content.Context.CONTEXT_IGNORE_SECURITY;
import static android.content.Context.MODE_PRIVATE;
import static com.hankcs.hanlp.HanLP.segment;

/**
 * <p>服务器返回结果，包括tts文本数据，控制命令数据，私有领域数据<br>
 * <p>需要特别关注onStreaming，onRecordStop, onRecognizeResult, onPretreatedResult, onNotify<br>
 */
public class UIManager implements IUiManager{
    public static final String TAG = "UIManager";
    private Handler uiHandler;
    private static Boolean FLAG = false;

    public void setUiHandler(Handler handler) {
        uiHandler = handler;
    }

    /**
     * <p></>对话面板出现和消失的回调<br>
     * <p>@param isShow true:出现 false:消失<br>
     */
    @Override
    public void onShow(boolean isShow) {

    }

    /**
     * 开始录音的回调
     * @param sessionId
     */
    @Override
    public void onRecordStart(int sessionId) {

    }

    /**
     * 特别关注
     * 实时ASR语音转文字
     * @param sessionId
     * @param streamText
     * @param isFinish
     */
    @Override
    public void onStreaming(int sessionId, String streamText, boolean isFinish) {
        Log.e(TAG, "sessionId = " + sessionId + ", streamText= " + streamText
         + ", isFinish=" + isFinish);
        if(isFinish){
            if (streamText != null){

                String[] dials = {"呼叫","拨打","打给","电话"};
                for (String dial : dials){
                    if (streamText.contains(dial)){
                        Message msg = new Message();
                        Bundle bundle = new Bundle();
                        bundle.putString("contant", streamText);
                        msg.what = 2;
                        msg.setData(bundle);
                        uiHandler.sendMessage(msg);
                        SdkControllerAdaptor.getInstance().ignoreThisSession(sessionId);
                    }
                }
            }else {
                SdkControllerAdaptor.getInstance().textToTts("没听清楚您的说话内容，请点击录音按钮，然后再说一遍");
            }

        }
    }



    /**
     * 音量变更回调
     * @param sessionId
     * @param volume
     */
    @Override
    public void onVolume(int sessionId, int volume) {
        Log.e(TAG, "sessionId = " + sessionId + ", volume= " + volume);
    }

    /**
     * 录音结束回调
     * @param sessionId
     */
    @Override
    public void onRecordStop(int sessionId) {
        Log.e(TAG, "sessionId = " + sessionId );

    }

    /**
     * 特别关注
     * NLP语义解析结果回调，原始数据
     * @param sessionId
     * @param data
     */
    @Override
    public void onRecognizeResult(int sessionId, ProtocolData data) {
        Log.e(TAG, "onRecognizeResult: sessionId = " + sessionId  + ",data = " + data.toString());
        //Log.e(TAG, "ignoreNextTts");
    }

    /**
     * 隐藏面板
     */
    @Override
    public void hideUi() {
        Log.e(TAG, "hideUi");
    }

    /**
     * 面板是否显示
     * @return
     */
    @Override
    public boolean isUiShowing() {
        return true;
    }

    /**
     * NLP语义预处理回调，已废弃
     * @param i
     * @param s
     * @param s1
     * @param jsonObject
     * @param s2
     * @return
     */
    @Override
    public int onPretreatedResult(int i, String s, String s1, JSONObject jsonObject, String s2) {
        return 0;
    }

    /**
     * 特别关注
     * NLP语义预处理回调，将领域，命令名，命令参数预解析出来，方便使用
     * @param sessionId
     * @param data
     * @param commandDomain
     * @param command
     * @param commandParams
     * @param question
     * @return ReturnCode.CONTINUE sdk内部继续处理改指令
     * @return ReturnCode.STOP  sdk不需要做后续处理
     */
    @Override
    public int onPretreatedResult(int sessionId, ProtocolData data, String commandDomain, String command, JSONObject commandParams, String question) {
        Log.e(TAG, "onPretreatedResult: sessionId = " + sessionId  + ",commandDomain = " + commandDomain + ",command = " + command + ",commandParams = " + commandParams.toString());
        return ReturnCode.CONTINUE;
    }

    /**
     * 特别关注
     * 底层通知回调，包括账号绑定，音量设置等通知
     * @param type
     * @param data
     * @param arg1
     * @param arg2
     */
    @Override
    public void onNotify(int type, Object data, int arg1, int arg2) {
        LogUtils.d("##onNotify: type="+type+",Object="+data+",arg1="+arg1+",arg2="+arg2);
        switch (type) {
            case IBaseView.NOTIFY_QRCODE_MESSAGE: {
                //显示二维码
                if (data != null) {
                    try {
                        Intent intent = new Intent();
                        intent.setAction(Constants.ACTION_SHOW_QRCODE);
                        intent.setPackage(SystemInfo.getContext().getPackageName());
                        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                        intent.putExtra(Constants.QRCODE_KEY, data.toString());
                        SystemInfo.getContext().startActivity(intent);
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
            }
            break;
            case IBaseView.NOTIFY_BINDER_USER: {
                //绑定设备
                LocalBroadcastManager.getInstance(SystemInfo.getContext()).sendBroadcast(new Intent(Constants.ACTION_STOP_ACTIVITY));
            }
            break;
            case IBaseView.NOTIFY_TYPE_ONLINE_STATUS:
                break;
        }
    }

    /**
     * IUIListener只有onShow一个接口，用于给其他类提供一个面板弹出时的回调入口
     * @param listener
     */
    @Override
    public void setUIListener(IUIListener listener) {

    }

    /**
     * 低内存回调
     */
    @Override
    public void onLowMemory() {

    }

    /**
     * 获取机顶盒上下文信息，手表暂时不需要
     * @param b
     * @param s
     * @return
     */
    @Override
    public String getTvContext(boolean b, String s) {
        return null;
    }

    /**
     * Created by mys on 2018/7/19.
     */

    public static class NluResultListener {
    }
}
