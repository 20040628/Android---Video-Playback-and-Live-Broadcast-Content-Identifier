package com.bytedance.trainingcamp;

import android.util.Log;

import java.util.HashMap;
import java.util.Map;

public class SwearwordChecker {
    // 构建脏话词库
    private static final Map<String, Integer> SWEAR_WORDS = new HashMap<>();

    static {
        // 初始化脏话词库
        SWEAR_WORDS.put("操", 1);
        SWEAR_WORDS.put("卧槽", 1);
        SWEAR_WORDS.put("他妈", 1);
        SWEAR_WORDS.put("他妈的", 1);
        SWEAR_WORDS.put("狗日", 1);
        SWEAR_WORDS.put("狗日的", 1);
        SWEAR_WORDS.put("傻逼", 1);
        SWEAR_WORDS.put("傻逼真", 1);
        SWEAR_WORDS.put("脑残", 1);
        SWEAR_WORDS.put("废物", 1);
        SWEAR_WORDS.put("杂种", 1);
        SWEAR_WORDS.put("滚蛋", 1);
        SWEAR_WORDS.put("混蛋", 1);
        SWEAR_WORDS.put("卧槽", 1);
        SWEAR_WORDS.put("草泥马", 1);
        SWEAR_WORDS.put("cnm", 1);
        SWEAR_WORDS.put("nmb", 1);
        SWEAR_WORDS.put("sb", 1);
        // 英文脏话
        SWEAR_WORDS.put("fuck", 1);
        SWEAR_WORDS.put("fucking", 1);
        SWEAR_WORDS.put("fucks", 1);
        SWEAR_WORDS.put("shit", 1);
        SWEAR_WORDS.put("bitch", 1);
        SWEAR_WORDS.put("bitches", 1);
        SWEAR_WORDS.put("asshole", 1);
        SWEAR_WORDS.put("dick", 1);
        SWEAR_WORDS.put("pussy", 1);
    }

    /**
     * 检查文本中的脏话并统计总次数
     * @param text 待检测文本（转写后的 transcriptionText）
     * @return 脏话出现总次数（无脏话返回0）
     */
    public static int checkSwearwordCount(String text) {
        if (text == null || text.isEmpty()) {
            Log.d("SwearwordChecker", "待检测文本为空");
            return 0;
        }

        int totalCount = 0;
        // 转换为小写
        String lowerText = text.toLowerCase();

        // 遍历词库，匹配脏话并累加次数
        for (String swearWord : SWEAR_WORDS.keySet()) {
            String lowerSwearWord = swearWord.toLowerCase();
            int count = countOccurrences(lowerText, lowerSwearWord);
            if (count > 0) {
                Log.d("SwearwordChecker", "检测到脏话：" + swearWord + "，出现次数：" + count);
                totalCount += count;
            }
        }

        return totalCount;
    }

    /**
     * 统计某个字符串在文本中出现的次数
     * @param text 原文
     * @param target 待统计的字符串
     * @return 出现次数
     */
    private static int countOccurrences(String text, String target) {
        if (target.length() == 0) return 0;

        int count = 0;
        int index = 0;
        // 循环查找，每次找到后从下一个位置继续（避免重叠匹配）
        while ((index = text.indexOf(target, index)) != -1) {
            count++;
            index += target.length(); // 跳过当前匹配的字符串，避免重复统计
        }
        return count;
    }

}
