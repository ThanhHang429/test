package com.eventapp.app.ui.host;

import android.os.Bundle;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.navigation.fragment.NavHostFragment;
import androidx.navigation.ui.NavigationUI;

import com.eventapp.app.R;
import com.google.android.material.bottomnavigation.BottomNavigationView;

/** FR-O02: host chứa khu vực nhà tổ chức và bottom navigation. */
public class OrganizerHostActivity extends AppCompatActivity {

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_organizer_host);

        NavHostFragment navHost = (NavHostFragment) getSupportFragmentManager()
                .findFragmentById(R.id.nav_host_fragment);
        if (navHost != null) {
            NavigationUI.setupWithNavController(
                    (BottomNavigationView) findViewById(R.id.bottom_navigation),
                    navHost.getNavController());
        }
    }
}
