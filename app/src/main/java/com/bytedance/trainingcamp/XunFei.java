package com.bytedance.trainingcamp;

import android.content.Context;
import android.os.AsyncTask;
import android.util.Base64;
import android.util.Log;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.security.MessageDigest;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;

// 定义回调接口：用于将脏话数量传递给VideoFragment

public class XunFei {
    private static final String APP_ID = "7fee0d4b";
    private static final String SECRET_KEY = "b32c4692b7c325539a0daf6f551bad6d";
    private static String ORDER_ID = "";

    private Context context;
    private String videoName;

    public XunFei(Context context, String videoName) {
        this.context = context;
        this.videoName = videoName;
    }

    public void uploadAudio() {
        new AsyncTask<Void, Void, String>() {
            @Override
            protected String doInBackground(Void... voids) {
                try {
                    // wav 文件名
                    String wavFileName = videoName.replaceAll("\\.[^.]+$", ".wav");
                    // 读取 assets 中 wav 文件为 byte[]
                    InputStream is =  context.getAssets().open(wavFileName);
                    ByteArrayOutputStream baos = new ByteArrayOutputStream();
                    byte[] buffer = new byte[4096];
                    int len;
                    while ((len = is.read(buffer)) != -1) {
                        baos.write(buffer, 0, len);
                    }
                    is.close();
                    byte[] audioData = baos.toByteArray();

                    long fileSize = audioData.length;
                    long duration = 60;

                    long ts = System.currentTimeMillis() / 1000;

                    String signa = generateSigna(APP_ID, SECRET_KEY, ts);
                    String encodedSigna = URLEncoder.encode(signa, "UTF-8");

                    // 拼接上传URL
                    String urlStr = "https://raasr.xfyun.cn/v2/api/upload" +
                            "?duration=" + duration +
                            "&signa=" + encodedSigna +
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
                    Log.d("XunFei", "Response: " + responseStr);

                    // 处理返回结果
                    if (responseCode == 200) {
                        ORDER_ID = responseStr.split("\"orderId\":\"")[1].split("\"")[0];
                    }
                    return responseStr;
                } catch (Exception e) {
                    e.printStackTrace();
                    return null;
                }
            }

            @Override
            protected void onPostExecute(String result) {
                if (result != null)  {
                    Log.d("XunFei", "上传完成: " + result);
                    if (!ORDER_ID.isEmpty()) {
                        Log.d("XunFei", "开始查询转写结果，orderId: " + ORDER_ID);
                        queryTranscriptionResult(ORDER_ID);
                    } else {
                        Log.e("XunFei", "未获取到有效orderId，查询终止");
                    }
                } else {
                    Log.d("XunFei", "上传失败");
                }
            }
        }.execute();
    }

