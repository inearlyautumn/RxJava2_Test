package com.rxjava2_test;

import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;

import com.jakewharton.retrofit2.adapter.rxjava2.RxJava2CallAdapterFactory;
import com.rxjava2_test.test1.GetRequest_Interface;
import com.rxjava2_test.test1.Translation;

import java.util.concurrent.TimeUnit;

import io.reactivex.Observable;
import io.reactivex.Observer;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.disposables.Disposable;
import io.reactivex.functions.Consumer;
import io.reactivex.schedulers.Schedulers;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

public class MainActivity extends AppCompatActivity {
    public static final String TAG = MainActivity.class.getSimpleName();


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        findViewById(R.id.btn_cancel).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

            }
        });
//        test1();
    }


    /**
     * 网络请求轮询（无条件）
     */
    private void test1() {
                /*
        * 步骤1：采用interval()延迟发送
        * 注：此处主要展示无限次轮询，仅需interval()改成intervalRange()即可
        * */
        Observable.interval(2, 1, TimeUnit.SECONDS)
                //参数说明：
                //参数1：第1次延时时间；
                //参数2：间隔时间数字；
                //参数3：时间单位；
                //该例子发送的事件特点：延迟2s后发送事件，每隔1秒产生一个数字(从0开始递增1，无限个)

                /*
                * 步骤2：每次发送数字前发送1次网络请求(doOnNext()在执行Next事件前调用)
                * 即每隔1秒产生1个数字前，就发送1次网络请求，从而实现轮询需求
                * */
                .doOnNext(new Consumer<Long>() {
                    @Override
                    public void accept(Long aLong) throws Exception {
                        Log.i(TAG, "accept: ---00 第 " + aLong + " 次轮询");
                        //步骤3：通过Retrofit发送网络请求
                        //a. 创建Retrofit对象
                        Retrofit retrofit = new Retrofit.Builder()
                                .baseUrl("http://fy.iciba.com/")
                                .addConverterFactory(GsonConverterFactory.create())
                                .addCallAdapterFactory(RxJava2CallAdapterFactory.create())
                                .build();

                        //b. 创建 网络请求接口的实例
                        GetRequest_Interface request = retrofit.create(GetRequest_Interface.class);

                        //c. 采用Observable<...>形式 对 网络请求 进行封装
                        Observable<Translation> observable = request.getCall();
                        //d. 通过线程切换发送网络请求
                        observable.subscribeOn(Schedulers.io())    //切换到IO线程进行网络请求
                                .observeOn(AndroidSchedulers.mainThread())  //切换回到主线程 处理请求结果
                                .subscribe(new Observer<Translation>() {
                                    @Override
                                    public void onSubscribe(Disposable d) {

                                    }

                                    @Override
                                    public void onNext(Translation translation) {
                                        //e. 接收服务器返回的数据
                                        translation.show();
                                    }

                                    @Override
                                    public void onError(Throwable e) {
                                        Log.i(TAG, "onError: ---00 请求失败");
                                    }

                                    @Override
                                    public void onComplete() {

                                    }
                                });
                    }
                })
                .subscribe(new Observer<Long>() {
                    @Override
                    public void onSubscribe(Disposable d) {

                    }

                    @Override
                    public void onNext(Long aLong) {

                    }

                    @Override
                    public void onError(Throwable e) {
                        Log.i(TAG, "onError: ---00 对Error事件作出响应");
                    }

                    @Override
                    public void onComplete() {
                        Log.i(TAG, "onComplete: ---00 对complete事件作出响应");
                    }
                });
    }


}
