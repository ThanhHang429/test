package com.eventapp.app.ui.organizer.checkin;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.eventapp.app.R;
import com.eventapp.app.ui.common.PlaceholderContent;

/** FR-O19 — QR check-in của sự kiện; phần xử lý phía sau chưa có nên hiện trạng thái đang xây dựng. */
public class ShowCheckinQrFragment extends Fragment {

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return PlaceholderContent.create(inflater, container, R.string.nav_checkin);
    }
}
