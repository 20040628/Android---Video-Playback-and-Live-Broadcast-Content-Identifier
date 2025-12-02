package com.bytedance.trainingcamp;

import android.os.Bundle;
import android.util.Log;

import androidx.appcompat.app.AppCompatActivity;
import androidx.viewpager2.widget.ViewPager2;


public class MainActivity extends AppCompatActivity {

    private static final String TAG = "MainActivity";

    private ViewPager2 viewPager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main_activity);

        viewPager = findViewById(R.id.viewPager);

        // 允许垂直滑动（像抖音）
        viewPager.setOrientation(ViewPager2.ORIENTATION_VERTICAL);

        String[] videos = {"video1.mp4", "video2.mp4"};

        viewPager.setAdapter(new VideoAdapter(this, videos));
    }
}
