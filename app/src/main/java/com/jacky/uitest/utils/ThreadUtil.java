package com.jacky.uitest.utils;

import android.os.Handler;
import android.os.Looper;

public class ThreadUtil {
    private static ThreadUtil mThread;
    private Handler mHandler;

    private ThreadUtil() {
        mHandler = new Handler(Looper.getMainLooper());
    }

    public static ThreadUtil getInstance() {
        if (null == mThread)
            synchronized (ThreadUtil.class) {
                if (null == mThread) {
                    mThread = new ThreadUtil();
                }
            }
        return mThread;
    }

    public void execute(Runnable runnable) {
        mHandler.post(runnable);
    }
}
