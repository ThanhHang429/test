package com.eventapp.app.ui.host;

import android.os.Bundle;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

/**
 * Khung tối thiểu dùng chung cho 3 khu chức năng (người dùng, nhà tổ chức, quản trị).
 * Giao diện mới sẽ được gắn vào đây sau khi dựng lại.
 */
public abstract class BaseHostActivity extends AppCompatActivity {

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (savedInstanceState == null) {
            getSupportFragmentManager()
                    .beginTransaction()
                    .replace(android.R.id.content, new PlaceholderFragment())
                    .commit();
        }
    }
}
