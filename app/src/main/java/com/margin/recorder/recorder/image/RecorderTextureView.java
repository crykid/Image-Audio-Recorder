package com.margin.recorder.recorder.image;

import android.content.Context;
import android.util.AttributeSet;
import android.view.TextureView;

public class RecorderTextureView extends TextureView {

    private int mRatioWidth, mRatioHeight;

    public RecorderTextureView(Context context) {
        this(context, null);
    }

    public RecorderTextureView(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public RecorderTextureView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }

    public void setRatiowSize(int ratioWidth, int ratioHeight) {
        this.mRatioWidth = ratioWidth;
        this.mRatioHeight = ratioHeight;

    }


    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec);
        int width = MeasureSpec.getSize(widthMeasureSpec);
        int height = MeasureSpec.getSize(heightMeasureSpec);
        if (0 == mRatioWidth || 0 == mRatioHeight) {
            setMeasuredDimension(width, height);
        } else {
            if (width < height * mRatioWidth / mRatioHeight) {
                setMeasuredDimension(width, width * mRatioHeight / mRatioWidth);
            } else {
                setMeasuredDimension(height * mRatioWidth / mRatioHeight, height);
            }
        }
    }

}
