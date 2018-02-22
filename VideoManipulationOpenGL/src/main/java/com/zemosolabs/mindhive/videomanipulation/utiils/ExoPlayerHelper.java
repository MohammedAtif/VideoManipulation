package com.zemosolabs.mindhive.videomanipulation.utiils;

import android.content.Context;
import android.net.Uri;
import android.os.Handler;
import android.os.HandlerThread;
import android.util.Log;

import com.google.android.exoplayer2.extractor.DefaultExtractorsFactory;
import com.google.android.exoplayer2.extractor.ExtractorsFactory;
import com.google.android.exoplayer2.source.ExtractorMediaSource;
import com.google.android.exoplayer2.source.LoopingMediaSource;
import com.google.android.exoplayer2.source.MediaSource;
import com.google.android.exoplayer2.trackselection.AdaptiveTrackSelection;
import com.google.android.exoplayer2.trackselection.DefaultTrackSelector;
import com.google.android.exoplayer2.trackselection.TrackSelection;
import com.google.android.exoplayer2.trackselection.TrackSelector;
import com.google.android.exoplayer2.upstream.BandwidthMeter;
import com.google.android.exoplayer2.upstream.DataSource;
import com.google.android.exoplayer2.upstream.DefaultBandwidthMeter;
import com.google.android.exoplayer2.upstream.DefaultDataSourceFactory;
import com.google.android.exoplayer2.upstream.TransferListener;
import com.google.android.exoplayer2.util.Util;
import com.zemosolabs.mindhive.videomanipulation.R;

/**
 * @author Atif
 * Created on 20/6/17.
 */

public class ExoPlayerHelper {

    private static final String TAG = ExoPlayerHelper.class.getSimpleName();

    private static BandwidthMeter defaultBandwidthMeter;
    private static TrackSelector defaultTrackSelector;
    private static HandlerThread videoPlayerThread;

    public static MediaSource getMediaSource(Context context, String videoPath, ExtractorMediaSource.EventListener eventListener, boolean isLooping, boolean isAsset){
        DataSource.Factory dataSourceFactory = new DefaultDataSourceFactory(
                context,
                Util.getUserAgent(context, context.getResources().getString(R.string.app_name)),
                (TransferListener<? super DataSource>) getDefaultBandwidthMeter()
        );
        // Produces Extractor instances for parsing the media data.
        ExtractorsFactory extractorsFactory = new DefaultExtractorsFactory();
        // This is the MediaSource representing the media to be played.
        String videoFileString;
        if(isAsset){
            videoFileString = "file:///android_asset/"+videoPath;
        }else{
            videoFileString = videoPath == null ? "" : videoPath;
        }
        Uri videoFile = Uri.parse(videoFileString);
        ExtractorMediaSource extractorMediaSource = new ExtractorMediaSource(
                videoFile,
                dataSourceFactory,
                extractorsFactory,
                getPlayerHandler(),
                eventListener
        );
        Log.d(TAG, "Media source generated with source "+videoFileString);
        if(isLooping) {
            return new LoopingMediaSource(extractorMediaSource);
        }else {
            return extractorMediaSource;
        }
    }

    public static TrackSelector getTrackSelector(){
        if(defaultTrackSelector == null){
            TrackSelection.Factory videoTrackSelectionFactory = new AdaptiveTrackSelection.Factory(new DefaultBandwidthMeter());
            defaultTrackSelector =  new DefaultTrackSelector(videoTrackSelectionFactory);
        }
        return defaultTrackSelector;
    }

    private static BandwidthMeter getDefaultBandwidthMeter(){
        if(defaultBandwidthMeter == null){
            defaultBandwidthMeter = new DefaultBandwidthMeter();
        }
        return defaultBandwidthMeter;
    }

    private static Handler getPlayerHandler(){
        if(videoPlayerThread == null){
            videoPlayerThread = new HandlerThread("VideoPlayer");
            videoPlayerThread.setPriority(Thread.MAX_PRIORITY);
            videoPlayerThread.start();
        }
        return new Handler(videoPlayerThread.getLooper());
    }

    public static void destroyPlayerThread(){
        if(videoPlayerThread != null){
            videoPlayerThread.quitSafely();
            videoPlayerThread = null;
        }
    }
}
