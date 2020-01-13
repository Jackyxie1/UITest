package com.jacky.uitest.utils;

import android.graphics.Bitmap;
import android.graphics.Matrix;
import android.os.Environment;

import com.googlecode.tesseract.android.TessBaseAPI;
import com.jacky.uitest.callback.RecognizeCallback;

import java.io.File;

public class OcrUtil {
    private static final String FONT_DATA_PATH = Environment.getExternalStorageDirectory() + File.separator + "Download" + File.separator;
    //language
    private static final String CN = "chi.sim";
    private static final String ENG = "eng";

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

    /**
     * bitmap rotate
     */
    public static Bitmap rotateToDegrees(Bitmap bitmap, float degrees) {
        Matrix matrix = new Matrix();
        matrix.reset();
        matrix.setRotate(degrees);
        return Bitmap.createBitmap(bitmap, 0, 0, bitmap.getWidth(), bitmap.getHeight(), matrix, true);
    }

    public boolean checkEngFontData() {
        File file = new File(FONT_DATA_PATH + "/tessdata/");
        if (!file.exists())
            throw new RuntimeException("there is no right font data path:\"" + FONT_DATA_PATH + "/tessdata/\"");

        String fontPath = FONT_DATA_PATH + "/tessdata/eng.traineddata";
        file = new File(fontPath);
        if (!file.exists())
            throw new RuntimeException("there is no right font data file:\"" + FONT_DATA_PATH + "/tessdata/eng.traineddata\"");
        return true;
    }

    public boolean checkChiFontData() {
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
        if (checkEngFontData()) {
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

    private final int PX_WHITE = -1;
    private final int PX_BLACK = -16777216;
    private final int PX_UNKNOW = -2;

    private int redThresh = 128;
    private int blueThresh = 128;
    private int greenThresh = 128;

    /**
     * binarization
     */
    private void binarization(int[] pixels, int width, int height) {
        for (int i = 0; i < height; i++)
            for (int j = 0; j < width; j++) {
                int gray = pixels[width * i + j];
                pixels[width * i + j] = getColor(gray);
                if (pixels[width * i + j] != PX_WHITE)
                    pixels[width * i + j] = PX_BLACK;
            }
    }

    private int getColor(int gray) {
        int alpha = 0xFF << 24;
        alpha = ((gray & 0xFF000000) >> 24);
        int red = ((gray & 0x00FF0000) >> 16);
        int green = ((gray & 0x0000FF00) >> 8);
        int blue = (gray & 0x000000FF);
        if (red > redThresh) {
            red = 255
        } else {
            red = 0;
        }

        if (blue > blueThresh) {
            blue = 255;
        } else {
            blue = 0;
        }

        if (green > greenThresh) {
            green = 255;
        } else {
            green = 0;
        }

        return alpha << 24 | red << 16 | green << 8 | blue;
    }
}
