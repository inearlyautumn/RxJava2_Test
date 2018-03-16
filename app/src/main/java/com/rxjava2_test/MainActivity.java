package com.rxjava2_test;

import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;

import com.google.gson.Gson;
import com.jakewharton.retrofit2.adapter.rxjava2.RxJava2CallAdapterFactory;
import com.rxjava2_test.test1.GetRequest_Interface;
import com.rxjava2_test.test1.Translation;
import com.rxjava2_test.test3.Translation1;
import com.rxjava2_test.test3.Translation2;

import java.io.IOException;
import java.util.concurrent.TimeUnit;

import io.reactivex.Observable;
import io.reactivex.ObservableEmitter;
import io.reactivex.ObservableOnSubscribe;
import io.reactivex.ObservableSource;
import io.reactivex.Observer;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.disposables.Disposable;
import io.reactivex.functions.BiFunction;
import io.reactivex.functions.Consumer;
import io.reactivex.functions.Function;
import io.reactivex.schedulers.Schedulers;
import okhttp3.Cache;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;
import retrofit2.http.GET;

public class MainActivity extends AppCompatActivity {
    public static final String TAG = MainActivity.class.getSimpleName();

    //博客地址：http://blog.csdn.net/carson_ho/article/details/79168723

    //可重试次数
    private int maxConnectCount = 10;
    //当前已重试次数
    private int currentRetryCount = 0;
    //重试等待时间
    private int waiRetryTime = 0;

    //定义 Observable 接口类型的网络请求对象
    Observable<Translation1> observable1;
    Observable<Translation2> observable2;

