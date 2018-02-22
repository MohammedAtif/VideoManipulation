/*
 * Copyright 2014 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.zemosolabs.mindhive.videomanipulation.custom_views;

import android.content.Context;
import android.content.res.TypedArray;
import android.util.AttributeSet;
import android.view.TextureView;
import android.view.ViewGroup;

import com.zemosolabs.mindhive.videomanipulation.R;

/**
 * A {@link TextureView} that can be adjusted to a specified aspect ratio.
 */
public class BurstPlaybackTextureView extends TextureView {

    private boolean isNarration;

    public BurstPlaybackTextureView(Context context) {
        this(context, false);
    }

    public BurstPlaybackTextureView(Context context, boolean isNarration) {
        super(context);
        this.isNarration = isNarration;
    }

    public BurstPlaybackTextureView(Context context, AttributeSet attrs) {
        super(context, attrs);
        initializeView(context, attrs);
    }

    public BurstPlaybackTextureView(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        initializeView(context, attrs);
    }

    private void initializeView(Context context, AttributeSet attrs){
        TypedArray array = context.getTheme().obtainStyledAttributes(
                attrs,
                R.styleable.BurstPlaybackTextureView,
                0, 0);
        try {
            isNarration = array.getBoolean(R.styleable.BurstPlaybackTextureView_isAuthorMode, false);
        } finally {
            array.recycle();
        }
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec);
        int width = MeasureSpec.getSize(widthMeasureSpec);
        int height = MeasureSpec.getSize(heightMeasureSpec);
        if(isNarration){
            height = width*16/9;
            //Adjusting the view to display the center part of the view to match with iOS
            ((ViewGroup.MarginLayoutParams)getLayoutParams()).topMargin = (width-height)/2;
        }else{
            width = (height * 9)/16;
        }
        setMeasuredDimension(width, height);
    }

}
