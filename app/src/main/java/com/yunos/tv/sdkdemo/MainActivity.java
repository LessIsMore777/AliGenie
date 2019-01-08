package com.yunos.tv.sdkdemo;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.media.MediaPlayer;
import android.net.Uri;
import android.nfc.Tag;
import android.os.Handler;
import android.os.Message;
import android.os.Process;
import android.os.Build;
import android.os.Bundle;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.view.WindowManager;
import android.view.animation.AlphaAnimation;
import android.view.animation.Animation;
import android.view.animation.AnimationSet;
import android.view.animation.ScaleAnimation;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.alibaba.ailabs.custom.core.DeviceState;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.hankcs.hanlp.HanLP;
import com.hankcs.hanlp.corpus.tag.Nature;
import com.hankcs.hanlp.seg.common.Term;
import com.hankcs.hanlp.tokenizer.StandardTokenizer;
import com.yunos.tv.sdkdemo.skills.call.model.ContactPhone;
import com.yunos.tv.sdkdemo.skills.call.utils.PinYin4j;
import com.yunos.tv.sdkdemo.ui.loadingdialog.LoadingDialog;
import com.yunos.tv.sdkdemo.utils.DialogUtils;


import java.util.ArrayList;
import java.util.List;


public class MainActivity extends Activity {
    private static String TAG = "MainActivity";
    private Button wakeup;
    private TextView spokenText;
    private TextView tv_ing;
    LoadingDialog pb;


    private ImageView mNormal, mWave1, mWave2, mWave3;

    private AnimationSet mAnimationSet1, mAnimationSet2, mAnimationSet3;

    private static final int OFFSET = 200;  //每个动画的播放时间间隔
    private static final int MSG_WAVE2_ANIMATION = 2;
    private static final int MSG_WAVE3_ANIMATION = 3;

