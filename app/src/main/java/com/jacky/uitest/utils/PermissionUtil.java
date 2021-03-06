package com.jacky.uitest.utils;

import android.Manifest;
import android.os.Build;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;

public class PermissionUtil {
    public static void permissionAsk(AppCompatActivity activity) {
        if (Build.VERSION.SDK_INT >= 23)
            ActivityCompat.requestPermissions(activity, new String[]{Manifest.permission.CAMERA,
                    Manifest.permission.WRITE_EXTERNAL_STORAGE,
                    Manifest.permission.READ_EXTERNAL_STORAGE}, 1);
    }
}
