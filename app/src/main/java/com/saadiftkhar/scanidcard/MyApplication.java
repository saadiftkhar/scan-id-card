package com.saadiftkhar.scanidcard;

import android.app.Application;


/**
 * Author       wildma
 * Github       https://github.com/wildma
 * Date         2019/04/28
 * Desc	        ${MyApplication}
 */
public class MyApplication extends Application {

    @Override
    public void onCreate() {
        super.onCreate();
//        initLeakCanary();
    }

    private void initLeakCanary() {
//        if (LeakCanary.isInAnalyzerProcess(this)) {
//            return;
//        }
//        LeakCanary.install(this);
    }
}
