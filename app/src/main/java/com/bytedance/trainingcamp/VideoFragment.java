package com.bytedance.trainingcamp;

import android.content.res.AssetFileDescriptor;
import android.media.MediaPlayer;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;


public class VideoFragment extends Fragment {

    private static final String ARG_VIDEO = "video_name";

    private MediaPlayer mediaPlayer;
    private String videoName;
    private SurfaceHolder holder;
    private boolean surfaceReady = false;

    public static VideoFragment newInstance(String videoName) {
        VideoFragment fragment = new VideoFragment();
        Bundle args = new Bundle();
        args.putString(ARG_VIDEO, videoName);
        fragment.setArguments(args);
        return fragment;
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.video_player, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {

        videoName = getArguments().getString(ARG_VIDEO);

        SurfaceView surfaceView = view.findViewById(R.id.surfaceView);
        holder = surfaceView.getHolder();

        holder.addCallback(new SurfaceHolder.Callback() {
            @Override
            public void surfaceCreated(SurfaceHolder surfaceHolder) {
                surfaceReady = true;
                startVideo();

                // 调用讯飞接口解析视频文本
                XunFei uploader = new XunFei(getActivity(), videoName);
                uploader.uploadAudio();
//                uploader.queryTranscriptionResult("DKHJQ20251203170415839kkixzqV1Gs31XGf2");
            }

            @Override public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {}
            @Override public void surfaceDestroyed(SurfaceHolder holder) {
                surfaceReady = false;
            }
        });

        // 点击暂停继续
        surfaceView.setOnClickListener(v -> {
            if (mediaPlayer != null) {
                if (mediaPlayer.isPlaying()) mediaPlayer.pause();
                else mediaPlayer.start();
            }
        });
    }

    @Override
    public void onResume() {
        super.onResume();
        startVideo();   // 当Fragment可见时播放
    }

    @Override
    public void onPause() {
        super.onPause();
        releasePlayer();  // Fragment离开屏幕时释放
    }

    private void startVideo() {
        if (!surfaceReady) return;

        releasePlayer();  // 创建前先释放

        mediaPlayer = new MediaPlayer();
        mediaPlayer.setLooping(true);
        mediaPlayer.setDisplay(holder);

        try {
            AssetFileDescriptor afd = getActivity().getAssets().openFd(videoName);
            mediaPlayer.setDataSource(
                    afd.getFileDescriptor(),
                    afd.getStartOffset(),
                    afd.getLength()
            );
            mediaPlayer.prepare();
            mediaPlayer.start();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void releasePlayer() {
        if (mediaPlayer != null) {
            try {
                mediaPlayer.stop();
            } catch (Exception ignored) {}
            mediaPlayer.release();
            mediaPlayer = null;
        }
    }
}
