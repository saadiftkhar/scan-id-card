package com.saadiftkhar.scanidcard;

import android.app.Application;


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
