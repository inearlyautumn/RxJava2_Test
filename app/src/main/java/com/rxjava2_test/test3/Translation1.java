package com.rxjava2_test.test3;

import android.util.Log;

import com.rxjava2_test.LogUtil;

/**
 * author: liweixing
 * date: 2018/3/16
 */

public class Translation1 {
    private int status;
    private content content;

    private static class content {
        private String from;
        private String to;
        private String vendor;
        private String out;
        private int errNo;
    }

    //定义 输出返回数据 的方法
    public void show() {

        LogUtil.i("RxJava", "翻译内容 = " + content.out);

    }
}
