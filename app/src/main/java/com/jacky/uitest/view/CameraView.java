package com.jacky.uitest.view;

import android.annotation.SuppressLint;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.ImageFormat;
import android.graphics.Rect;
import android.graphics.YuvImage;
import android.hardware.Camera;
import android.text.TextUtils;
import android.util.AttributeSet;
import android.util.Log;
import android.view.SurfaceHolder;
import android.view.SurfaceView;

import com.jacky.uitest.App;
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

    private SurfaceHolder holder;
    private Camera mCamera;
    private boolean isPreview, isOcrDoing;
    //preview size default
    private int imageHeight = 1080;
    private int imageWidth = 1920;

    private MyImageView hintImage;

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

    private long startTime, endTime;

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
                            final Bitmap ocrBitmap = OcrUtil.getInstance().catchPhoneRect(OcrUtil.rotateToDegrees(bitmap, 90), hintImage);
                            if (null == ocrBitmap) {
                                isOcrDoing = false;
                                return;
                            }
                            endTime = System.currentTimeMillis();
                            Log.d(OCR_TAG, "deal with image data: " + (endTime - startTime) + "ms");
                            startTime = endTime;
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
                                    isOcrDoing = false;
                                    Log.d(OCR_TAG, "-----------------end----------------");
                                }
                            });
                        }
                    } catch (Exception e) {
                        Log.d(OCR_TAG, e.getMessage());
                        isOcrDoing = false;
                    }
                }
            }).start();
        }


    }

    @Override
    public void surfaceCreated(SurfaceHolder holder) {
        openCamera(1);
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
        param.setPreviewSize(imageWidth, imageHeight);
        param.setPictureSize(imageWidth, imageHeight);

        //preview frame default
        int frame = 40;
        param.setPreviewFrameRate(frame);

        mCamera.setParameters(param);
        mCamera.setDisplayOrientation(270);
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
        Pattern pattern = Pattern.compile("[a-zA-Z]+");
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
}
