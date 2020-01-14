package com.jacky.uitest.activity;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;

import com.jacky.uitest.R;
import com.jacky.uitest.utils.PermissionUtil;

public class LanguageActivity extends BaseActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_language);
        PermissionUtil.permissionAsk(this);

        findViewById(R.id.chineseOcr).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(LanguageActivity.this, SerialPortActivity.class);
                intent.putExtra("chinese", getResources().getString(R.string.chinese));
                startActivity(intent);
            }
        });

        findViewById(R.id.englishOcr).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(LanguageActivity.this, SerialPortActivity.class);
                intent.putExtra("english", getResources().getString(R.string.english));
                startActivity(intent);
            }
        });
    }
}
