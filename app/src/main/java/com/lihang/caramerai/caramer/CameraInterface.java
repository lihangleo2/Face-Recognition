package com.lihang.caramerai.caramer;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.ImageFormat;
import android.graphics.Rect;
import android.hardware.Camera;
import android.hardware.Camera.PictureCallback;
import android.hardware.Camera.ShutterCallback;
import android.hardware.Camera.Size;
import android.media.ToneGenerator;
import android.os.Build;
import android.os.Environment;
import android.util.Log;
import android.view.SurfaceHolder;
import android.widget.Toast;


import com.lihang.caramerai.App;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;


/**
 * Created by lchad on 2017/7/20.
 */
public class CameraInterface {
    private static final String TAG = "jupiter";

    private static CameraInterface mCameraInterface;

    private Camera mCamera;
    private Camera.Parameters mParams;
    private boolean isPreviewing = false;
    private float mPreviwRate = -1f;
    private int mCameraId = -1;
    private List<Rect> mFaceRect = new ArrayList<>();


    /**
     * 快门音效
     */
    ShutterCallback mShutterCallback = new ShutterCallback() {
        public void onShutter() {
            Toast.makeText(App.getInstance(), "shutter", Toast.LENGTH_SHORT).show();
        }
    };

    PictureCallback mJpegPictureCallback = new PictureCallback() {
        public void onPictureTaken(byte[] data, Camera camera) {
            Bitmap b = null;
            if (null != data) {
                b = BitmapFactory.decodeByteArray(data, 0, data.length);
                mCamera.stopPreview();
                isPreviewing = false;
            }

            if (null != b) {
                Bitmap rotatedBitmap = null;
                if (mCameraId == Camera.CameraInfo.CAMERA_FACING_BACK) {
                    rotatedBitmap = ImageUtil.getRotateBitmap(b, 90.0f);
                } else if (mCameraId == Camera.CameraInfo.CAMERA_FACING_FRONT) {
                    rotatedBitmap = ImageUtil.getRotateBitmap(b, -90.0f);
                }

                FileUtil.saveBitmap(rotatedBitmap);
                if (mFaceRect.size() > 0) {
                    for (Rect rect : mFaceRect) {
                        Log.e("123456789", " face clips: " + rect.flattenToString());
//                        Bitmap faceClipsBitmap = Bitmap.createBitmap(rotatedBitmap, rect.left, rect.top, rect.width(), rect.height());
//                        FileUtil.saveBitmap(faceClipsBitmap);
                    }
                }
            }
            mCamera.startPreview();
            isPreviewing = true;
            mFaceRect.clear();
        }
    };


    private CameraInterface() {

    }

    public static synchronized CameraInterface getInstance() {
        if (mCameraInterface == null) {
            mCameraInterface = new CameraInterface();
        }
        return mCameraInterface;
    }

    public void doOpenCamera(CamOpenOverCallback callback, int cameraId) {
        Log.i(TAG, "Camera open....");

        try {
            mCamera = Camera.open(cameraId);
            mCameraId = cameraId;
            if (callback != null) {
                callback.cameraHasOpened();
            }
        } catch (Exception e) {
//            LogUtils.i("我打印了", "不会崩溃");
//            ToastUtils.showToast("请打开摄像头权限，保证正常使用");
//            EventBus.getDefault().post("请打开摄像头权限，保证正常使用");
        }

    }

    /**
     * 预览的尺寸
     */
    private Camera.Size preSize;
    /**
     * 实际的尺寸
     */
    private Camera.Size picSize;