    // 生成 signa
    private String generateSigna(String appId, String secretKey, long ts) throws Exception {
        // 1. baseString = appid + ts
        String baseString = appId + ts;
        // 2. md5(baseString)
        MessageDigest md5 = MessageDigest.getInstance("MD5");
        byte[] md5Bytes = md5.digest(baseString.getBytes("UTF-8"));
        StringBuilder md5Sb = new StringBuilder();
        for (byte b : md5Bytes) md5Sb.append(String.format("%02x", b));
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
    public void queryTranscriptionResult(String orderId) {
        new Thread(() -> {
            int pollCount = 0;
            int maxPollTimes = 30; // 30次 × 30秒 = 900秒（15分钟），足够覆盖1分钟转写+排队
            boolean isCompleted = false;

            while (pollCount < maxPollTimes && !isCompleted) {
                pollCount++;
                try {
                    // 每次查询重新生成ts和signa
                    long ts = System.currentTimeMillis() / 1000;
                    String signa = generateSigna(APP_ID, SECRET_KEY, ts);
                    String encodedSigna = URLEncoder.encode(signa, "UTF-8");

                    // 拼接查询URL
                    String queryUrlStr = "https://raasr.xfyun.cn/v2/api/getResult" +
                            "?appId=" + APP_ID +
                            "&ts=" + ts +
                            "&signa=" + encodedSigna +
                            "&orderId=" + orderId +
                            "&resultType=transfer";

                    // 发起GET请求
                    URL url = new URL(queryUrlStr);
                    HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                    conn.setRequestMethod("GET");
                    conn.setConnectTimeout(10000);
                    conn.setReadTimeout(10000);

                    int responseCode = conn.getResponseCode();
                    InputStream responseStream = (responseCode == 200) ? conn.getInputStream() : conn.getErrorStream();

                    // 读取完整JSON响应
                    ByteArrayOutputStream baos = new ByteArrayOutputStream();
                    byte[] buffer = new byte[4096];
                    int len;
                    while ((len = responseStream.read(buffer)) != -1) {
                        baos.write(buffer, 0, len);
                    }
                    responseStream.close();
                    conn.disconnect();

                    // 直接打印JSON结果
                    String resultJson = new String(baos.toByteArray(), "UTF-8");
                    Log.d("XunFeiQuery", "第" + pollCount + "次查询，响应JSON: " + resultJson);

                    // 解析 status 字段，判断是否转写完成。转写成功（orderInfo.status==4）或转写失败（orderInfo.status==-1）
                    int status = -1;
                    String orderResultStr = "";
                    try {
                        // 1. 解析外层JSON
                        org.json.JSONObject outerJson = new org.json.JSONObject(resultJson);
                        if ("000000".equals(outerJson.getString("code"))) {
                            org.json.JSONObject contentJson = outerJson.getJSONObject("content");
                            // 提取 orderInfo.status
                            org.json.JSONObject orderInfoJson = contentJson.getJSONObject("orderInfo");
                            status = orderInfoJson.getInt("status");
                            // 提取 orderResult 嵌套JSON字符串
                            orderResultStr = contentJson.getString("orderResult");
                        }
                    } catch (org.json.JSONException e) {
                        Log.e("XunFeiQuery", "解析外层JSON失败", e);
                    }

                    // 终止条件：status=4（转写完成）
                    if (status == 4) {
                        Log.d("XunFeiQuery", "转写成功！终止轮询");
                        isCompleted = true;

                        // 解析orderResult获取最终文本
                        try {
                            // 1. 解析内层orderResult（嵌套JSON字符串）
                            org.json.JSONObject innerJson = new org.json.JSONObject(orderResultStr);
                            org.json.JSONArray latticeArray = innerJson.getJSONArray("lattice");

                            StringBuilder finalText = new StringBuilder(); // 存储最终文本

                            // 2. 遍历每层lattice，提取json_1best中的文本
                            for (int i = 0; i < latticeArray.length(); i++) {
                                org.json.JSONObject latticeObj = latticeArray.getJSONObject(i);
                                String json1best = latticeObj.getString("json_1best");

                                // 3. 解析json_1best（再次嵌套的JSON字符串）
                                org.json.JSONObject json1bestObj = new org.json.JSONObject(json1best);
                                org.json.JSONObject stObj = json1bestObj.getJSONObject("st");
                                org.json.JSONArray rtArray = stObj.getJSONArray("rt");

                                // 4. 提取每个词（ws→cw→w）
                                for (int j = 0; j < rtArray.length(); j++) {
                                    org.json.JSONObject rtObj = rtArray.getJSONObject(j);
                                    org.json.JSONArray wsArray = rtObj.getJSONArray("ws");

                                    for (int k = 0; k < wsArray.length(); k++) {
                                        org.json.JSONObject wsObj = wsArray.getJSONObject(k);
                                        org.json.JSONArray cwArray = wsObj.getJSONArray("cw");

                                        for (int m = 0; m < cwArray.length(); m++) {
                                            org.json.JSONObject cwObj = cwArray.getJSONObject(m);
                                            String word = cwObj.getString("w"); // 核心文本
                                            finalText.append(word); // 拼接文本
                                        }
                                    }
                                }
                            }

                            // 最终文本结果
                            String transcriptionText = finalText.toString().trim();
                            Log.d("XunFeiQuery", "WAV对应最终文本：" + transcriptionText);

                            // 调用脏话检测工具
                            int swearwordCount = SwearwordChecker.checkSwearwordCount(transcriptionText);
                            Log.d("XunFeiQuery", "脏话出现总次数：" + swearwordCount);
                            // （可选）如果需要在UI显示，切换到主线程
                            // ((Activity) context).runOnUiThread(() -> {
                            //     // 例如：textView.setText(transcriptionText);
                            // });

                        } catch (org.json.JSONException e) {
                            Log.e("XunFeiQuery", "解析orderResult获取文本失败", e);
                        }
                    } else if (pollCount < maxPollTimes) {
                        // 未完成且未到最大次数，等待30秒后继续
                        Log.d("XunFeiQuery", "转写中，30秒后进行第" + (pollCount + 1) + "次查询");
                        Thread.sleep(30000);
                    }

                } catch (Exception e) {
                    Log.e("XunFeiQuery", "第" + pollCount + "次查询异常", e);
                    try {
                        Thread.sleep(30000);
                    } catch (InterruptedException ie) {
                        ie.printStackTrace();
                    }
                }
            }

            if (!isCompleted) {
                Log.e("XunFeiQuery", "已轮询15分钟（30次），未获取最终结果，终止查询");
            }
        }).start();
    }
}
