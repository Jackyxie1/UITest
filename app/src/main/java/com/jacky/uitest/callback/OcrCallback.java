package com.jacky.uitest.callback;

public interface OcrCallback {
    void onResult(String result);

    void onResponseTimes(String time);

    void onOcrTimes(String time);
}
