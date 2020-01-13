package com.jacky.uitest.view;

import android.annotation.SuppressLint;
import android.content.Context;
import android.graphics.Bitmap;
import android.hardware.Camera;
import android.util.AttributeSet;
import android.view.SurfaceHolder;
import android.view.SurfaceView;

import com.jacky.uitest.App;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.List;

public class CameraView extends SurfaceView implements SurfaceHolder.Callback, Camera.PreviewCallback {

    private static final String TAG = "CameraView";

    private SurfaceHolder holder;
    private Camera mCamera;
    private boolean isPreview;
    //preview size default
    private int imageHeight = 1080;
    private int imageWidth = 1920;
    //preview frame default
    private int frame = 30;

    public CameraView(Context context) {
        super(context);
        init();
    }

    public CameraView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    public CameraView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init();
    }

    private void init() {
        holder = getHolder();
        holder.addCallback(this);
        holder.setType(SurfaceHolder.SURFACE_TYPE_PUSH_BUFFERS);
    }

    @Override
    public void onPreviewFrame(byte[] data, Camera camera) {
    }

    @Override
    public void surfaceCreated(SurfaceHolder holder) {
        openCamera(0);
    }

    @Override
    public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {
        if (null != mCamera)
            initCameraParams();
    }

    @Override
    public void surfaceDestroyed(SurfaceHolder holder) {
        try {
            release();
        } catch (Exception e) {
        }
    }

    private int getQuality(int width) {
        int quality = 100;
        if (width > 480) {
            float w = 480 / (float) width;
            quality = (int) (w * 100);
        }
        return quality;
    }

    @SuppressLint("WrongThread")
    public String saveFrameData(Bitmap bitmap, String fileName) {
        if (null == bitmap) return null;
        File file = null;
        try {
            file = new File(App.getContext().getCacheDir().getAbsolutePath() + fileName);
            if (!file.exists())
                file.createNewFile();
        } catch (IOException e) {
            e.printStackTrace();
        }
        FileOutputStream fos = null;
        try {
            fos = new FileOutputStream(file);
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }
        bitmap.compress(Bitmap.CompressFormat.JPEG, 100, fos);
        if (null != fos) {
            try {
                fos.flush();
            } catch (IOException e) {
                e.printStackTrace();
            }
            try {
                fos.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        if (file.exists())
            return file.getAbsolutePath();
        return null;
    }

    /**
     * set camera parameter
     */
    public void initCameraParams() {
        stopPreview();
        Camera.Parameters param = mCamera.getParameters();
        List<Camera.Size> sizes = param.getSupportedPreviewSizes();
        for (int i = 0; i < sizes.size(); i++) {
            if ((sizes.get(i).width >= imageWidth && sizes.get(i).height >= imageHeight) || i == sizes.size() - 1) {
                imageWidth = sizes.get(i).width;
                imageHeight = sizes.get(i).height;
                break;
            }
        }
        param.setPreviewSize(imageWidth, imageHeight);
        param.setPictureSize(imageWidth, imageHeight);

        param.setPreviewFrameRate(frame);

        mCamera.setParameters(param);
        mCamera.setDisplayOrientation(90);
        startPreview();

    }


    public void startPreview() {
        try {
            mCamera.setPreviewCallback(this);
            mCamera.setPreviewDisplay(holder);
            mCamera.startPreview();
            mCamera.autoFocus(autofocus);
        } catch (IOException e) {
            mCamera.release();
            mCamera = null;
        }
    }

    public void stopPreview() {
        if (null != mCamera) {
            mCamera.setPreviewCallback(null);
            mCamera.stopPreview();
            mCamera = null;
        }
    }

    public void openCamera(int facing) {
        Camera.CameraInfo info = new Camera.CameraInfo();
        switch (facing) {
            case 1:
                for (int cameraId = 0; cameraId < Camera.getNumberOfCameras(); cameraId++) {
                    Camera.getCameraInfo(cameraId, info);
                    if (info.facing == Camera.CameraInfo.CAMERA_FACING_FRONT) {
                        try {
                            mCamera = Camera.open(cameraId);
                        } catch (Exception e) {
                            if (null != mCamera) {
                                mCamera.release();
                                mCamera = null;
                            }
                        }
                        break;
                    }
                }
                break;
            case 0:
                for (int cameraId = 0; cameraId < Camera.getNumberOfCameras(); cameraId++) {
                    Camera.getCameraInfo(cameraId, info);
                    if (info.facing == Camera.CameraInfo.CAMERA_FACING_BACK) {
                        try {
                            mCamera = Camera.open(cameraId);
                        } catch (Exception e) {
                            if (null != mCamera) {
                                mCamera.release();
                                mCamera = null;
                            }
                        }
                        break;
                    }
                }
                break;
            default:
                break;
        }
    }

    //camera auto focus
    Camera.AutoFocusCallback autofocus = new Camera.AutoFocusCallback() {
        @Override
        public void onAutoFocus(boolean success, Camera camera) {
            postDelayed(doAutoFocus, 1000);
        }
    };
    private Runnable doAutoFocus = new Runnable() {
        @Override
        public void run() {
            if (null != mCamera) {
                try {
                    mCamera.autoFocus(autofocus);
                } catch (Exception e) {
                }
            }
        }
    };

    public void release() {
        if (isPreview && null != mCamera) {
            isPreview = false;
            mCamera.setPreviewCallback(null);
            mCamera.stopPreview();
            mCamera.release();
            mCamera = null;
        }
    }
}
