package com.bytedance.trainingcamp;

import android.content.res.AssetFileDescriptor;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Base64;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.security.MessageDigest;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;


public class VideoFragment extends Fragment {

    private static final String ARG_VIDEO = "video_name";
    private static final String APP_ID = "7fee0d4b";
    private static final String SECRET_KEY = "b32c4692b7c325539a0daf6f551bad6d";
    private static String ORDER_ID = "";
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

                // 视频显示后，开始上传对应的 wav 音频
                uploadAudioToXunFei();
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

    private void uploadAudioToXunFei() {
        // 异步任务上传，避免阻塞UI线程
        new AsyncTask<Void, Void, String>() {
            @Override
            protected String doInBackground(Void... voids) {
                try {
                    // 构造对应的 wav 文件名
                    String wavFileName = videoName.replaceAll("\\.[^.]+$", ".wav");

                    // 读取 assets 中 wav 文件为 byte[]
                    InputStream is = getActivity().getAssets().open(wavFileName);
                    ByteArrayOutputStream baos = new ByteArrayOutputStream();
                    byte[] buffer = new byte[4096];
                    int len;
                    while ((len = is.read(buffer)) != -1) {
                        baos.write(buffer, 0, len);
                    }
                    is.close();
                    byte[] audioData = baos.toByteArray();

                    long fileSize = audioData.length;
                    long duration = 60; // 随便填一个数

                    long ts = System.currentTimeMillis() / 1000; // 当前时间戳秒级

                    String signa = generateSigna(APP_ID, SECRET_KEY, ts);

                    // 拼接上传URL
                    String urlStr = "https://raasr.xfyun.cn/v2/api/upload" +
                            "?duration=" + duration +
                            "&signa=" + signa +
                            "&fileName=" + wavFileName +
                            "&fileSize=" + fileSize +
                            "&appId=" + APP_ID +
                            "&ts=" + ts ;

                    // 建立 HttpURLConnection
                    URL url = new URL(urlStr);
                    HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                    conn.setRequestMethod("POST");
                    conn.setRequestProperty("Content-Type", "application/octet-stream");
                    conn.setDoOutput(true);

                    // 写入音频流
                    conn.getOutputStream().write(audioData);
                    conn.getOutputStream().flush();
                    conn.getOutputStream().close();

                    int responseCode = conn.getResponseCode();
                    InputStream responseStream = (responseCode == 200) ? conn.getInputStream() : conn.getErrorStream();

                    ByteArrayOutputStream responseBaos = new ByteArrayOutputStream();
                    while ((len = responseStream.read(buffer)) != -1) {
                        responseBaos.write(buffer, 0, len);
                    }
                    responseStream.close();

                    String responseStr = new String(responseBaos.toByteArray());
                    Log.d("XunFeiUpload", "Response: " + responseStr);

                    // 处理返回结果
                    if (responseCode == 200) {
                        ORDER_ID = responseStr.split("\"orderId\":\"")[1].split("\"")[0];
//                        Log.d("XunFeiUpload", "OrderId: " + ORDER_ID);
                    }
                    return responseStr;

                } catch (Exception e) {
                    e.printStackTrace();
                    return null;
                }
            }

            @Override
            protected void onPostExecute(String result) {
                if (result != null) {
                    Log.d("XunFeiUpload", "上传完成: " + result);
                    // 查询结果
//                    queryTranscriptionResult(ORDER_ID);
                } else {
                    Log.d("XunFeiUpload", "上传失败");
                }
            }
        }.execute();
    }
    // 生成 signa
    private String generateSigna(String appId, String secretKey, long ts) throws Exception {
        // 1. baseString = appid + ts
        String baseString = appId + ts;

        // 2. MD5(baseString)
        MessageDigest md5 = MessageDigest.getInstance("MD5");
        byte[] md5Bytes = md5.digest(baseString.getBytes("UTF-8"));
        // 关键：将MD5字节数组转换成32位小写的十六进制字符串,得到MD5结果
        StringBuilder md5Sb = new StringBuilder();
        for (byte b : md5Bytes) {
            // 1个byte转成2位十六进制（不足2位补0），必须是小写（%02x）
            md5Sb.append(String.format("%02x", b));
        }
        String md5Result = md5Sb.toString();

        // 3. HmacSHA1(MD5(baseString), secretKey)
        Mac mac = Mac.getInstance("HmacSHA1");
        SecretKeySpec keySpec = new SecretKeySpec(secretKey.getBytes("UTF-8"), "HmacSHA1");
        mac.init(keySpec);
        byte[] hmacBytes = mac.doFinal(md5Result.getBytes("UTF-8"));

        // 4. Base64 encode
        return Base64.encodeToString(hmacBytes, Base64.NO_WRAP);
    }


