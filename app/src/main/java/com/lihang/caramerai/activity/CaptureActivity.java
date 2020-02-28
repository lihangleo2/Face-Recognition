package com.lihang.caramerai.activity;

import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.hardware.Camera;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import android.view.View;
import android.widget.Toast;

import com.lihang.caramerai.R;
import com.lihang.caramerai.TimeUtils;
import com.lihang.caramerai.caramer.CameraInterface;
import com.lihang.caramerai.caramer.ImageUtil;
import com.lihang.caramerai.caramer.OnCaptureData;
import com.lihang.caramerai.databinding.ActivityCaptureBinding;
import com.lihang.caramerai.faceai.EventUtil;
import com.lihang.caramerai.faceai.FaceView;
import com.lihang.caramerai.faceai.GoogleFaceDetect;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.lang.ref.WeakReference;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.databinding.DataBindingUtil;

/**
 * Created by leo
 * on 2020/2/28.
 */
public class CaptureActivity extends AppCompatActivity implements View.OnClickListener, OnCaptureData {
    ActivityCaptureBinding binding;
    private int currentFlashMode = 3;//闪光灯模式
    private boolean haveFlash = true;//此设备是否具有闪光灯
    private String filepath;//保存的文件路径
    private int flag = 0;

    /**
     * 相机api识别人脸
     */
    GoogleFaceDetect googleFaceDetect = null;
    private MainHandler mMainHandler = null;


    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = DataBindingUtil.setContentView(this, R.layout.activity_capture);
        binding.setOnClickListener(this);
        isHaveFlash();
        if (Build.VERSION.SDK_INT >= 23) {
            googleFaceDetect = new GoogleFaceDetect(CaptureActivity.this);
            mMainHandler = new MainHandler(binding.faceView, googleFaceDetect);
        }
    }

    @Override
    protected void onStart() {
        super.onStart();
        if (Build.VERSION.SDK_INT >= 23) {
            mMainHandler.sendEmptyMessageDelayed(EventUtil.CAMERA_HAS_STARTED_PREVIEW, 1500);
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        if (Build.VERSION.SDK_INT >= 23) {
            stopGoogleFaceDetect();
        }
    }

    private void isHaveFlash() {
        if (!getPackageManager().hasSystemFeature(PackageManager.FEATURE_CAMERA_FLASH)) {
            binding.imageFlash.setVisibility(View.GONE);
            haveFlash = false;
            currentFlashMode = -1;
        }
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.image_switch:
                //切换前后置摄像头
                switchCamera();
                break;

            case R.id.image_take:
                String tag = (String) binding.imageTake.getTag();
                if (tag.equals("0")) {
                    binding.imageTake.setTag("1");
                    //这里点击拍照了
                    CameraInterface.getInstance().tackPicture(CaptureActivity.this);
                    binding.imageTake.setImageResource(R.mipmap.take_photo_re);
                    binding.imageSwitch.setVisibility(View.GONE);
                    binding.imageFlash.setVisibility(View.GONE);
                } else {
                    binding.imageTake.setTag("0");
                    binding.imageTake.setImageResource(R.mipmap.tack_photo);
                    binding.imageSwitch.setVisibility(View.VISIBLE);
                    //如果是点击重新拍摄的话
                    //0 后置; 1 前置
                    int newId = CameraInterface.getInstance().getCameraId();
                    if (newId == 0) {
                        CameraInterface.getInstance().doStopCamera();
                        CameraInterface.getInstance().doOpenCamera(null, newId);
                        CameraInterface.getInstance().doStartPreview(binding.CameraSurfaceView.getSurfaceHolder(), 1.78f, currentFlashMode);
                        if (haveFlash) {
                            binding.imageFlash.setVisibility(View.VISIBLE);
                        }
                    } else {
                        CameraInterface.getInstance().doStopCamera();
                        CameraInterface.getInstance().doOpenCamera(null, newId);
                        CameraInterface.getInstance().doStartPreview(binding.CameraSurfaceView.getSurfaceHolder(), 1.78f, 0);
                    }
                    deleteFile();

                }
                break;


            case R.id.image_flash://1是自动  2 是开启  3是关闭
                flag++;
                switch (flag % 3) {
                    case 0:
                        binding.imageFlash.setImageResource(R.mipmap.flash_off);
                        CameraInterface.getInstance().setFlashlight(3);
                        currentFlashMode = 3;
                        break;

                    case 1:
                        binding.imageFlash.setImageResource(R.mipmap.flash_on);
                        CameraInterface.getInstance().setFlashlight(2);
                        currentFlashMode = 2;
                        break;

                    case 2:

                        binding.imageFlash.setImageResource(R.mipmap.flash_a);
                        CameraInterface.getInstance().setFlashlight(1);
                        currentFlashMode = 1;
                        break;
                }

                break;
        }
    }


    private void switchCamera() {//这是切换前后置摄像头用的方法
        if (Build.VERSION.SDK_INT >= 23) {
            stopGoogleFaceDetect();
        }
        //0 后置; 1 前置
        int newId = (CameraInterface.getInstance().getCameraId() + 1) % 2;
        if (newId == 0) {
            if (haveFlash) {
                binding.imageFlash.setVisibility(View.VISIBLE);
            }
            CameraInterface.getInstance().doStopCamera();
            CameraInterface.getInstance().doOpenCamera(null, newId);
            CameraInterface.getInstance().doStartPreview(binding.CameraSurfaceView.getSurfaceHolder(), 1.78f, currentFlashMode);

            if (Build.VERSION.SDK_INT >= 23) {
                mMainHandler.sendEmptyMessageDelayed(EventUtil.CAMERA_HAS_STARTED_PREVIEW, 1500);
            }
        } else {
            binding.imageFlash.setVisibility(View.GONE);
            CameraInterface.getInstance().doStopCamera();
            CameraInterface.getInstance().doOpenCamera(null, newId);
            CameraInterface.getInstance().doStartPreview(binding.CameraSurfaceView.getSurfaceHolder(), 1.78f, 0);
            if (Build.VERSION.SDK_INT >= 23) {
                mMainHandler.sendEmptyMessageDelayed(EventUtil.CAMERA_HAS_STARTED_PREVIEW, 1500);
            }
        }

    }

    @Override
    public void onCapture(boolean success, byte[] data) {
        if (data == null || data.length == 0) {
//            showToast("程序无法识别，请重新拍摄~");
            return;
        }
        filepath = savePicture(data);
        if (this.filepath == null || this.filepath.equals("")) {
//            showToast("请选择一张图片或者拍照");
            return;
        }
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
//        String imgFilePath = imgFileDir.getPath() + File.separator + this.generateFileName();
        String imgFilePath = getFilesDir().getAbsolutePath().toString() + "/" + TimeUtils.getDateToStringLeo(System.currentTimeMillis() + "") + "_atmancarm.jpg";
        Bitmap b = BitmapFactory.decodeByteArray(data, 0, data.length);


        Bitmap rotatedBitmap = null;
        if (null != b) {
            if (CameraInterface.getInstance().getCameraId() == Camera.CameraInfo.CAMERA_FACING_BACK) {
                rotatedBitmap = ImageUtil.getRotateBitmap(b, 90.0f);
            } else if (CameraInterface.getInstance().getCameraId() == Camera.CameraInfo.CAMERA_FACING_FRONT) {
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

    /**
     * 删除图片文件呢
     */
    private void deleteFile() {
        if (this.filepath == null || this.filepath.equals("")) {
            return;
        }
        File f = new File(this.filepath);
        if (f.exists()) {
            f.delete();
        }
        filepath = "";
    }


    private class MainHandler extends Handler {
        private final WeakReference<FaceView> mFaceViewWeakReference;
        private final WeakReference<GoogleFaceDetect> mGoogleFaceDetectWeakReference;

        public MainHandler(FaceView faceView, GoogleFaceDetect googleFaceDetect) {
            mFaceViewWeakReference = new WeakReference<>(faceView);
            mGoogleFaceDetectWeakReference = new WeakReference<>(googleFaceDetect);
            mGoogleFaceDetectWeakReference.get().setHandler(MainHandler.this);
        }


        @Override
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case EventUtil.UPDATE_FACE_RECT:
                    Camera.Face[] faces = (Camera.Face[]) msg.obj;
                    mFaceViewWeakReference.get().setFaces(faces);
                    if (faces.length == 0) {
                        //当前没有脸
                        Log.e("当前是什么呢", "当前没有脸呢没有脸");
                        Toast.makeText(CaptureActivity.this, "请您对准你的脸", Toast.LENGTH_SHORT).show();
                    } else {
                        //当前有脸了
                        Log.e("当前是什么呢", "当前有脸了=======================");
                        Toast.makeText(CaptureActivity.this, "当前有脸，可拍摄", Toast.LENGTH_SHORT).show();

                    }


                    break;
                case EventUtil.CAMERA_HAS_STARTED_PREVIEW:
                    Camera.Parameters params = CameraInterface.getInstance().getCameraParams();
                    if (params != null && params.getMaxNumDetectedFaces() > 0) {
                        if (mFaceViewWeakReference.get() != null) {
                            mFaceViewWeakReference.get().clearFaces();
                        }
                        CameraInterface.getInstance().getCameraDevice().setFaceDetectionListener(mGoogleFaceDetectWeakReference.get());
                        CameraInterface.getInstance().getCameraDevice().stopFaceDetection();
                        CameraInterface.getInstance().getCameraDevice().startFaceDetection();
                    }
                    break;
            }
            super.handleMessage(msg);
        }
    }


    private void stopGoogleFaceDetect() {

        if (binding.faceView != null) {
            Camera.Parameters params = CameraInterface.getInstance().getCameraParams();
            if (params != null) {

                if (params.getMaxNumDetectedFaces() > 0) {
                    CameraInterface.getInstance().getCameraDevice().setFaceDetectionListener(null);
                    CameraInterface.getInstance().getCameraDevice().stopFaceDetection();
                    binding.faceView.clearFaces();
                }
            }
        }

    }
}
