package com.yunos.tv.sdkdemo;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.media.AudioFormat;
import android.media.MediaRecorder;
import android.util.Log;
import android.os.Handler;

import com.alibaba.ailabs.custom.audio.MediaOutputBridge;
import com.alibaba.ailabs.custom.command.WakeUpCommand;
import com.alibaba.ailabs.custom.core.DeviceController;
import com.alibaba.ailabs.custom.core.DeviceState;
import com.alibaba.ailabs.custom.core.GatewayBridge;
import com.alibaba.ailabs.custom.logger.STSLogger;
import com.alibaba.ailabs.custom.ui.NluRusultBridge;
import com.alibaba.ailabs.custom.util.LogUtils;
import com.alibaba.ailabs.custom.util.SystemInfo;
import com.alibaba.ailabs.geniesdk.AiLabsCore;
import com.alibaba.ailabs.geniesdk.audioin.AudioRecorderAdapter;
import com.alibaba.ailabs.geniesdk.record.NativeRecorder;
import com.alibaba.ailabs.geniesdk.status.StatusManager;
import com.alibaba.ailabs.geniesdk.status.SystemStatusUtis;
import com.alibaba.ailabs.geniesdk.ut.IUserTracer;
import com.alibaba.ailabs.geniesdk.ut.NativeUserTracer;
import com.alibaba.fastjson.JSONObject;
import com.yunos.tv.alitvasr.controller.Controller;
import com.yunos.tv.alitvasr.controller.session.SessionID;

import java.io.File;
import java.sql.Time;
import java.util.Map;
import java.util.Timer;

/**
 * sdk控制接口类
 */
public class SdkControllerAdaptor implements IUserTracer {
    private static SdkControllerAdaptor instance;
    private UIManager uiManager;
    private NluResultListener nluResultListener;
    //private NearFieldRecorder recorder;
    private Context contex;

    public static SdkControllerAdaptor getInstance() {
        if (null == instance) {
            Class var4 = Controller.class;
            synchronized (Controller.class) {
                if (null == instance) {
                    instance = new SdkControllerAdaptor();
                }
            }
        }
        return instance;
    }

    public SdkControllerAdaptor() {
        uiManager = new UIManager();
        nluResultListener = new NluResultListener();
    }

    /**
     * <p>@param handler 注册Ui线程中创建的Handler，将tts返回结果透传给UI线程做文本展示<br>
     */
    public void setUiHandler(Handler handler) {
        uiManager.setUiHandler(handler);
        nluResultListener.setUiHandler(handler);
    }

    /**
     * <p>完成sdk初始化流程，注册以下监听器：<br>
     * <p>UIManager: 服务器返回结果，包括tts文本数据，控制命令数据，私有领域数据<br>
     * <p>Device: 设定类，客户如果希望实现缓存数据加密，需要自行实现 corypt和decorypt<br>
     * <p>@param ctx Application::Context 或 Activity::Context <br>
     * <p>@param devSecretKey 开放平台创建设备时候，会生成biz_type, biz_group, devSecretKey, \
     *                     biz_type和biz_group需要在assets/prodconf.json中配置 <br>
     */
    public void initGenieSdk(Context ctx, String devSecretKey) {
        contex = ctx.getApplicationContext();
        SystemInfo.setContext(contex);
        //startService("com.alibaba.ailabs.geniesdk.NativeService");
        String path = contex.getFilesDir() + "";
        String libPath = contex.getApplicationInfo().nativeLibraryDir;
        Log.e("test", "libPath = " + libPath);

        File caFile = new File("/data/data/com.alibaba.sdk.aligeniesdkdemo/lib/libcapem.so");
        Device device;
        if (caFile.exists()) {
            device = new Device(path, null, contex, devSecretKey,
                    "/data/data/com.alibaba.sdk.aligeniesdkdemo/lib/libcapem.so");
        } else {
            device = new Device(path, null, contex, devSecretKey,
                    libPath + "/libcapem.so");
        }

        SessionManager sessionManager = new SessionManager(contex);
        Controller controller = Controller.getInstance(contex, device, sessionManager, null);
        controller.setUiManager(uiManager);
        AudioRecorderAdapter.getInstance().setParams(16000,
                1/*AudioFormat.CHANNEL_IN_STEREO*/,
                MediaRecorder.AudioSource.MIC/*MediaRecorder.AudioSource.VOICE_RECOGNITION*/,
                AudioFormat.ENCODING_PCM_16BIT, SessionID.INPUT_REMOTE_SPEAKER);
        AiLabsCore.getInstance().setGlobalRecordDevice(new NativeRecorder(AudioRecorderAdapter.getInstance()));
        NluRusultBridge.getInstance().setNluResultCallback(nluResultListener);
        AiLabsCore.getInstance().setEnableDebug(true);
        AiLabsCore.getInstance().setModeChange(AiLabsCore.SCREEN_TYPE, AiLabsCore.SCREEN_MODE);
        controller.getAiLabsCore().setUserTrace(new NativeUserTracer(this));
        //DeviceSettings.getInstance().setWakupSoundEnable(true);
    }

