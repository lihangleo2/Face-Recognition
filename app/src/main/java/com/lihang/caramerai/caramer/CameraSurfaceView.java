package com.lihang.caramerai.caramer;

import android.content.Context;
import android.content.pm.PackageManager;
import android.content.res.Configuration;
import android.graphics.PixelFormat;
import android.hardware.Camera;
import android.util.AttributeSet;
import android.view.SurfaceHolder;
import android.view.SurfaceView;

/**
 * Created by leo
 * on 2020/2/28.
 */
public class CameraSurfaceView extends SurfaceView implements SurfaceHolder.Callback {
    private static final String TAG = "jupiter";
    SurfaceHolder mSurfaceHolder;

    public CameraSurfaceView(Context context, AttributeSet attrs) {
        super(context, attrs);
        mSurfaceHolder = getHolder();
        mSurfaceHolder.setFormat(PixelFormat.TRANSPARENT);
        mSurfaceHolder.addCallback(this);
        mSurfaceHolder.setType(SurfaceHolder.SURFACE_TYPE_PUSH_BUFFERS);
    }

    private int witch;
    public void setWitch(int witch) {
        this.witch = witch;
    }


    @Override
    public void surfaceCreated(SurfaceHolder holder) {
        if (witch == 1) {
            CameraInterface.getInstance().doOpenCamera(null, Camera.CameraInfo.CAMERA_FACING_BACK);
        } else {
            CameraInterface.getInstance().doOpenCamera(null, Camera.CameraInfo.CAMERA_FACING_FRONT);
        }

    }

    @Override
    public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {
        //这个比例，在好好研究完相机后，自然解开
        float previewRate = 1.78f;

        if (!getContext().getPackageManager().hasSystemFeature(PackageManager.FEATURE_CAMERA_FLASH)) {
//            当前设备没有闪光灯
            CameraInterface.getInstance().doStartPreview(mSurfaceHolder, previewRate, -1);//这里是相机初始化的  长宽比  影响分辨率变形
        } else {
            CameraInterface.getInstance().doStartPreview(mSurfaceHolder, previewRate, 3);//这里是相机初始化的  长宽比  影响分辨率变形
        }

    }

    @Override
    public void surfaceDestroyed(SurfaceHolder holder) {
        CameraInterface.getInstance().doStopCamera();
//        LogUtils.i("这个方法什么时候会走啊","1212111111111111111");
    }


    public SurfaceHolder getSurfaceHolder() {
        return mSurfaceHolder;
    }

    /**
     * 判断是否平板设备
     *
     * @param context
     * @return true:平板,false:手机
     */
    private boolean isTabletDevice(Context context) {
        return (context.getResources().getConfiguration().screenLayout & Configuration.SCREENLAYOUT_SIZE_MASK) >=
                Configuration.SCREENLAYOUT_SIZE_LARGE;
    }
}
