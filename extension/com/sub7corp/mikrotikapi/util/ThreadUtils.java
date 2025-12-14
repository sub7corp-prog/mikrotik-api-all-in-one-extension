package com.sub7corp.mikrotikapi.util;

import android.app.Activity;

public class ThreadUtils {

    public static void runAsync(Runnable task) {
        new Thread(task).start();
    }

    public static void runOnUi(Activity activity, Runnable task) {
        activity.runOnUiThread(task);
    }
}