    // 查询转写结果
    private void queryTranscriptionResult(String orderId) {
        // 分梯度轮询：避免无限循环
        new Thread(() -> {
            int[] pollIntervals = {20000, 30000, 60000, 120000, 240000}; // 轮询间隔（毫秒）
            boolean isCompleted = false;

            for (int interval : pollIntervals) {
                if (isCompleted) break;

                try {
                    // 1. 生成当前时间戳和signa
                    long ts = System.currentTimeMillis() / 1000;
                    String signa = generateSigna(APP_ID, SECRET_KEY, ts);

                    // 2. 拼接查询URL（参数按文档要求顺序拼接，resultType按需修改）
                    String queryUrlStr = "https://raasr.xfyun.cn/v2/api/getResult" +
                            "?signa=" + signa +
                            "&orderId=" + orderId +
                            "&appId=" + APP_ID +
                            "&resultType=transfer" +
                            "&ts=" + ts ;

                    // 3. 发起GET请求
                    URL url = new URL(queryUrlStr);
                    HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                    conn.setRequestMethod("GET");
//                    conn.setConnectTimeout(5000);
//                    conn.setReadTimeout(10000);

                    int responseCode = conn.getResponseCode();
                    InputStream responseStream = (responseCode == 200) ? conn.getInputStream() : conn.getErrorStream();

                    // 4. 读取响应结果
                    ByteArrayOutputStream baos = new ByteArrayOutputStream();
                    byte[] buffer = new byte[4096];
                    int len;
                    while ((len = responseStream.read(buffer)) != -1) {
                        baos.write(buffer, 0, len);
                    }
                    responseStream.close();
                    conn.disconnect();

                    String result = new String(baos.toByteArray(), "UTF-8");
                    Log.d("XunFeiQuery", "查询结果: " + result);

                    // 5. 解析响应状态（核心：判断转写是否完成）
                    if (result.contains("\"code\":\"0\"")) { // code=0 表示成功（转写完成）
                        Log.d("XunFeiQuery", "转写成功！最终结果: " + result);
                        // TODO: 这里处理转写结果（解析JSON中的transcription字段）
                        parseTranscriptionResult(result);
                        isCompleted = true;
                    } else {
                        Log.e("XunFeiQuery", "转写失败！错误信息: " + result);
                        isCompleted = true;
                    }

                } catch (Exception e) {
                    e.printStackTrace();
                    try {
                        Thread.sleep(2000); // 异常时等待2秒再重试
                    } catch (InterruptedException ie) {
                        ie.printStackTrace();
                    }
                }
            }

            if (!isCompleted) {
                Log.e("XunFeiQuery", "轮询次数已达上限，未获取到转写结果");
            }
        }).start();
    }

    // 解析转写结果（JSON格式，需根据实际返回结构调整）
    private void parseTranscriptionResult(String resultJson) {
        // 示例返回格式（参考讯飞文档）：
        // {"code":"0","descInfo":"success","orderId":"xxx","transcription":"这是转写后的文本内容","status":1}
        try {
            // 方法1：用原生JSON解析（无需第三方库）
            org.json.JSONObject jsonObject = new org.json.JSONObject(resultJson);
            String transcription = jsonObject.getString("transcription"); // 核心转写文本
            String status = jsonObject.getString("status"); // 1=成功，0=失败

            // 切换到UI线程更新（比如显示到TextView）
            getActivity().runOnUiThread(() -> {
                Log.d("XunFeiQuery", "转写文本: " + transcription);
                // TODO: 这里将transcription显示到界面（例：textView.setText(transcription)）
            });

        } catch (org.json.JSONException e) {
            e.printStackTrace();
            Log.e("XunFeiQuery", "解析转写结果失败");
        }
    }
}
