package cn.easyar.samples.helloarcloud;

import android.app.Application;

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
    }

    public static App getInstance() {
        return instance;
    }
}