    @SuppressLint("HandlerLeak")
    private Handler mHandler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case MSG_WAVE2_ANIMATION:
                    mWave2.setVisibility(View.VISIBLE);
                    mWave2.startAnimation(mAnimationSet2);
                    break;
                case MSG_WAVE3_ANIMATION:
                    mWave3.setVisibility(View.VISIBLE);
                    mWave3.startAnimation(mAnimationSet3);
                    break;
            }
        }
    };


    private Handler handler = new Handler(new Handler.Callback() {
        @Override
        public boolean handleMessage(Message msg) {
            String result = msg.getData().getString("contant");
            spokenText.setText(result);
            switch (msg.what){
                case 1:
                    break;
                case 2:
                    if(result != null) {
                        boolean isFind = false;
                        List<ContactPhone> contactPhones = queryContactPhoneNumber();
                        for(final ContactPhone contactPhone : contactPhones) {
                            if(result.contains(contactPhone.getPhoneName())){
                                isFind = true;
                                SdkControllerAdaptor.getInstance().textToTts("好的，准备打电话给" + contactPhone.getPhoneName());
                                callPhone(contactPhone.getPhoneNumber());
                            }
                        }
                        if(!isFind) {
                            SdkControllerAdaptor.getInstance().textToTts("抱歉，电话本里没有该联系人~");
                        }
                    }
                    break;
            }
            return false;
        }
    });

    /**
     * 匹配通讯录 (拼音模糊查询)
     * 需要分词,本地技能无法实现，直接报溢出内存错误
     * @param Targetname
     * @return
     */
    private ContactPhone PhoneCallMatch(String Targetname) {
        List<ContactPhone> listPhones = queryContactPhoneNumber();
        if (listPhones.size() == 0) {
            return null;
        }
        PinYin4j  pinYin4j = new PinYin4j();
        String TargetnamePY = pinYin4j.makeStringByStringSet(pinYin4j.getPinyin(Targetname));
        for (ContactPhone phone : listPhones) {
            String phoneNamePy = pinYin4j.makeStringByStringSet(pinYin4j.getPinyin(phone.getPhoneName()));
            if (TargetnamePY.contains(phoneNamePy)||phoneNamePy.contains(TargetnamePY)) {
                return phone;
            }
        }
        return null;
    }

    /**
     * 获取通讯录
     * @return
     */
    public List<ContactPhone> queryContactPhoneNumber(){
        try {
            List<ContactPhone> list = new ArrayList<>();
            //跨应用拿sharedPreferences数据，手表android4.4系统可以实现
            Context mContext = createPackageContext("com.toycloud.watch.launcher", CONTEXT_IGNORE_SECURITY);
            SharedPreferences sharedPreferences = mContext.getSharedPreferences("launcher_share", MODE_PRIVATE);
            String sp = sharedPreferences.getString("CONTACTS", null);

            JSONObject jsonObject = JSONObject.parseObject(sp);
            JSONArray json = jsonObject.getJSONArray("contactslist");
            if(json.size()>0){
                for(int i=0;i<json.size();i++){
                    JSONObject job = json.getJSONObject(i);
                    list.add(new ContactPhone(job.getString("name"),job.getString("phone")));
                }
            }
            return list;
        }catch (Exception e){
            Log.d(TAG,"Query Contact Error");
        }
        return null;
    }

    /**
     * 拨打电话
     * @param phoneNumber
     */

    private void callPhone(String phoneNumber) {
        Intent intent = new Intent(Intent.ACTION_CALL);
        Uri data = Uri.parse("tel:" + phoneNumber);
        intent.setData(data);
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.CALL_PHONE) != PackageManager.PERMISSION_GRANTED) {
            return;
        }
        startActivity(intent);
    }

    @Override
    protected void onStop() {
        if((DeviceState.getInstance().getState() & DeviceState.PLAYING) == 0) {
            SdkControllerAdaptor.getInstance().stopWebsocket();
        }
        super.onStop();
    }

    @Override
    protected void onResume() {
        super.onResume();
        SdkControllerAdaptor.getInstance().startWebsocket();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        setContentView(R.layout.activity_main);
//        GifImageView wakeup = findViewById(R.id.wakeupButton);
        wakeup = findViewById(R.id.wakeup);
        tv_ing = findViewById(R.id.tv_ing);
        spokenText = findViewById(R.id.spokenText);
        mNormal = (ImageView) findViewById(R.id.normal);
        mWave1 = (ImageView) findViewById(R.id.wave1);
        mWave2 = (ImageView) findViewById(R.id.wave2);
        mWave3 = (ImageView) findViewById(R.id.wave3);

        mAnimationSet1 = initAnimationSet();
        mAnimationSet2 = initAnimationSet();
        mAnimationSet3 = initAnimationSet();


        requestPermission(Manifest.permission.ACCESS_COARSE_LOCATION);
        requestPermission(Manifest.permission.ACCESS_FINE_LOCATION);
        requestPermission(Manifest.permission.RECORD_AUDIO);
        requestPermission(Manifest.permission.WAKE_LOCK);
        SdkControllerAdaptor.getInstance().enableVad();
//      SdkControllerAdaptor.getInstance().disableExpectSpeech();
        pb = DialogUtils.getDialod(this, "聆听中..");
        wakeup.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                switch (event.getAction()) {
                    case MotionEvent.ACTION_UP://松开事件发生后执行代码的区域
                        v.setBackgroundResource(R.drawable.speak_normal);
                        pb.dismiss();
                        clearWaveAnimation();
//                        tv_ing.setVisibility(View.GONE);
                        //SdkControllerAdaptor.getInstance().stopTalk();
                        break;
                    case MotionEvent.ACTION_DOWN://按住事件发生后执行代码的区域
//                        v.setBackgroundColor(0xff0000ff);
//                        tv_ing.setVisibility(View.VISIBLE);
                        pb.show();
                        v.setBackgroundResource(R.drawable.speak_press);
                        SdkControllerAdaptor.getInstance().startTalk();
                        showWaveAnimation();
                        //SdkControllerAdaptor.getInstance().textToTts("小主人，你好，我是小町，是你的好朋友");
                        break;
                    case MotionEvent.ACTION_CANCEL:
//                        tv_ing.setVisibility(View.GONE);
                        clearWaveAnimation();
//                        pb.dismiss();
                    default:
                        break;
                }
                return true;
            }
        });

        SdkControllerAdaptor.getInstance().setUiHandler(handler);
    }

    @Override
    public void onRequestPermissionsResult(int code, String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(code, permissions, grantResults);
        Toast.makeText(this, "permission not granted!", Toast.LENGTH_SHORT).show();
    }

    private void requestPermission(String permission) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {

            if (ContextCompat.checkSelfPermission(this, permission) ==
                    PackageManager.PERMISSION_GRANTED) {
                return;
            }
            if (shouldShowRequestPermissionRationale(permission)) {
                Toast.makeText(this, "App required access to audio", Toast.LENGTH_SHORT).show();
            }
            requestPermissions(new String[]{permission}, 1);
        }
    }

    private AnimationSet initAnimationSet() {
        AnimationSet as = new AnimationSet(true);
        ScaleAnimation sa = new ScaleAnimation(1f, 1.5f, 1f, 1.5f,
                ScaleAnimation.RELATIVE_TO_SELF, 0.5f,
                ScaleAnimation.RELATIVE_TO_SELF, 0.5f);
        sa.setDuration(OFFSET * 2);
        sa.setRepeatCount(Animation.INFINITE);// 设置循环
        AlphaAnimation aa = new AlphaAnimation(1, 0.1f);
        aa.setDuration(OFFSET * 2);
        aa.setRepeatCount(Animation.INFINITE);//设置循环
        as.addAnimation(sa);
        as.addAnimation(aa);
        return as;
    }

    private void showWaveAnimation() {
        mWave1.setVisibility(View.VISIBLE);
        mWave1.startAnimation(mAnimationSet1);
        mHandler.sendEmptyMessageDelayed(MSG_WAVE2_ANIMATION, OFFSET);
        mHandler.sendEmptyMessageDelayed(MSG_WAVE3_ANIMATION, OFFSET * 2);
    }

    private void clearWaveAnimation() {
        mWave1.clearAnimation();
        mWave2.clearAnimation();
        mWave3.clearAnimation();

        mWave1.setVisibility(View.GONE);
        mWave2.setVisibility(View.GONE);
        mWave3.setVisibility(View.GONE);
    }

}