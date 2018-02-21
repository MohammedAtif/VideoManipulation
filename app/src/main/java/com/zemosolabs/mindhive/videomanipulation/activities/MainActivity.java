package com.zemosolabs.mindhive.videomanipulation.activities;

import android.graphics.SurfaceTexture;
import android.support.design.widget.FloatingActionButton;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.Surface;
import android.view.TextureView;
import android.view.View;

import com.google.android.exoplayer2.ExoPlaybackException;
import com.google.android.exoplayer2.ExoPlayer;
import com.google.android.exoplayer2.ExoPlayerFactory;
import com.google.android.exoplayer2.PlaybackParameters;
import com.google.android.exoplayer2.Player;
import com.google.android.exoplayer2.SimpleExoPlayer;
import com.google.android.exoplayer2.Timeline;
import com.google.android.exoplayer2.source.ExtractorMediaSource;
import com.google.android.exoplayer2.source.TrackGroupArray;
import com.google.android.exoplayer2.trackselection.TrackSelectionArray;
import com.zemosolabs.mindhive.videomanipulation.R;
import com.zemosolabs.mindhive.videomanipulation.renderers.VideoTextureRenderer;
import com.zemosolabs.mindhive.videomanipulation.utiils.ExoPlayerHelper;

import java.io.IOException;

public class MainActivity extends AppCompatActivity implements View.OnClickListener{

    private static final String TAG = "MainActivity";

    private boolean isAnimated = false;

    private FloatingActionButton regularVideoController;
    private FloatingActionButton animatedVideoController;
    private TextureView mVideoTexture;
    private VideoTextureRenderer videoTextureRenderer;
    private SimpleExoPlayer simpleExoPlayer;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        regularVideoController = findViewById(R.id.regular);
        animatedVideoController = findViewById(R.id.animated);
        mVideoTexture = findViewById(R.id.video_texture_view);
        mVideoTexture.setSurfaceTextureListener(surfaceTextureListener);
        simpleExoPlayer = ExoPlayerFactory.newSimpleInstance(this, ExoPlayerHelper.getTrackSelector());
        simpleExoPlayer.prepare(ExoPlayerHelper.getMediaSource(this, "beach.mp4", extractorEventListener, false, true));
        simpleExoPlayer.addListener(playerEventListener);
        regularVideoController.setOnClickListener(this);
        animatedVideoController.setOnClickListener(this);
    }

    @Override
    protected void onPause() {
        if(simpleExoPlayer != null){
            simpleExoPlayer.release();
            simpleExoPlayer = null;
            regularVideoController.setSelected(false);
            animatedVideoController.setSelected(false);
        }
        super.onPause();
    }

    private void initRenderer(int width, int height) {
        Log.v(TAG, (mVideoTexture.getContext() == null)+ " context");
        videoTextureRenderer = new VideoTextureRenderer(
                mVideoTexture.getContext(),
                mVideoTexture.getSurfaceTexture(),
                mVideoTexture.getWidth(),
                mVideoTexture.getHeight(),
                rendererEvents
        );
        videoTextureRenderer.setVideoSize(width, height);
    }

    private ExtractorMediaSource.EventListener extractorEventListener = new ExtractorMediaSource.EventListener() {
        @Override
        public void onLoadError(IOException error) {
            Log.e(TAG, "Error loading the extractor media source", error);
        }
    };

    private Player.EventListener playerEventListener = new Player.EventListener() {
        @Override
        public void onTimelineChanged(Timeline timeline, Object manifest) {

        }

        @Override
        public void onTracksChanged(TrackGroupArray trackGroups, TrackSelectionArray trackSelections) {

        }

        @Override
        public void onLoadingChanged(boolean isLoading) {

        }

        @Override
        public void onPlayerStateChanged(boolean playWhenReady, int playbackState) {

        }

        @Override
        public void onRepeatModeChanged(int repeatMode) {

        }

        @Override
        public void onShuffleModeEnabledChanged(boolean shuffleModeEnabled) {

        }

        @Override
        public void onPlayerError(ExoPlaybackException error) {

        }

        @Override
        public void onPositionDiscontinuity(int reason) {

        }

        @Override
        public void onPlaybackParametersChanged(PlaybackParameters playbackParameters) {

        }

        @Override
        public void onSeekProcessed() {

        }
    };

    private TextureView.SurfaceTextureListener surfaceTextureListener = new TextureView.SurfaceTextureListener() {
        @Override
        public void onSurfaceTextureAvailable(SurfaceTexture surface, int width, int height) {
            if(isAnimated) {
                initRenderer(width, height);
            }else{
                simpleExoPlayer.setVideoSurface(new Surface(surface));
            }
        }

        @Override
        public void onSurfaceTextureSizeChanged(SurfaceTexture surface, int width, int height) {

        }

        @Override
        public boolean onSurfaceTextureDestroyed(SurfaceTexture surface) {
            return false;
        }

        @Override
        public void onSurfaceTextureUpdated(SurfaceTexture surface) {

        }
    };


    private VideoTextureRenderer.RendererEvents rendererEvents = new VideoTextureRenderer.RendererEvents() {
        @Override
        public void onInitialized() {
            if(simpleExoPlayer != null && videoTextureRenderer != null) {
                simpleExoPlayer.setVideoSurface(new Surface(videoTextureRenderer.getVideoTexture()));
            }
        }

        @Override
        public void onDrawFrame() {

        }
    };

    @Override
    public void onClick(View v) {
        if(v.equals(regularVideoController)){
            simpleExoPlayer.setPlayWhenReady(!regularVideoController.isSelected());
            regularVideoController.setSelected(!regularVideoController.isSelected());
            animatedVideoController.setVisibility(View.GONE);
        }else if(v.equals(animatedVideoController)){
            simpleExoPlayer.setPlayWhenReady(!animatedVideoController.isSelected());
            animatedVideoController.setSelected(!animatedVideoController.isSelected());
            regularVideoController.setVisibility(View.GONE);
        }
    }
}