    private String memoryCache = null;
    private String diskCache = "从磁盘缓存中获取数据";
    private String result = "数据源来自 = ";

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
//        test2();
//        test3();
//        test5();
//        test6();
//        test7();
        test8();
    }

    /**
     * 联合判断
     * 需要同时对多个事件进行联合判断
     * 如：填写表单时，需要表单里所有信息（姓名、年龄、职业等）都被填写后，才允许点击“提交”按钮
     */
    private void test8() {


    }

    /**
     * 合并数据源 zip方式
     */
    private void test7() {
        //采用 Zip() 操作符

        /*
        * 1、从不同数据源（2个服务器）获取数据，即 合并网络请求的发送
        * 2、统一显示结果
        * */
        Retrofit retrofit = new Retrofit.Builder()
                .baseUrl("http://fy.iciba.com/")
                .addConverterFactory(GsonConverterFactory.create())
                .addCallAdapterFactory(RxJava2CallAdapterFactory.create())
                .build();

        GetRequest_Interface request = retrofit.create(GetRequest_Interface.class);

        observable1 = request.getCall_1().subscribeOn(Schedulers.io());// 新开线程进行网络请求1
        observable2 = request.getCall_2().subscribeOn(Schedulers.io());// 新开线程进行网络请求2
        //即2个网络请求异步 & 同时发送

        //通过使用 Zip() 对两个网络请求进行合并再发送
        Observable.zip(observable1, observable2, new BiFunction<Translation1, Translation2, String>() {
            // 注：创建 BiFunction 对象传入的第3个参数 = 合并后数据的数据类型
            @Override
            public String apply(Translation1 translation1, Translation2 translation2) throws Exception {
                return translation1.show2() + " & " + translation2.show2();
            }
        })
                .observeOn(AndroidSchedulers.mainThread())// 在主线程接收 & 处理数据
                .subscribe(new Consumer<String>() {
                    //成功返回数据时调用
                    @Override
                    public void accept(String s) throws Exception {
                        //结合显示 2 个网络请求的数据结果
                        LogUtil.i(TAG, "最终接收到的数据是： " + s);
                    }
                }, new Consumer<Throwable>() {
                    //网络错误时调用
                    @Override
                    public void accept(Throwable throwable) throws Exception {
                        LogUtil.i(TAG, "登录失败");
                    }
                });
    }

    /**
     * 合并数据源 merge方式
     */
    private void test6() {
        // 采用 Merge() 操作符

        /*
        * 设置第1个Observable: 通过网络获取数据
        * 此处仅作网络请求的模拟
        * */
        Observable<String> network = Observable.just("网络");
        /*
        * 设置第2个Observable:通过本地文件获取数据
        * 此处仅作本地文件请求的模拟
        * */
        Observable<String> file = Observable.just("本地文件");

        //通过 merge() 合并事件 & 同时发送事件
        Observable.merge(network, file)
                .subscribe(new Observer<String>() {
                    @Override
                    public void onSubscribe(Disposable d) {

                    }

                    @Override
                    public void onNext(String s) {
                        LogUtil.i(TAG, "数据源有： " + s);
                        result += s + "+";
                    }

                    @Override
                    public void onError(Throwable e) {
                        LogUtil.i(TAG, "对Error事件作出响应");
                    }

                    /**
                     * 接收合并事件后，统一展示
                     */
                    @Override
                    public void onComplete() {
                        LogUtil.i(TAG, "获取数据完成");
                        LogUtil.i(TAG, result);
                    }
                });
    }

    /**
     * 从磁盘 / 内存缓存中 获取缓存数据
     * 需要进行的嵌套网络的请求：即在第1个网络请求成功后，继续再进行一次网络请求
     */
    private void test5() {

        //设置第1个Observable：检查内存缓存是否有该数据的缓存
        Observable<String> memory = Observable.create(new ObservableOnSubscribe<String>() {
            @Override
            public void subscribe(ObservableEmitter<String> e) throws Exception {
                //先判断内存缓存有无数据
                if (memoryCache != null) {
                    //若有该数据，则发送
                    e.onNext(memoryCache);
                } else {
                    //若无该数据，则直接发送结束事件
                    e.onComplete();
                }
            }
        });

        //设置第2个Observable：检查磁盘缓存是否有该数据的缓存
        Observable<String> disk = Observable.create(new ObservableOnSubscribe<String>() {
            @Override
            public void subscribe(ObservableEmitter<String> e) throws Exception {
                //先判断磁盘缓存有无数据
                if (diskCache != null) {
                    //若有则发送
                    e.onNext(diskCache);
                } else {
                    e.onComplete();
                }
            }
        });

        //设置第3个Observable：通过网络获取数据
        Observable<String> network = Observable.just("从网络中获取数据");
        //此处仅作网络请求的模拟

        //通过concat() 和 firstElement() 操作符实现缓存功能

        //1，通过concat() 合并memory、disk、network 3个被观察者的事件（即检查内存缓存、磁盘缓存 & 发送网络请求）
        //并将它们按顺序串联成队列
        Observable.concat(memory, disk, network)
                //2, 通过 firstElement()，从串联队列中取出并发送第1个有效事件（Next事件），即依次判断检查memory、disk、network
                .firstElement()
                //即本例的逻辑为：
                /*
                * a. firstElement() 取出第1个事件  =  memory，即先判断内存缓存中有无数据缓存；
                * 由于memoryCache = null，即内存缓存中无数据，所以发送结束事件（视为无效事件）
                *
                * b. firstElement() 继续取出第2个事件 = disk，即判断磁盘缓存中有无数据缓存；由于disCache != null,
                * 即磁盘缓存中有数据，所以发送Next事件（有效事件）
                *
                * c. 即 firstElement() 已发出第1个有效事件（disk事件），所以停止判断。
                * */

                //3.观察者订阅
                .subscribe(new Consumer<String>() {
                    @Override
                    public void accept(String s) throws Exception {
                        Log.i(TAG, "accept:---00 最终获取的数据来源 = " + s);
                    }
                });
    }

    /**
     * 网络请求嵌套回调
     * 需要进行嵌套的网络请求：即在第1个网络请求成功后，继续再进行一次网络请求
     * 如：先进行用户注册 的网络请求，待注册成功后回再继续发送 用户登录 的网络请求
     */
    private void test3() {
        //步骤一：
        Retrofit retrofit = new Retrofit.Builder()
                .baseUrl("http://fy.iciba.com/")
                .addConverterFactory(GsonConverterFactory.create())
                .addCallAdapterFactory(RxJava2CallAdapterFactory.create())
                .build();

        //步骤二:
        final GetRequest_Interface request = retrofit.create(GetRequest_Interface.class);

        //步骤三：采用Observable<...>形式 对 2个网络请求时行封装
        observable1 = request.getCall_1();
        observable2 = request.getCall_2();

        observable1.subscribeOn(Schedulers.io())   //（初始被观察者）切换到IO线程进行网络请求1
                .observeOn(AndroidSchedulers.mainThread()) //（新观察者）切换到主线程 处理网络请求1的结果
                .doOnNext(new Consumer<Translation1>() {
                    @Override
                    public void accept(Translation1 translation1) throws Exception {
                        LogUtil.i(TAG, "accept: 第1次网络请求成功");
                        translation1.show();
                        //对第1次网络请求返回的结果进行操作 = 显示翻译结果
                    }
                })
                .observeOn(Schedulers.io())
               /*
                * (新被观察者， 同时也是新观察者) 切换到IO线程去发起登录请求
                * 特别注意：因为flatMap是对初始被观察者变换，所以对于旧被观察者，它是新观察者，所以通过
                *          observeOn切换纯种
                *
                * 但对于初始观察者，它则是新的被观察者
                * */
                .flatMap(new Function<Translation1, ObservableSource<Translation2>>() { // 作变换， 即作嵌套网络请求
                    @Override
                    public ObservableSource<Translation2> apply(Translation1 translation1) throws Exception {
                        //将网络请求1转换成网络请求2，即发送网络请求2
                        return observable2;
                    }
                })
                .observeOn(AndroidSchedulers.mainThread()) //(初始观察者) 切换到主线程 处理网络请求的结果
                .subscribe(new Consumer<Translation2>() {
                    @Override
                    public void accept(Translation2 translation2) throws Exception {
                        LogUtil.i(TAG, "accept: 第2次网络请求成功");
                        translation2.show();
                    }
                }, new Consumer<Throwable>() {
                    @Override
                    public void accept(Throwable throwable) throws Exception {
                        LogUtil.i(TAG, "登录失败！");
                    }
                });

    }

    /**
     * 网络请求出错重连
     */
    private void test2() {
        //步骤一：创建Retrofit对象
        Retrofit retrofit = new Retrofit.Builder()
                .baseUrl("http://fy.iciba.com/")
                .addConverterFactory(GsonConverterFactory.create())
                .addCallAdapterFactory(RxJava2CallAdapterFactory.create())
                .build();

        //步骤二：创建网络请求接口 的实例
        GetRequest_Interface request = retrofit.create(GetRequest_Interface.class);
        //步骤三：采用Observable<...> 形式 对网络 请求进行封装
        Observable<Translation> observable = request.getCall();
        //步骤四：发送网络请求 & 通过retryWhen() 进行重试
        //注： 主要异常才会回调retryWhen() 进行重试
        observable.retryWhen(new Function<Observable<Throwable>, ObservableSource<?>>() {
            @Override
            public ObservableSource<?> apply(Observable<Throwable> throwableObservable) throws Exception {
                //参数Observable<Throwable>中的泛型 = 上游操作符抛出的异常，可通过该条件来判断异常的类型
                return throwableObservable.flatMap(new Function<Throwable, ObservableSource<?>>() {
                    @Override
                    public ObservableSource<?> apply(Throwable throwable) throws Exception {
                        //输出异常信息
                        LogUtil.i(TAG, "apply: 发生异常 = " + throwable.toString());

                        /*
                        * 需求1：根据异常类型选择是否重试
                        * 即，当发生的异常 = 网络异常 = IO 异常 才选择重试
                        * */
                        if (throwable instanceof IOException) {
                            LogUtil.i(TAG, "apply: 属于IO异常，需重试");

                            /*
                            * 需求2：限制重试次数
                            * 即，当已重试次数  <  设置的重试次数，才选择重试
                            * */
                            if (currentRetryCount < maxConnectCount) {
                                //记录重试次数
                                currentRetryCount++;
                                LogUtil.i(TAG, "apply: 重试次数 = " + currentRetryCount);

                                /*
                                * 需求2：实现重试
                                * 通过返回的Observable发送的事件 =  Next事件，从而使得retryWhen（）重订阅，最终实现重试功能
                                *
                                * 需求3：延迟1段时间再重试
                                * 采用delay操作符 = 延迟一段时间发送，以实现重试间隔设置
                                *
                                * 需求4：遇到的异常越多，时间越长
                                * 在delay操作符的等待时间内设置 = 每重试1次，增多延迟重试时间1s
                                * */
                                //设置等待时间
                                waiRetryTime = 1000 + currentRetryCount * 1000;
                                LogUtil.i(TAG, "apply: 等待时间 = " + waiRetryTime);
                                return Observable.just(1).delay(waiRetryTime, TimeUnit.MILLISECONDS);
                            } else {
                                //若重试次数已 > 设置重试次数，则不重试
                                //通过发送 error来停止重试（可在观察者的onError()中获取信息）
                                return Observable.error(new Throwable("重试次数已起过设置次数 = " + currentRetryCount + "，即 不再重试"));
                            }
                        } else {
                            //若发生的异常不属于I/O异常，则不重试
                            //通过返回的Observable发送的事件  =  Error 事件实现（可在观察者的onError()中获取信息）
                            return Observable.error(new Throwable("发生了非网络异常（非I/O异常）"));
                        }
                    }
                });
            }
        }).subscribeOn(Schedulers.io()) //切换到IO线程进行网络请求
                .observeOn(AndroidSchedulers.mainThread()) // 切换回到主线程 处理请求结果
                .subscribe(new Observer<Translation>() {
                    @Override
                    public void onSubscribe(Disposable d) {

                    }

                    @Override
                    public void onNext(Translation translation) {
                        //接收服务器返回的数据
                        LogUtil.i(TAG, "onNext: 发送成功");
                    }

                    @Override
                    public void onError(Throwable e) {
                        //获取停止重试的信息
                        LogUtil.i(TAG, "onError: " + e.toString());
                    }

                    @Override
                    public void onComplete() {

                    }
                });
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
                        LogUtil.i(TAG, "accept: ---00 第 " + aLong + " 次轮询");
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
                                        LogUtil.i(TAG, "onError: ---00 请求失败");
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
                        LogUtil.i(TAG, "onError: ---00 对Error事件作出响应");
                    }

                    @Override
                    public void onComplete() {
                        LogUtil.i(TAG, "onComplete: ---00 对complete事件作出响应");
                    }
                });
    }


}
