package com.lihang.caramerai;

import android.app.Application;

/**
 * Created by leo
 * on 2020/2/28.
 */
public class App extends Application {
    private static App context;

    @Override
    public void onCreate() {
        super.onCreate();
        context = this;
    }

    public static App getInstance() {
        return context;
    }
}
