package com.zemosolabs.mindhive.videomanipulation.utiils;

import android.app.Application;

/**
 * @author atif
 *         Created on 20/02/18.
 */

public class VideoApplication extends Application {

    static {
        System.loadLibrary("sample.lib");
        System.loadLibrary("video-processing.lib");
    }

    @Override
    public void onCreate() {
        super.onCreate();
    }

}
