package com.bytedance.trainingcamp;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentActivity;
import androidx.viewpager2.adapter.FragmentStateAdapter;

public class VideoAdapter extends FragmentStateAdapter {
    private String[] videos;

    public VideoAdapter(@NonNull FragmentActivity fa, String[] videos) {
        super(fa);
        this.videos = videos;
    }

    @NonNull
    @Override
    public Fragment createFragment(int position) {
        return VideoFragment.newInstance(videos[position]);
    }

    @Override
    public int getItemCount() {
        return videos.length;
    }
}
