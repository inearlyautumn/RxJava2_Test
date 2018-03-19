package com.rxjava2_test;

import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;

import org.reactivestreams.Subscriber;
import org.reactivestreams.Subscription;

import io.reactivex.BackpressureStrategy;
import io.reactivex.Flowable;
import io.reactivex.FlowableEmitter;
import io.reactivex.FlowableOnSubscribe;
import io.reactivex.Observable;
import io.reactivex.ObservableEmitter;
import io.reactivex.ObservableOnSubscribe;
import io.reactivex.Observer;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.disposables.Disposable;
import io.reactivex.schedulers.Schedulers;

public class BackPressureAct extends AppCompatActivity {
    public static final String TAG = BackPressureAct.class.getSimpleName();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_back_pressure);

        test2();
        test3();
    }

    /**
     * 被观察者 发送事件速度太快，而观察者 来不及接收所有事件，从而导致观察者无法及时响应 / 处理所有发送过来事件的问题，
     * 最终导致缓存区溢出、事件丢失 & OOM
     */
    private void test2() {
        Observable.create(new ObservableOnSubscribe<Integer>() {
            @Override
            public void subscribe(ObservableEmitter<Integer> e) throws Exception {
                for (int i = 0; i < 30; i++) {
                    LogUtil.i(TAG, "发送了事件 "+i);
                    Thread.sleep(10);
                    //发送事件速度： 10ms/ 个
                    e.onNext(i);
                }
            }
        }).subscribeOn(Schedulers.io()) // 设置被观察者在io线程中进行
                .observeOn(AndroidSchedulers.mainThread()) // 设置观察者在主线程中进行
                .subscribe(new Observer<Integer>() {
                    @Override
                    public void onSubscribe(Disposable d) {
                        LogUtil.i(TAG,"开始采用subscribe链接");
                    }

                    @Override
                    public void onNext(Integer integer) {
                        try {
                            Thread.sleep(5000);
                            //接收事件速度: 5s/个
                            LogUtil.i(TAG,"接收到了事件 "+integer);
                        } catch (InterruptedException e) {
                            e.printStackTrace();
                        }
                    }

                    @Override
                    public void onError(Throwable e) {
                        LogUtil.i(TAG, "对Error事件作出响应");
                    }

                    @Override
                    public void onComplete() {
                        LogUtil.i(TAG,"对Complete事件作出响应");
                    }
                });
    }

    private void test3() {
        //步骤一：创建被观察者 = Flowable
        Flowable<Integer> upstream = Flowable.create(new FlowableOnSubscribe<Integer>() {
            @Override
            public void subscribe(FlowableEmitter<Integer> e) throws Exception {
                e.onNext(1);
                e.onNext(2);
                e.onNext(3);
                e.onComplete();
            }
        }, BackpressureStrategy.ERROR);
        //需要传入背压参数 BackpressureStrategy, 下面会详细讲解

        //步骤二：创建观察者  =   Subscriber
        Subscriber<Integer> downstream = new Subscriber<Integer>() {
            @Override
            public void onSubscribe(Subscription s) {
                /*
                * 对比 Observer 传入的 Disposable 参数， Subscriber 此处传入的参数 = Subscription
                *
                * 相同点：Subscription具备 Disposable参数的作用，即Disposable.dispose()切断连接，
                * 同样的调用 Subscription.cancel()切换连接
                *
                * 不同点：Subscription 增加了void request(long n)
                * */
                LogUtil.i(TAG, "onSubscribe");
                s.request(Long.MAX_VALUE);
                //关于request()下面会继续详细说明
            }

            @Override
            public void onNext(Integer integer) {
                LogUtil.i(TAG,"onNext: "+integer);
            }

            @Override
            public void onError(Throwable t) {
                LogUtil.i(TAG, "onError: "+t);
            }

            @Override
            public void onComplete() {
                LogUtil.i(TAG, "onComplete: ");
            }
        };
        //步骤三：建立订阅关系
        upstream.subscribe(downstream);
    }
}