    /**
     * <p>开启vad<br>
     * <p>开启vad以后，sdk会自动识别本轮语音输入是否结束<br>
     */
    public void enableVad() {
        AudioRecorderAdapter.getInstance().enableVad();
    }

    /**
     * <p>关闭vad<br>
     */
    public void disableVad() {AudioRecorderAdapter.getInstance().disableVad();
    }

    /**
     * <p>每次按下语音键时候，调用该接口<br>
     */
    public void startTalk() {
        GatewayBridge.fakeWakeup();
        MediaOutputBridge.getInstance().stopTTSPlaying();
        MediaOutputBridge.getInstance().suspendAudioPlaying();
        DeviceController.getInstance().clear();
        DeviceState.getInstance().setState(DeviceState.WAKE);
        AudioRecorderAdapter.getInstance().startTalk();
    }

    public void cleanPlay(){
        MediaOutputBridge.getInstance().stopTTSPlaying();
        DeviceController.getInstance().clear();
        stopTalk();
        DeviceState.getInstance().setState(DeviceState.WAKE);
    }

    /**
     * <p>每次抬起语音键时候，调用该接口<br>
     */
    public void stopTalk() {
        //if (!AudioRecorderAdapter.getInstance().getVadStatus()) {
            AudioRecorderAdapter.getInstance().stopTalk();
        //}
    }

    /**
     * <p>将文本转换成tts语音，并播报<br>
     */
    public void textToTts(String text) {
        MediaOutputBridge.getInstance().playTts(text);
    }

    /**
     * <p>关闭sdk内部调试信息<br>
     */
    public void setDebugStatus(Boolean status) {
        AiLabsCore.getInstance().setEnableDebug(status);
    }

    public void stopWebsocket() {
        AiLabsCore.getInstance().stopService(AiLabsCore.SERVICE_GATEWAY|AiLabsCore.SERVICE_ASR);
    }

    public void startWebsocket() {
        AiLabsCore.getInstance().startService(AiLabsCore.SERVICE_GATEWAY|AiLabsCore.SERVICE_ASR);
    }

//    public void ignoreNextTts() {
//        MediaOutputBridge.getInstance().ignoreNextTts(true);
//    }

    public void ignoreThisSession(int sessionId) {
        MediaOutputBridge.getInstance().ignoreThisSession(sessionId);
    }
//
//    public void enableExpectSpeech() {
//        MediaOutputBridge.getInstance().enableExpectSpeech();
//    }
//
//    public void disableExpectSpeech() {
//        MediaOutputBridge.getInstance().disableExpectSpeech();
//    }

    @Override
    public void send(String type, String name, String content, Map<String, String> systemInfo) {
        try {
            JSONObject contextInfo = JSONObject.parseObject(content);
            STSLogger.getInstance().uploadLogAsyncWithSystemConfig(type, name, contextInfo, systemInfo);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void syncVolumeLevel(Context context) {
        Controller.getInstance().getStatusManager().statusChange(StatusManager.NOTIFY_SPEAKER_STATUS, SystemStatusUtis.getSpeakerStatus(context), true);
    }
}
