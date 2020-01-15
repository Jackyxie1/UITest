package com.jacky.uitest.utils;

import android.graphics.Bitmap;
import android.graphics.Matrix;
import android.os.Environment;
import android.view.View;

import com.googlecode.tesseract.android.TessBaseAPI;
import com.jacky.uitest.callback.RecognizeCallback;
import com.jacky.uitest.view.MyImageView;

import java.io.File;
import java.util.Stack;

public class OcrUtil {
    private static final String TAG = "OcrUtil";
    private static final String FONT_DATA_PATH = Environment.getExternalStorageDirectory() + File.separator + "Download" + File.separator;
    //language
    private static final String CN = "chi_sim";
    private static final String ENG = "eng";

    private float proportion = 0.5f;

    private static OcrUtil mOcrUtil = null;

    private OcrUtil() {
    }

    public static OcrUtil getInstance() {
        if (null == mOcrUtil) {
            synchronized (OcrUtil.class) {
                if (null == mOcrUtil)
                    mOcrUtil = new OcrUtil();
            }
        }
        return mOcrUtil;
    }

    static {
        System.loadLibrary("native-lib");
    }

    /**
     * bitmap rotate
     */
    public static Bitmap rotateToDegrees(Bitmap bitmap, float degrees) {
        Matrix matrix = new Matrix();
        matrix.reset();
        matrix.setRotate(degrees);
        return Bitmap.createBitmap(bitmap, 0, 0, bitmap.getWidth(), bitmap.getHeight(), matrix, true);
    }

    private boolean checkEngFontData() {
        File file = new File(FONT_DATA_PATH + "/tessdata/");
        if (!file.exists())
            throw new RuntimeException("there is no right font data path:\"" + FONT_DATA_PATH + "/tessdata/\"");

        String fontPath = FONT_DATA_PATH + "/tessdata/eng.traineddata";
        file = new File(fontPath);
        if (!file.exists())
            throw new RuntimeException("there is no right font data file:\"" + FONT_DATA_PATH + "/tessdata/eng.traineddata\"");
        return true;
    }

    private boolean checkChiFontData() {
        File file = new File(FONT_DATA_PATH + "/tessdata/");
        if (!file.exists())
            throw new RuntimeException("there is no right font data path:\"" + FONT_DATA_PATH + "/tessdata/\"");

        String fontPath = FONT_DATA_PATH + "/tessdata/chi_sim.traineddata";
        file = new File(fontPath);
        if (!file.exists())
            throw new RuntimeException("there is no right font data file:\"" + FONT_DATA_PATH + "/tessdata/chi_sim.traineddata\"");
        return true;
    }

    public void scanEnglish(final Bitmap bitmap, final RecognizeCallback callback) {
        if (checkEngFontData()) {
            TessBaseAPI baseAPI = new TessBaseAPI();
            if (baseAPI.init(FONT_DATA_PATH, ENG)) {
                baseAPI.setPageSegMode(TessBaseAPI.PageSegMode.PSM_SPARSE_TEXT);
                baseAPI.setImage(bitmap);
                baseAPI.setVariable(TessBaseAPI.VAR_SAVE_BLOB_CHOICES, TessBaseAPI.VAR_TRUE);

                String result = baseAPI.getUTF8Text();
                baseAPI.clear();
                baseAPI.end();
                bitmap.recycle();
                callback.response(result);
            }
        }
    }

    public void scanChinese(final Bitmap bitmap, final RecognizeCallback callback) {
        if (checkChiFontData()) {
            TessBaseAPI baseAPI = new TessBaseAPI();
            if (baseAPI.init(FONT_DATA_PATH, CN)) {
                baseAPI.setPageSegMode(TessBaseAPI.PageSegMode.PSM_SPARSE_TEXT);
                baseAPI.setImage(bitmap);
                baseAPI.setVariable(TessBaseAPI.VAR_SAVE_BLOB_CHOICES, TessBaseAPI.VAR_TRUE);

                String result = baseAPI.getUTF8Text();
                baseAPI.clear();
                baseAPI.end();
                bitmap.recycle();
                callback.response(result);
            }
        }
    }

    private void showImage(final Bitmap bmp, final MyImageView imageView) {
        //将裁切的图片显示出来（测试用，需要为CameraView  setTag（ImageView））
        ThreadUtil.getInstance().
                execute(new Runnable() {
                    @Override
                    public void run() {
                        imageView.setVisibility(View.VISIBLE);
                        imageView.setImageBitmap(bmp);
                    }
                });
    }

    private final int PX_WHITE = -1;
    private final int PX_BLACK = -16777216;
    private final int PX_UNKNOW = -2;

    public Bitmap getBitmap(Bitmap bitmap, MyImageView imageView) {
        int width = bitmap.getWidth();
        int height = bitmap.getHeight();
        int[] pixels = new int[width * height];
        bitmap.getPixels(pixels, 0, width, 0, 0, width, height);

        //bitmap binary
        getBinaryBitmap(bitmap);

        bitmap.getPixels(pixels, 0, width, 0, 0, width, height);

        int space = 0;
        int textWidth = 0;
        int startX = 0;
        int textLength = 0;
        int textStartX = 0;
        int right = 0;
        int bottom = 0;
        int left = width;
        int top = height;
        int iCount = 0;

        //to determine whether there is word existing
        for (int i = 10; i < height; i = i + 10) {
            for (int j = 0; j < width; j++) {
                if (pixels[i * j] == PX_WHITE) {
                    if (1 == space) textLength++;
                    if (textWidth > 0 && startX > 0 && startX < width - 1 && (space > 100 || j == width - 1)) {
                        if (textLength > 1)
                            if (textWidth > right - left) {
                                left = j - space - textWidth - (space / 2);
                                if (left < 0)
                                    left = 0;
                                right = j - 1 - (space / 2);
                                if (right > width)
                                    right = width - 1;
                                textStartX = startX;
                            }
                        textLength = 0;
                        space = 0;
                        startX = 0;
                    }
                    space++;
                } else {
                    if (startX == 0)
                        startX = j;
                    textWidth = j - startX;
                    space = 0;
                }
            }
            if (right - left < width * 0.1f)
                break;
            iCount = i + 10;
        }

        if (iCount > height) return null;

        Bitmap bitmapTmp=Bitmap.createBitmap(width,height, Bitmap.Config.ARGB_8888);
        if (null != imageView) {
            bitmapTmp.setPixels(pixels, 0, width, 0, 0, width, height);
            showImage(bitmapTmp, imageView);
        } else {
            bitmapTmp.recycle();
        }
        return bitmapTmp;
    }

    //bitmap binary by ndk
    public static native void getBinaryBitmap(Object bitmap);
}
