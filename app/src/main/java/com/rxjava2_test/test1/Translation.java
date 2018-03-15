package com.rxjava2_test.test1;

import android.util.Log;

/**
 * author: liweixing
 * date: 2018/3/15
 */

public class Translation {
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
        Log.d("RxJava", "---00 "+content.out );
    }

}
