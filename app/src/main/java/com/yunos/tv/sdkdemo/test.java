package com.yunos.tv.sdkdemo;

import com.hankcs.hanlp.tokenizer.StandardTokenizer;

/**
 * Created by nhuan
 * Time:2019/1/3.
 */

public class test {
    public static void main(String[] args) {

        System.out.println(StandardTokenizer.segment("今天天气很好，不如打电话给妮妮约个饭吧！"));
    }
}
