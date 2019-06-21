package com.daddysoffice.sample.broadcasttest;

import android.content.Context;
import android.graphics.Color;
import android.graphics.ImageFormat;
import android.graphics.Rect;
import android.graphics.YuvImage;
import android.hardware.Camera;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;
import android.view.ViewGroup;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.List;

public class CameraPreview  extends ViewGroup implements SurfaceHolder.Callback {

    private Camera mCamera;

    private int mSelectedCameraId = 0;

    private SurfaceView mSurfaceView = null;

    private SurfaceHolder mHolder = null;

    private boolean mStartPreviewAfterSurfaceCreated = false;

    private Camera.Size mPreviewSize;

    private int mPreviewFormat = 0;

    private List<Camera.Size> mSupportedPreviewSizes;

    private byte [] mFrameBuffer;

    private CaptureEventListener mEventListener;

    private Context mContext;

    public interface CaptureEventListener{
        void OnFrame(byte [] frame);
    }

    CameraPreview(Context context) {

        super(context);

        mContext = context;

        mSurfaceView = new SurfaceView(context);

        mSurfaceView.getHolder().addCallback(this);
        mSurfaceView.getHolder().setType(SurfaceHolder.SURFACE_TYPE_PUSH_BUFFERS);

        addView(mSurfaceView);

        setBackgroundColor(Color.BLACK);
    }

    @Override
    public void surfaceCreated(SurfaceHolder holder) {
        mHolder = holder;

        if (mStartPreviewAfterSurfaceCreated){
            startPreview();
            mStartPreviewAfterSurfaceCreated = false;
        }
   }

    @Override
    public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {
        requestLayout();
    }

    @Override
    public void surfaceDestroyed(SurfaceHolder holder) {
        stopPreview();
        stopCapture();
        mHolder = null;
    }

    @Override
    protected void onLayout(boolean changed, int l, int t, int r, int b) {
        if (getChildCount() > 0) {
            final View child = getChildAt(0);
            final int width = r - l;
            final int height = b - t;
            int previewWidth = width;
            int previewHeight = height;
            if (mPreviewSize != null) {
                previewWidth = mPreviewSize.width;
                previewHeight = mPreviewSize.height;
            }

            // Center the child SurfaceView within the parent.
            if (width * previewHeight > height * previewWidth) {
                final int scaledChildWidth = previewWidth * height / previewHeight;
                child.layout(   (width - scaledChildWidth) / 2, 0,
                        (width + scaledChildWidth) / 2, height);
            }
            else {
                final int scaledChildHeight = previewHeight * width / previewWidth;
                child.layout(0, (height - scaledChildHeight) / 2,
                        width, (height + scaledChildHeight) / 2);
            }
        }
    }

    public void setCaptureEventListener( CaptureEventListener listener){
        mEventListener = listener;
    }

    public void openCamera() {

        closeCamera();

        int typeId = Camera.CameraInfo.CAMERA_FACING_BACK;

        int numberOfCameras = Camera.getNumberOfCameras();
        Camera.CameraInfo cameraInfo = new Camera.CameraInfo();
        for (int i = 0; i < numberOfCameras; i++) {
            Camera.getCameraInfo(i, cameraInfo);
            if (cameraInfo.facing == typeId) {
                mSelectedCameraId = i;
            }
        }

        try {
            mCamera = Camera.open(mSelectedCameraId);

            Camera.Parameters parameters = mCamera.getParameters();

            mPreviewFormat = parameters.getPreviewFormat();
            mSupportedPreviewSizes = parameters.getSupportedPreviewSizes();

            if (mPreviewSize == null) {
                mPreviewSize = mSupportedPreviewSizes.get(0);
                parameters.setPreviewSize(mPreviewSize.width, mPreviewSize.height);
            } else {
                boolean found = false;
                for (Camera.Size size : mSupportedPreviewSizes) {
                    if (size.width == mPreviewSize.width && size.height == mPreviewSize.height) {
                        parameters.setPreviewSize(mPreviewSize.width, mPreviewSize.height);
                        found = true;
                        break;
                    }
                }

                if (!found) {
                    mPreviewSize = mSupportedPreviewSizes.get(0);
                    parameters.setPreviewSize(mPreviewSize.width, mPreviewSize.height);
                }
            }

            mCamera.setParameters(parameters);
        }
        catch(Exception e){
            e.printStackTrace();
            closeCamera();
            return;
        }

        requestLayout();

        startPreview();
        startCapture();
    }

    public void closeCamera(){

        stopCapture();
        stopPreview();

        if (mCamera != null) {
            mCamera.release();
            mCamera = null;
        }
    }

    private void startPreview(){

        if (mCamera != null){
            if (mHolder != null) {
                try {
                    mCamera.setPreviewDisplay(mHolder);
                }
                catch (IOException e) {
                    e.printStackTrace();
                }
                mCamera.startPreview();
            }
            else {
                mStartPreviewAfterSurfaceCreated = true;
            }
        }
    }

    private void stopPreview(){

        mStartPreviewAfterSurfaceCreated = false;

        if (mCamera != null) {

            mCamera.stopPreview();

            try {
                mCamera.setPreviewDisplay(null);
            }
            catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    public void startCapture(){

        stopCapture();

        final int bitsPerPixel = ImageFormat.getBitsPerPixel(mPreviewFormat);
        mCamera.setPreviewCallback(null);
        int size = mPreviewSize.width * mPreviewSize.height * bitsPerPixel / 8;
        mFrameBuffer = new byte[size];
        mCamera.addCallbackBuffer(mFrameBuffer);
        mCamera.setPreviewCallbackWithBuffer(new Camera.PreviewCallback() {
            @Override
            public void onPreviewFrame(byte[] data, Camera camera) {
                if (mEventListener != null){
                    //
                    // JPEG圧縮データ
                    byte[] jpegData = convertYuv2Jpeg(data, mPreviewFormat, mPreviewSize.width, mPreviewSize.height);

                    if (jpegData != null) {
                        mEventListener.OnFrame(jpegData);
                    }
                }
                mCamera.addCallbackBuffer(mFrameBuffer);
            }
        });
    }

    public void stopCapture(){
        if (mCamera != null){
            mCamera.setPreviewCallbackWithBuffer(null);
        }
    }

    private byte[] convertYuv2Jpeg(byte[] yuvData, int format, int w, int h) {
        byte[] jpegData = null;

        if (yuvData != null) {
            try {
                YuvImage yuvimage = new YuvImage(yuvData, format, w, h, null);
                ByteArrayOutputStream baos = new ByteArrayOutputStream();
                yuvimage.compressToJpeg(new Rect(0, 0, w, h), 70, baos);
                jpegData = baos.toByteArray();
                baos.close();
            }
            catch (Exception e) {
                e.printStackTrace();
            }
        }
        return jpegData;
    }
}
