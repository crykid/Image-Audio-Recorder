package com.margin.recorder.recorder.image;

import android.graphics.SurfaceTexture;
import android.view.TextureView;

/**
 * Created by : mr.lu
 * Created at : 2020-04-22 at 14:42
 * Description:
 */
public abstract class RecordSurfaceTextureIml implements TextureView.SurfaceTextureListener {


    @Override
    final public void onSurfaceTextureSizeChanged(SurfaceTexture surface, int width, int height) {

    }

    @Override
    final public boolean onSurfaceTextureDestroyed(SurfaceTexture surface) {
        return false;
    }

    @Override
    final public void onSurfaceTextureUpdated(SurfaceTexture surface) {

    }
}
