package com.studybuddy.app;

import android.os.Bundle;

import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;

import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.studybuddy.app.data.Seeder;
import com.studybuddy.app.ui.HomeFragment;
import com.studybuddy.app.ui.LibraryFragment;
import com.studybuddy.app.ui.PlayFragment;
import com.studybuddy.app.ui.TeacherFragment;
import com.studybuddy.app.util.Async;

public class MainActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        final boolean fresh = savedInstanceState == null;
        final BottomNavigationView nav = findViewById(R.id.bottom_nav);

        // First launch: create the built-in database and fill it with starter content.
        Async.<Boolean>load(() -> {
            Seeder.ensureSeeded(getApplicationContext());
            return true;
        }, ok -> {
            if (isFinishing() || isDestroyed()) {
                return;
            }
            nav.setOnItemSelectedListener(item -> {
                Fragment f;
                int id = item.getItemId();
                if (id == R.id.nav_library) {
                    f = new LibraryFragment();
                } else if (id == R.id.nav_play) {
                    f = new PlayFragment();
                } else if (id == R.id.nav_teacher) {
                    f = new TeacherFragment();
                } else {
                    f = new HomeFragment();
                }
                getSupportFragmentManager().beginTransaction().replace(R.id.container, f).commit();
                return true;
            });
            if (fresh) {
                nav.setSelectedItemId(R.id.nav_home);
            }
        });
    }
}
