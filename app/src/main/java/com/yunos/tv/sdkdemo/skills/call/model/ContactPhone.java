package com.yunos.tv.sdkdemo.skills.call.model;

/**
 * Created by nhuan
 * Time:2019/1/2.
 */

public class ContactPhone {
    private String phoneName;
    private String phoneNumber;

    public ContactPhone(String phoneName, String phoneNumber) {
        this.phoneName = phoneName;
        this.phoneNumber = phoneNumber;
    }

    public String getPhoneName() {
        return phoneName;
    }

    public void setPhoneName(String phoneName) {
        this.phoneName = phoneName;
    }

    public String getPhoneNumber() {
        return phoneNumber;
    }

    public void setPhoneNumber(String phoneNumber) {
        this.phoneNumber = phoneNumber;
    }
}
