package com.eventapp.app.ui.host;

import android.os.Bundle;

import androidx.annotation.Nullable;

import com.eventapp.app.R;

/** FR-O02: host chứa khu vực nhà tổ chức. Giao diện sẽ được dựng lại sau. */
public class OrganizerHostActivity extends BaseHostActivity {

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        // Áp overlay trước khi inflate để các component đọc ?attr/colorPrimary ra teal.
        getTheme().applyStyle(R.style.ThemeOverlay_EventApp_Organizer, true);
        super.onCreate(savedInstanceState);
    }
}
