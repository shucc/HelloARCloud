package cn.easyar.samples.helloarcloud;

import android.app.Application;

import com.squareup.leakcanary.LeakCanary;

/**
 * Created by shucc on 18/1/25.
 * cc@cchao.org
 */
public class App extends Application {

    private static App instance;

    @Override
    public void onCreate() {
        super.onCreate();
        instance = this;
        if (LeakCanary.isInAnalyzerProcess(this)) {
            return;
        }
        LeakCanary.install(this);
    }

    public static App getInstance() {
        return instance;
    }
}
