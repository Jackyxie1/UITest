package com.jacky.uitest.view;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.ImageFormat;
import android.graphics.Rect;
import android.graphics.YuvImage;
import android.hardware.Camera;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Message;
import android.text.TextUtils;
import android.util.AttributeSet;
import android.util.Log;
import android.view.Surface;
import android.view.SurfaceHolder;
import android.view.SurfaceView;

import androidx.annotation.NonNull;

import com.jacky.uitest.App;
import com.jacky.uitest.callback.OcrCallback;
import com.jacky.uitest.callback.RecognizeCallback;
import com.jacky.uitest.utils.OcrUtil;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class CameraView extends SurfaceView implements SurfaceHolder.Callback, Camera.PreviewCallback {

    private static final String TAG = "CameraView";
    private static final String OCR_TAG = "OCR";

    String cn = "中文识别";
    String eng = "英文识别";

    private static final int MSG_OCR_TIME_COST = 100;
    private static final int MSG_OCR_RESULT = 101;
    private static final int MSG_OCR_TIME_RESPONSE = 102;

    private SurfaceHolder holder;
    private Camera mCamera;
    private boolean isPreview, isOcrDoing, isOcrComplete;
    //preview size default
    private int imageHeight = 1080;
    private int imageWidth = 1920;

    private MyImageView hintImage;
    Activity activity;
    OcrCallback callback = null;

    public CameraView(Context context) {
        super(context);
        init(context);
    }

    public CameraView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init(context);
    }

    public CameraView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init(context);
    }

    private void init(Context context) {
        holder = getHolder();
        holder.addCallback(this);
        holder.setType(SurfaceHolder.SURFACE_TYPE_PUSH_BUFFERS);
        activity = (Activity) context;
    }

    private long startTime, endTime, firstTime;

    @Override
    public void onPreviewFrame(final byte[] data, final Camera camera) {
        if (!isOcrDoing) {
            isOcrDoing = true;
            new Thread(new Runnable() {
                @Override
                public void run() {
                    try {
                        Log.d(OCR_TAG, "----------------start---------------");
                        startTime = System.currentTimeMillis();
                        firstTime = startTime;
                        Camera.Size size = camera.getParameters().getPreviewSize();
                        int left = 0;
                        int top = 0;
                        int right = size.width;
                        int bottom = size.height;
                        final YuvImage image = new YuvImage(data, ImageFormat.NV21, size.width, size.height, null);
                        if (null != image) {
                            ByteArrayOutputStream stream = new ByteArrayOutputStream();
                            image.compressToJpeg(new Rect(left, top, right, bottom), getQuality(size.height), stream);
                            Bitmap bitmap = BitmapFactory.decodeByteArray(stream.toByteArray(), 0, stream.size());

                            endTime = System.currentTimeMillis();
                            Log.d(OCR_TAG, "frame data compress to bitmap cost: " + (endTime - startTime) + "ms");
                            startTime = endTime;
                            if (null == bitmap) {
                                isOcrDoing = false;
                                return;
                            }
                            if (null == hintImage && null != getTag()) {
                                if (getTag() instanceof MyImageView) {
                                    hintImage = (MyImageView) getTag();
                                }
                            }
                            final Bitmap ocrBitmap = OcrUtil.getInstance().getBitmap(bitmap, hintImage);
                            if (null == ocrBitmap) {
                                isOcrDoing = false;
                                return;
                            }
                            endTime = System.currentTimeMillis();
                            Log.d(OCR_TAG, "deal with image data: " + (endTime - startTime) + "ms");
                            startTime = endTime;
                            if (TextUtils.equals(type, cn)) {
                                //start ocr
                                OcrUtil.getInstance().scanChinese(ocrBitmap, new RecognizeCallback() {
                                    @Override
                                    public void response(String result) {
                                        endTime = System.currentTimeMillis();
                                        Log.d(OCR_TAG, "ocr time cost: " + (endTime - startTime) + "ms");
                                        startTime = endTime;
                                        Log.d(OCR_TAG, "ocr result: " + result);
                                        if (!TextUtils.isEmpty(getChinese(result.replace(" ", "")))) {
                                            Log.d(OCR_TAG, "ocr result format: " + getChinese(result));
                                        }
                                        endTime = System.currentTimeMillis();//ocr complete
                                        isOcrDoing = false;
                                        Log.d(OCR_TAG, "-----------------end----------------");
                                        sendInfo(endTime, getChinese(result));
                                        isOcrComplete = true;
                                    }
                                });
                            }
                            if (TextUtils.equals(type, eng)) {
                                //start ocr
                                OcrUtil.getInstance().scanEnglish(ocrBitmap, new RecognizeCallback() {
                                    @Override
                                    public void response(String result) {
                                        endTime = System.currentTimeMillis();
                                        Log.d(OCR_TAG, "ocr time cost: " + (endTime - startTime) + "ms");
                                        startTime = endTime;
                                        Log.d(OCR_TAG, "ocr result: " + result);
                                        if (!TextUtils.isEmpty(getEnglish(result.replace(" ", "")))) {
                                            Log.d(OCR_TAG, "ocr result format: " + getEnglish(result));
                                        }
                                        endTime = System.currentTimeMillis();//ocr complete
                                        isOcrDoing = false;
                                        Log.d(OCR_TAG, "-----------------end----------------");
                                        sendInfo(endTime, getEnglish(result));
                                        isOcrComplete = true;
                                    }
                                });
                            }
                        }
                    } catch (Exception e) {
                        e.printStackTrace();
                        isOcrDoing = false;
                    }
                }
            }).start();
        }
    }

    @Override
    public void surfaceCreated(SurfaceHolder holder) {
        getCameraInstance(1);
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

//        imageWidth = sizes.get(0).width;
//        imageHeight = sizes.get(0).height;
//        param.setPreviewSize(imageWidth, imageHeight);
//        param.setPictureSize(imageWidth, imageHeight);

        //preview frame default
        int frame = 40;
        param.setPreviewFrameRate(frame);

        mCamera.setParameters(param);
        setPreviewOrientation(activity, mCamera, 1);
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

    private String getChinese(String param) {
        if (param.length() <= 0)
            return "";
        Pattern pattern = Pattern.compile("[^\\u4E00-\\u9FA5]");
        Matcher matcher = pattern.matcher(param);
        String realResult = matcher.replaceAll("");
        return realResult;
    }

    private String getEnglish(String param) {
        if (param.length() <= 0)
            return "";
        Pattern pattern = Pattern.compile("[a-zA-Z-_]+");
        Matcher matcher = pattern.matcher(param);
        StringBuffer sb = new StringBuffer();
        while (matcher.find()) {
            sb.append(matcher.group()).append(" ");
        }
        int len = sb.length();
        if (len > 0) {
            sb.deleteCharAt(len - 1);
        }
        return sb.toString();
    }

    public Camera getCameraInstance(int facing) {
        if (null == mCamera) {
            CameraHandlerThread mThread = new CameraHandlerThread("camera thread");
            synchronized (mThread) {
                mThread.openCamera(facing);
            }
        }
        return mCamera;
    }

    private class CameraHandlerThread extends HandlerThread {

        Handler mHandler;

        public CameraHandlerThread(String name) {
            super(name);
            start();
            mHandler = new Handler(getLooper());
        }

        synchronized void notifyCameraOpened() {
            notify();
        }

        void openCamera(final int facing) {
            mHandler.post(new Runnable() {
                @Override
                public void run() {
                    selectCamera(facing);
                    notifyCameraOpened();
                }
            });
            try {
                wait();
            } catch (InterruptedException e) {
                Log.w(TAG, "wait was interrupted");
            }
        }
    }

    private void selectCamera(int facing) {
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

    private void setPreviewOrientation(Activity activity, Camera camera, int facing) {
        Camera.CameraInfo info = new Camera.CameraInfo();
        Camera.getCameraInfo(facing, info);
        int rotation = activity.getWindowManager().getDefaultDisplay().getRotation();
        int degrees = 0;
        switch (rotation) {
            case Surface.ROTATION_0:
                degrees = 0;
                break;
            case Surface.ROTATION_90:
                degrees = 90;
                break;
            case Surface.ROTATION_180:
                degrees = 180;
                break;
            case Surface.ROTATION_270:
                degrees = 270;
                break;
        }
        int displayDegree;
        if (info.facing == Camera.CameraInfo.CAMERA_FACING_FRONT) {
            displayDegree = (info.orientation + degrees) % 360;
            displayDegree = (360 - displayDegree) % 360;
        } else {
            displayDegree = (info.orientation - degrees + 360) % 360;
        }
        camera.setDisplayOrientation(displayDegree);
    }

    private Handler infoHandler = new Handler() {
        @Override
        public void handleMessage(@NonNull Message msg) {
            super.handleMessage(msg);
            switch (msg.what) {
                case MSG_OCR_TIME_COST:
                    if (null != callback)
                        callback.onOcrTimes((String) msg.obj);
                    break;
                case MSG_OCR_RESULT:
                    if (null != callback)
                        callback.onResult((String) msg.obj);
                    break;
                case MSG_OCR_TIME_RESPONSE:

                    break;
            }
        }
    };

    private void sendInfo(long endTime, String result) {

        //bitmap data dispose and ocr cost
        long doOcrCost = endTime - firstTime;
        String ocrTimeString = doOcrCost + "ms";
        handleOcrTimeCost(ocrTimeString);

        //ocr result send
        handleOcrResult(result);

        //response time send
        handleResponseTime("搁置");


    }

    private void handleOcrTimeCost(String time) {
        if (null != infoHandler)
            infoHandler.sendMessage(infoHandler.obtainMessage(MSG_OCR_TIME_COST, time));
    }

    private void handleOcrResult(String result) {
        if (null != infoHandler)
            infoHandler.sendMessage(infoHandler.obtainMessage(MSG_OCR_RESULT, result));
    }

    private void handleResponseTime(String time) {
        if (null != infoHandler)
            infoHandler.sendMessage(infoHandler.obtainMessage(MSG_OCR_TIME_RESPONSE, time));
    }

    String type;

    public void setOcrMode(String language) {
        type = language;
    }

    private void setOcrCallbackListener(OcrCallback callback) {
        this.callback = callback;
    }

    public void setListener(OcrCallback callback) {
        setOcrCallbackListener(callback);
    }

}