    public void doStartPreview(SurfaceHolder holder, float previewRate, int falshModel) {
        try {
            if (isPreviewing) {
                mCamera.stopPreview();
                if (Build.VERSION.SDK_INT >= 23) {
                    mCamera.stopFaceDetection();
                }
                return;
            }
            if (mCamera != null) {
                mParams = mCamera.getParameters();
                mParams.setPictureFormat(ImageFormat.JPEG);

                CamParaUtil.getInstance().printSupportPictureSize(mParams);
                CamParaUtil.getInstance().printSupportPreviewSize(mParams);

                Size pictureSize = CamParaUtil.getInstance().getPropPictureSize(
                        mParams.getSupportedPictureSizes(), previewRate, 800);
                mParams.setPictureSize(pictureSize.width, pictureSize.height);
                Size previewSize = CamParaUtil.getInstance().getPropPreviewSize(
                        mParams.getSupportedPreviewSizes(), previewRate, 800);
                mParams.setPreviewSize(previewSize.width, previewSize.height);//这里是设置摄像头变形相关

//                mParams.setPreviewSize(1920, 1080 - 48);
                mCamera.setDisplayOrientation(90);

                CamParaUtil.getInstance().printSupportFocusMode(mParams);
                List<String> focusModes = mParams.getSupportedFocusModes();
                if (focusModes.contains("continuous-video")) {
                    mParams.setFocusMode(Camera.Parameters.FOCUS_MODE_CONTINUOUS_VIDEO);
                }


                switch (falshModel) {
                    case 1://自动
                        mParams.setFlashMode(Camera.Parameters.FLASH_MODE_AUTO);
                    case 2://开启
                        mParams.setFlashMode(Camera.Parameters.FLASH_MODE_ON);
                        break;
                    case 3:
                        mParams.setFlashMode(Camera.Parameters.FLASH_MODE_OFF);
                        break;
                }


                mCamera.setParameters(mParams);

                try {
                    mCamera.setPreviewDisplay(holder);
                    mCamera.startPreview();
                } catch (IOException e) {
                    e.printStackTrace();
                }

                isPreviewing = true;
                mPreviwRate = previewRate;

                mParams = mCamera.getParameters();
                Log.i(TAG, "PreviewSize--With = " + mParams.getPreviewSize().width
                        + "Height = " + mParams.getPreviewSize().height);
                Log.i(TAG, "PictureSize--With = " + mParams.getPictureSize().width
                        + "Height = " + mParams.getPictureSize().height);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void doStopCamera() {
        if (null != mCamera) {
            try {
                mCamera.setPreviewCallback(null);
                mCamera.stopPreview();
                if (Build.VERSION.SDK_INT >= 23) {
                    mCamera.stopFaceDetection();
                }
                isPreviewing = false;
                mPreviwRate = -1f;
                mCamera.release();
                mCamera = null;
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    public void doTakePicture(List<Rect> rects) {
        if (isPreviewing && (mCamera != null)) {

            mFaceRect.addAll(rects);
            if (Build.VERSION.SDK_INT >= 23) {
                mCamera.stopFaceDetection();
            }
            mCamera.takePicture(mShutterCallback, null, mJpegPictureCallback);

        }
    }


    public Camera.Parameters getCameraParams() {
        if (mCamera != null) {
            mParams = mCamera.getParameters();
            return mParams;
        }
        return null;
    }

    public Camera getCameraDevice() {
        return mCamera;
    }

    public int getCameraId() {
        return mCameraId;
    }

    public interface CamOpenOverCallback {
        void cameraHasOpened();
    }


    /**
     * 设置闪光灯模式
     *
     * @return
     */
    public void setFlashlight(int witchC) {
        switch (witchC) {
            case 1://自动
                if (mParams != null) {
                    mParams.setFlashMode(Camera.Parameters.FLASH_MODE_AUTO);
                    mCamera.setParameters(mParams);
                    mCamera.startPreview();
                }
            case 2://开启
                if (mParams != null) {

                    mParams.setFlashMode(Camera.Parameters.FLASH_MODE_ON);
                    mCamera.setParameters(mParams);
                    mCamera.startPreview();
                }
                break;
            case 3:
                if (mParams != null) {
                    mParams.setFlashMode(Camera.Parameters.FLASH_MODE_OFF);
                    mCamera.setParameters(mParams);
                    mCamera.startPreview();
                }
                break;
        }
    }


    /**
     * develop by leo
     */
    private ToneGenerator tone;


    /**
     * 照相
     */
    public void tackPicture(final OnCaptureData callback) {
//        LogUtils.i("重新拍照这里有问题啊老哥", "111111111");
        if (mCamera == null) {
            return;
        }
        mCamera.takePicture(null, null, new PictureCallback() {
            @Override
            public void onPictureTaken(byte[] data, Camera camera) {
//                String filepath = savePicture(data);//拍照不该这这里保存 应该点击下一步的时候做操作
                boolean success = false;
                if (data != null && data.length > 0) {
                    success = true;
                }
//                LogUtils.i("重新拍照这里有问题啊老哥", "3333333");
                doStopCamera();
                callback.onCapture(success, data);
            }
        });
    }


    /**
     * 保存图片
     *
     * @param data
     * @return
     */
    private String savePicture(byte[] data) {
        File imgFileDir = getImageDir();
        if (!imgFileDir.exists() && !imgFileDir.mkdirs()) {
            return null;
        }
//		文件路径路径
        String imgFilePath = imgFileDir.getPath() + File.separator + this.generateFileName();
        Bitmap b = BitmapFactory.decodeByteArray(data, 0, data.length);

        Bitmap rotatedBitmap = null;
        if (null != b) {
            if (mCameraId == Camera.CameraInfo.CAMERA_FACING_BACK) {
                rotatedBitmap = ImageUtil.getRotateBitmap(b, 90.0f);
            } else if (mCameraId == Camera.CameraInfo.CAMERA_FACING_FRONT) {
                rotatedBitmap = ImageUtil.getRotateBitmap(b, -90.0f);
            }
        }
        File imgFile = new File(imgFilePath);
        FileOutputStream fos = null;
        BufferedOutputStream bos = null;
        try {
            fos = new FileOutputStream(imgFile);
            bos = new BufferedOutputStream(fos);
            rotatedBitmap.compress(Bitmap.CompressFormat.JPEG, 100, fos);
        } catch (Exception error) {
            return null;
        } finally {
            try {
                if (fos != null) {
                    fos.flush();
                    fos.close();
                }
                if (bos != null) {
                    bos.flush();
                    bos.close();
                }
            } catch (IOException e) {
            }
        }
        return imgFilePath;
    }

    /**
     * 生成图片名称
     *
     * @return
     */

    private String generateFileName() {
        SimpleDateFormat dateFormat = new SimpleDateFormat("yyyyMMddhhmmss", Locale.getDefault());
        String strDate = dateFormat.format(new Date());
        return "img_" + strDate + ".jpg";
    }

    /**
     * @return
     */
    private File getImageDir() {
        String path =
                Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DCIM).getPath();
        File file = new File(path);
        if (!file.exists()) {
            file.mkdir();
        }
        return file;
    }

}
