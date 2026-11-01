package com.eventapp.app.ui.organizer.onboarding;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.eventapp.app.R;
import com.eventapp.app.ui.common.PlaceholderContent;

/** Đích điều hướng đã có trong nav graph nhưng giao diện thuộc phần của thành viên khác. */
public class OrganizerProfileFragment extends Fragment {

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return PlaceholderContent.create(inflater, container, R.string.nav_organizer_profile);
    }
}
