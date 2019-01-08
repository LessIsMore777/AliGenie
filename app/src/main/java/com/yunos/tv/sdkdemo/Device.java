package com.yunos.tv.sdkdemo;

import android.content.Context;
import android.net.wifi.WifiManager;
import android.os.Build;
import android.os.PowerManager;

import com.alibaba.ailabs.custom.audio.TtsManager;
import com.alibaba.ailabs.custom.command.BaseCommand;
import com.alibaba.ailabs.custom.command.CommandName;
import com.alibaba.ailabs.custom.command.CommandParser;
import com.alibaba.ailabs.custom.logger.STSLogger;
import com.alibaba.ailabs.custom.util.LogUtils;
import com.alibaba.ailabs.custom.util.StringUtil;
import com.alibaba.ailabs.geniesdk.Device.DeviceInfo;
import com.alibaba.ailabs.geniesdk.Device.IDevice;
import com.yunos.tv.alitvasr.controller.Controller;

import java.net.NetworkInterface;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;

/**
 * <p>设备相关接口，如果需要对缓存数据加密，需要厂商自行实现encrypt 和 encrypt<br>
 */

public class Device implements IDevice {
    private DeviceInfo mDeviceInfo = null;
    private Context mContext;
    private final static int DEVICE_ID_TYPE_MAC = 0;
    private final static int DEVICE_ID_TYPE_SN = 1;

    public Device(String workPath, String configPath, Context context, String devSecretKey, String caPath) {
        mDeviceInfo = new DeviceInfo();
        this.mContext = context.getApplicationContext();
        mDeviceInfo.devSecretKey = devSecretKey;
        mDeviceInfo.configPath = configPath;
        mDeviceInfo.workPath = workPath;
        //mDeviceInfo.sn = Build.SERIAL;
        mDeviceInfo.sn = "860860000032090";
        mDeviceInfo.systemVer = Build.VERSION.RELEASE;
        //mDeviceInfo.deviceIDType = DEVICE_ID_TYPE_MAC;
        mDeviceInfo.deviceIDType = 1;
        mDeviceInfo.caPath = caPath;
    }

    @Override
    public String encrypt(String src) {

        return src;
    }

    @Override
    public String decrypt(String src) {
        return src;
    }


    public String getMacAddr() {
        String wifiMac = "02:00:00:00:00:00";
        boolean needCloseWifi = false;
        WifiManager wifiManager = (WifiManager)mContext.getSystemService(Context.WIFI_SERVICE);
        try {
            if(wifiManager != null && !wifiManager.isWifiEnabled()){
                needCloseWifi = true;
                wifiManager.setWifiEnabled(true);
                for (int i=0; i<100; i++) {
                    if(wifiManager.isWifiEnabled()) {
                        break;
                    }
                    Thread.sleep(50);
                }
            }

            List<NetworkInterface> all = Collections.list(NetworkInterface.getNetworkInterfaces());
            for (NetworkInterface nif : all) {
                if (!nif.getName().equalsIgnoreCase("wlan0")) {
                    continue;
                }
                byte[] macBytes = nif.getHardwareAddress();
                if (macBytes == null) {
                    return "";
                }

                StringBuilder res1 = new StringBuilder();
                for (byte b : macBytes) {
                    res1.append(String.format("%02X:", b));
                }

                if (res1.length() > 0) {
                    res1.deleteCharAt(res1.length() - 1);
                }
                //return res1.toString();
                wifiMac = res1.toString();
                break;
            }

        } catch (Exception ex) {
            ex.printStackTrace();
        }
        if (needCloseWifi) {
            wifiManager.setWifiEnabled(false);
        }
        return wifiMac;
    }

    @Override
    public String getPublicIp() {
        return STSLogger.getInstance().getIp();
    }


    /**
     * @return
     */
    @Override
    public boolean getScreenStatus() {
        //return true;
        return false;
    }

    @Override
    public String getTvContext(String thridContext) {
        return "{\"systemInfo\":{\"area_code\":\"\",\"uuid\":\"928911AD5D41A7FD9BFCE90E0A86C96D\",\"device_model\":\"ChildrenWatch\",\"device_firmware_version\":\"6.1.0-R-20180417.0354\",\"firmware\":\"6.1.0-R-20180417.0354\",\"device_sn\":\"928911AD5D41A7FD9BFCE90E0A86C96D\"},\"protocolVersion\":1}";

    }

    @Override
    public DeviceInfo getDeviceInfo() {
        return mDeviceInfo;
    }

    private LinkedList<BaseCommand> cmdList = new LinkedList<BaseCommand>();

    public void sendCommand(final int sessionId, String command) {
        if (StringUtil.isEmpty(command)) {
            return;
        }
        LogUtils.d("####sendCommand: " + sessionId + command + "Mac：" + getMacAddr());

        final BaseCommand cmd = CommandParser.getInstance().parse(command, sessionId);

        if (cmd == null) {
            LogUtils.e("Not a valid command, don't deal!");
            return;
        }

//        if (CommandName.audioPlay.name().equals(cmd.getCommandName())) {
//            //TODO
//        }

        synchronized (cmdList) {
            int index = -1;
            for (int i = 0; i < cmdList.size(); i++) {
                LogUtils.d("cmdList:" + i);
                if (cmd.getPriority() > cmdList.get(i).getPriority()) {
                    LogUtils.d("need insert command before:" + i);
                    index = i;
                    break;
                }
            }
            if (index != -1) {
                cmdList.add(index, cmd);
            } else {
                cmdList.add(cmd);
            }

            if (cmd.isLast()) {
                final LinkedList<BaseCommand> exeCmdList = (LinkedList<BaseCommand>) cmdList.clone();
                cmdList.clear();
                new Thread() {
                    public void run() {
                        LogUtils.i("Will exec " + exeCmdList.size());
                        for (BaseCommand exe : exeCmdList) {
                            LogUtils.i(">>>>>>> Now exec cmd:" + exe.getCommandName());
                            exe.deal(sessionId);
                        }
                    }
                }.start();
            }
        }
//        if (cmd.getCommandName())) {
//            //TODO
//        }
//        new Thread() {
//            public void run() {
//                cmd.deal(sessionId);
//            }
//        }.start();
    }

    public void sendAttachment(int sessionId, byte[] data, int lenth) {
        LogUtils.d("####sendAttachment: " + sessionId);
        TtsManager.getInstance().appendData(sessionId, data, lenth);
    }

    public void sendAttachmentEnd(int sessionId) {
        LogUtils.d("####sendAttachmentEnd: " + sessionId);
        TtsManager.getInstance().appendDataFinish(sessionId);
    }
}