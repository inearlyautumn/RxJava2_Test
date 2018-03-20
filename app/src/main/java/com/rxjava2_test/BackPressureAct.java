package com.rxjava2_test;

import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;

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

    private Subscription mSubscription; // 用于保存Subscription对象
    private Button btnSeven; // 该按钮用于调用Subscription.request（long n ）


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_back_pressure);

        findViewById(R.id.btn_one).setOnClickListener(this);
        findViewById(R.id.btn_two).setOnClickListener(this);
        findViewById(R.id.btn_three).setOnClickListener(this);
        findViewById(R.id.btn_four).setOnClickListener(this);
        findViewById(R.id.btn_five).setOnClickListener(this);
        findViewById(R.id.btn_six).setOnClickListener(this);
//        findViewById(R.id.btn_seven).setOnClickListener(this);
        btnSeven = (Button) findViewById(R.id.btn_seven);

//        test1();
//        test2();
//        test3();
//        test4();
//        test5();
        //讲解 s.request(n);
//                test6();
//        test7();
        test8();
    }

    /**
     *
     */
    private void test8() {

    }

    /**
     * 被观察者 发送事件速度太快，而观察者 来不及接收所有事件，从而导致观察者无法及时响应 / 处理所有发送过来事件的问题，
     * 最终导致缓存区溢出、事件丢失 & OOM
     */
    private void test1() {
        Observable.create(new ObservableOnSubscribe<Integer>() {
            @Override
            public void subscribe(ObservableEmitter<Integer> e) throws Exception {
                for (int i = 0; i < 30; i++) {
                    LogUtil.i(TAG, "发送了事件 " + i);
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
                        LogUtil.i(TAG, "开始采用subscribe链接");
                    }

                    @Override
                    public void onNext(Integer integer) {
                        try {
                            Thread.sleep(5000);
                            //接收事件速度: 5s/个
                            LogUtil.i(TAG, "接收到了事件 " + integer);
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
                        LogUtil.i(TAG, "对Complete事件作出响应");
                    }
                });
    }

    /**
     * 采用 Flowable 实现 背压策略
     */
    private void test2() {
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
                LogUtil.i(TAG, "onNext: " + integer);
            }

            @Override
            public void onError(Throwable t) {
                LogUtil.i(TAG, "onError: " + t);
            }

            @Override
            public void onComplete() {
                LogUtil.i(TAG, "onComplete: ");
            }
        };
        //步骤三：建立订阅关系
        upstream.subscribe(downstream);
    }


    /**
     * 被观察者根据观察者自身接收事件能力（10个事件），从而仅发送10个事件
     */
    private void test7() {
        Flowable.create(new FlowableOnSubscribe<Integer>() {
            @Override
            public void subscribe(FlowableEmitter<Integer> e) throws Exception {
                //调用 e.requested() 来获取当前观察者需要接收的事件数量
                long n = e.requested();

                LogUtil.i(TAG, "观察者可接收事件 " + n);
                //根据 e.requested() 的值，即当前观察者需要接收的事件数量来发送事件
                for (int i = 0; i < n; i++) {
                    LogUtil.i(TAG, "发送了事件 " + i);
                    e.onNext(i);
                }
            }
        }, BackpressureStrategy.ERROR)
                .subscribe(new Subscriber<Integer>() {
                    @Override
                    public void onSubscribe(Subscription s) {
                        LogUtil.i(TAG, "onSubscribe");
                        //设置观察者每次能接受11个事件
                        s.request(11);
                    }

                    @Override
                    public void onNext(Integer integer) {
                        LogUtil.i(TAG, "接收到了事件 " + integer);
                    }

                    @Override
                    public void onError(Throwable t) {
                        LogUtil.i(TAG, "onError");
                    }

                    @Override
                    public void onComplete() {
                        LogUtil.i(TAG, "onComplete");
                    }
                });
    }

    /**
     * 同步订阅
     * <p>
     * 同步订阅 & 异步订阅 的区别在于：
     * - 同步订阅中，被观察者 & 观察者工作于同1线程
     * - 同步订阅关系中没有缓存区
     */
    private void test6() {
        //被观察者在发送1个事件后，必须等待观察者接收后，才能继续发下1个事件

        //步骤一：创建被观察者
        Flowable<Integer> upstream = Flowable.create(new FlowableOnSubscribe<Integer>() {
            @Override
            public void subscribe(FlowableEmitter<Integer> e) throws Exception {
                // 发送3个事件
                LogUtil.i(TAG, "发送了事件1");
                e.onNext(1);
                LogUtil.i(TAG, "发送了事件2");
                e.onNext(2);
                LogUtil.i(TAG, "发送了事件3");
                e.onNext(3);
                e.onComplete();
            }
        }, BackpressureStrategy.ERROR);

        //步骤二：创建观察者 = Subscriber
        Subscriber<Integer> downstream = new Subscriber<Integer>() {
            @Override
            public void onSubscribe(Subscription s) {
                LogUtil.i(TAG, "onSubscribe");
                s.request(3);
                //每次可接收事件 = 3 二次匹配(观察者只能接受3个事件，但被观察者却发送了4个事件，所以出现了不匹配情况)
            }

            @Override
            public void onNext(Integer integer) {
                LogUtil.i(TAG, "接收到了事件" + integer);
            }

            @Override
            public void onError(Throwable t) {
                LogUtil.i(TAG, "onError");
            }

            @Override
            public void onComplete() {
                LogUtil.i(TAG, "onComplete");
            }
        };

        //步骤三：建立订阅关系
        upstream.subscribe(downstream);
    }

    /**
     * 代码演示1：观察者不接收事件的情况下，被观察者继续发送事件 & 存放到缓存区；再按需取出
     */
    private void test5() {
        //步骤一：设置变量

        //步骤二：设置点击事件 = 调用Subscription.request(long n)
        btnSeven.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mSubscription.request(2);
            }
        });

        //步骤三：异步调用
        Flowable.create(new FlowableOnSubscribe<Integer>() {
            @Override
            public void subscribe(FlowableEmitter<Integer> e) throws Exception {
                LogUtil.i(TAG, "发送事件 1");
                e.onNext(1);
                LogUtil.i(TAG, "发送事件 2");
                e.onNext(2);
                LogUtil.i(TAG, "发送事件 3");
                e.onNext(3);
                LogUtil.i(TAG, "发送事件 4");
                e.onNext(4);
                LogUtil.i(TAG, "发送完成");
                e.onComplete();
            }
        }, BackpressureStrategy.ERROR)
                .subscribeOn(Schedulers.io())// 设置被观察者在io线程中进行
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(new Subscriber<Integer>() {
                    @Override
                    public void onSubscribe(Subscription s) {
                        LogUtil.i(TAG, "onSubscribe");
                        mSubscription = s;
                        //保存Subscription对象，等待点击按钮时（调用request(2)）观察者再接收事件
                    }

                    @Override
                    public void onNext(Integer integer) {
                        LogUtil.i(TAG, "接收到了事件" + integer);
                    }

                    @Override
                    public void onError(Throwable t) {
                        LogUtil.i(TAG, "onError" + t);
                    }

                    @Override
                    public void onComplete() {
                        LogUtil.i(TAG, "onComplete: ");
                    }
                });
    }

    private void test4() {
        //创建被观察者Flowable
        Flowable.create(new FlowableOnSubscribe<Integer>() {
            @Override
            public void subscribe(FlowableEmitter<Integer> e) throws Exception {
                // 一共发送4个事件
                LogUtil.i(TAG, "发送事件 1");
                e.onNext(1);
                LogUtil.i(TAG, "发送事件 2");
                e.onNext(2);
                LogUtil.i(TAG, "发送事件 3");
                e.onNext(3);
                LogUtil.i(TAG, "发送事件 4");
                e.onNext(4);
                LogUtil.i(TAG, "发送完成");
                e.onComplete();
            }
        }, BackpressureStrategy.ERROR)
                .subscribeOn(Schedulers.io()) //设置被观察者在io线程中进行(在io线程处理完所有操作后再到主线程)
                .observeOn(AndroidSchedulers.mainThread())  //设置观察者在主线程中进行
                .subscribe(new Subscriber<Integer>() {
                    @Override
                    public void onSubscribe(Subscription s) {
                        /*
                         * 对比Observer传入的Disposable参数，Subscriber此处传入的参数 = Subscription
                         * 相同点：Subscription参数具备Disposable参数的作用，即Disposable.dispose()切断连接，
                         * 同样的调用Subscription.cancel（）切断连接
                         *
                         * 不同点：Subscription增加了void request(long n)
                         * */
                        s.request(3);
                        /*
                        * 作用：决定观察者能够接收多少个事件
                        *
                        * 如设置了 s.request(3)，这就说明观察者能够接收3个事件（多出的事件存放在级存区）
                        *
                        * 官方默认推荐使用Long.MAX_VALUE， 即s.request(Long.MAX_VALUE);
                        * */

                    }

                    @Override
                    public void onNext(Integer integer) {
                        LogUtil.i(TAG, "接收到了事件" + integer);
                    }

                    @Override
                    public void onError(Throwable t) {
                        LogUtil.i(TAG, "onError: " + t);
                    }

                    @Override
                    public void onComplete() {
                        LogUtil.i(TAG, "onComplete");
                    }
                });
    }

    private void test3() {
        //步骤一：创建被观察者  =  Flowable
        Flowable.create(new FlowableOnSubscribe<Integer>() {
            @Override
            public void subscribe(FlowableEmitter<Integer> e) throws Exception {
                LogUtil.i(TAG, "发送事件 1 ");
                e.onNext(1);
                LogUtil.i(TAG, "发送事件 2 ");
                e.onNext(2);
                LogUtil.i(TAG, "发送事件 3 ");
                e.onNext(3);
                LogUtil.i(TAG, "发送完成");
                e.onComplete();
            }
        }, BackpressureStrategy.ERROR)
                .subscribe(new Subscriber<Integer>() {
                    //步骤二：创建观察者  =  Subscriber & 建立订阅关系

                    @Override
                    public void onSubscribe(Subscription s) {
                        LogUtil.i(TAG, "onSubscribe");
                        s.request(3);
                    }

                    @Override
                    public void onNext(Integer integer) {
                        LogUtil.i(TAG, "接收到了事件 " + integer);
                    }

                    @Override
                    public void onError(Throwable t) {
                        LogUtil.i(TAG, "onError：" + t);
                    }

                    @Override
                    public void onComplete() {
                        LogUtil.i(TAG, "onComplete");
                    }
                });
    }
}
