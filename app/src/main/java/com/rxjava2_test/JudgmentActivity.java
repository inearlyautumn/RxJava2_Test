package com.rxjava2_test;

import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.widget.Button;
import android.widget.EditText;

import com.jakewharton.rxbinding2.widget.RxTextView;

import org.w3c.dom.Text;

import io.reactivex.Observable;
import io.reactivex.functions.Consumer;
import io.reactivex.functions.Function3;

public class JudgmentActivity extends AppCompatActivity {
    public static final String TAG = JudgmentActivity.class.getSimpleName();

    EditText name, age, job;

    Button list;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_judgment);

        //步骤一:
        name = (EditText) findViewById(R.id.name);
        age = (EditText) findViewById(R.id.age);
        job = (EditText) findViewById(R.id.job);
        list = (Button) findViewById(R.id.list);

        /*
        * 步骤二: 为每个EditText设置观察者，用于发送监听事件
        * 说明：
        * 1，此处采用了RxBinding: RxTextView.textChanges(name) = 对控件数据变更进行监听（功能类似TextWatcher）,
        * 需要引入依赖：compile 'com.jakewharton.rxbinding2:rxbinding:2.0.0'
        *
        * 2，传入EditText控件，点击任1个EditText撰写时，都会发送数据事件 = Function3() 的返回值（下面会详细说明）
        *
        * 3，采用skp(1)原因：跳过一开始 EditText无任何输入时的空值
        * */
        final Observable<CharSequence> nameObservable = RxTextView.textChanges(name).skip(1);
        Observable<CharSequence> ageObservable = RxTextView.textChanges(age).skip(1);
        Observable<CharSequence> jobObservable = RxTextView.textChanges(job).skip(1);

        //步骤三: 通过combineLatest() 合并事件 & 联合判断
        Observable.combineLatest(nameObservable, ageObservable, jobObservable, new Function3<CharSequence, CharSequence, CharSequence, Boolean>() {
            @Override
            public Boolean apply(CharSequence charSequence, CharSequence charSequence2, CharSequence charSequence3) throws Exception {
                //步骤四：规定表单信息输入不能为空
                //1，姓名信息
                boolean isUserNameValid = !TextUtils.isEmpty(name.getText());
                //除了设置为空，也可设置长度限制

                //2，年龄信息
                boolean isUserAgeValid = !TextUtils.isEmpty(age.getText());

                //3，职业信息
                boolean isUserJobValid = !TextUtils.isEmpty(job.getText());

                //步骤五：返回信息  =  联合判断，即3个信息同时已填写，“提交按钮”才可点击
                return isUserNameValid && isUserAgeValid && isUserJobValid;
            }
        }).subscribe(new Consumer<Boolean>() {
            @Override
            public void accept(Boolean aBoolean) throws Exception {
                //步骤六：返回结果  &  调协按钮可点击样式
                Log.i(TAG, "accept: ---00 提交按钮是否可点击" + aBoolean);
                list.setEnabled(aBoolean);
            }
        });
    }
}
