package com.lihang.caramerai.faceai;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.Rect;
import android.graphics.RectF;
import android.hardware.Camera;
import android.util.AttributeSet;
import android.view.MotionEvent;

import com.lihang.caramerai.caramer.CameraInterface;

import java.util.ArrayList;
import java.util.List;

import androidx.appcompat.widget.AppCompatImageView;

/**
 * Created by leo
 * on 2020/2/28.
 */
public class FaceView extends AppCompatImageView {
    private Paint mLinePaint;
    private Camera.Face[] mFaces;
    private Matrix mMatrix = new Matrix();
    private RectF mRect = new RectF();

    public FaceView(Context context, AttributeSet attrs) {
        super(context, attrs);
        initPaint();
    }


    public void setFaces(Camera.Face[] faces) {
        this.mFaces = faces;
        invalidate();
    }

    public void clearFaces() {
        mFaces = null;
        invalidate();
    }


    @Override
    protected void onDraw(Canvas canvas) {
        if (mFaces == null || mFaces.length < 1) {
            return;
        }
        boolean isMirror = false;
        int cameraId = CameraInterface.getInstance().getCameraId();
        if (cameraId == Camera.CameraInfo.CAMERA_FACING_BACK) {
            isMirror = false;
        } else if (cameraId == Camera.CameraInfo.CAMERA_FACING_FRONT) {
            isMirror = true;
        }
        Util.prepareMatrix(mMatrix, isMirror, 90, getWidth(), getHeight());
        canvas.save();
        mMatrix.postRotate(0);
        canvas.rotate(-0);


        for (int i = 0; i < mFaces.length; i++) {
            Camera.Face mFace = mFaces[i];
            mRect.set(mFace.rect);
            mMatrix.mapRect(mRect);
//            canvas.drawRoundRect(mRect, 15, 15, mLinePaint);
            canvas.drawOval(mRect.left, mRect.top - 50, mRect.right, mRect.bottom + 50, mLinePaint);
        }
        canvas.restore();
        super.onDraw(canvas);
    }

    private void initPaint() {
        mLinePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        int color = Color.rgb(255, 255, 255);
        mLinePaint.setColor(color);
        mLinePaint.setStyle(Paint.Style.STROKE);
        mLinePaint.setStrokeWidth(4f);
        mLinePaint.setAlpha(180);
    }
}
