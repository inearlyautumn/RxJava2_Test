package com.rxjava2_test;

import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.widget.EditText;
import android.widget.TextView;

import com.jakewharton.rxbinding2.widget.RxTextView;

import java.util.concurrent.TimeUnit;

import io.reactivex.Observer;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.disposables.Disposable;

public class AssociativeSearch extends AppCompatActivity {
    public static final String TAG = AssociativeSearch.class.getSimpleName();

    private EditText ed;
    private TextView tv;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_associative_search);

        ed = (EditText) findViewById(R.id.ed);
        tv = (TextView) findViewById(R.id.tv);

        /*
        * 说明
        * 1，此处采用了RxBinding：RxTextView.textChanges(name) = 对控件数据变更进行监听（功能类似TextWatcher），
        * 需要引入依赖：compile 'com.jakewharton.rxbinding2:rxbinding:2.0.0'
        *
        * 2，传入EditText控件，输入字符时都会发送数据事件（此处不会马上发送，因为使用了debounce()）
        *
        * 3，采用skip(1)原因：跳过 第1次请求 = 初始输入框的空字符状态
        * */
        RxTextView.textChanges(ed)
                .debounce(1, TimeUnit.SECONDS)
                .skip(1)
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(new Observer<CharSequence>() {
                    @Override
                    public void onSubscribe(Disposable d) {

                    }

                    @Override
                    public void onNext(CharSequence charSequence) {
                        tv.setText("发送给服务器的字符 = " + charSequence.toString());
                    }

                    @Override
                    public void onError(Throwable e) {
                        LogUtil.i(TAG, "对Error事件作出响应");
                    }

                    @Override
                    public void onComplete() {
                        LogUtil.i(TAG, "对Complete事件作出响应");
                    }
                });
    }
}
