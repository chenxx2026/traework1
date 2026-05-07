package com.example.service;

import cn.hutool.http.HttpRequest;
import cn.hutool.http.HttpResponse;
import cn.hutool.json.JSONArray;
import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;
import com.example.common.config.DuobaoConfig;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

@Service
public class DuobaoService {

    @Resource
    private DuobaoConfig duobaoConfig;

    // 简单的节流机制：记录最后调用时间，防止过于频繁的调用
    private long lastCallTime = 0;
    private static final long MIN_CALL_INTERVAL = TimeUnit.SECONDS.toMillis(1);
    
    // HTTP配置
    private static final int TIMEOUT = 120000; // 增加到120秒
    private static final int MAX_RETRIES = 2; // 最大重试次数

    public String chat(String userMessage, List<Map<String, String>> history) {
        // 节流检查
        long currentTime = System.currentTimeMillis();
        if (currentTime - lastCallTime < MIN_CALL_INTERVAL) {
            try {
                Thread.sleep(MIN_CALL_INTERVAL - (currentTime - lastCallTime));
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }
        lastCallTime = System.currentTimeMillis();

        // 构建请求体
        JSONObject requestBody = new JSONObject();
        requestBody.set("model", duobaoConfig.getModelName());

        JSONArray messages = new JSONArray();

        // 添加系统提示词（更简洁，减少响应时间）
        Map<String, String> systemMessage = new HashMap<>();
        systemMessage.put("role", "system");
        systemMessage.put("content", "你是一个专业的旅游路线规划助手。请根据用户需求，提供简洁实用的旅游路线建议。");
        messages.add(systemMessage);

        // 添加历史对话
        if (history != null) {
            for (Map<String, String> msg : history) {
                Map<String, String> historyMsg = new HashMap<>();
                historyMsg.put("role", msg.get("role"));
                historyMsg.put("content", msg.get("content"));
                messages.add(historyMsg);
            }
        }

        // 添加当前用户消息
        Map<String, String> userMsg = new HashMap<>();
        userMsg.put("role", "user");
        userMsg.put("content", userMessage);
        messages.add(userMsg);

        requestBody.set("messages", messages);

        // 带重试机制的HTTP请求
        Exception lastException = null;
        for (int attempt = 0; attempt <= MAX_RETRIES; attempt++) {
            try {
                if (attempt > 0) {
                    // 重试前等待
                    Thread.sleep(2000);
                    System.out.println("豆包API调用重试，第" + (attempt + 1) + "次尝试");
                }

                // 发送HTTP请求
                HttpResponse httpResponse = HttpRequest.post(duobaoConfig.getApiUrl())
                        .header("Content-Type", "application/json")
                        .header("Authorization", "Bearer " + duobaoConfig.getApiKey())
                        .body(requestBody.toString())
                        .timeout(TIMEOUT)
                        .execute();

                String responseBody = httpResponse.body();

                // 检查HTTP状态码
                if (!httpResponse.isOk()) {
                    System.out.println("豆包API返回错误状态码: " + httpResponse.getStatus());
                    System.out.println("响应内容: " + responseBody);
                    throw new RuntimeException("API返回错误: " + httpResponse.getStatus());
                }

                // 解析响应
                JSONObject jsonResponse = JSONUtil.parseObj(responseBody);
                JSONArray choices = jsonResponse.getJSONArray("choices");
                if (choices != null && choices.size() > 0) {
                    JSONObject choice = choices.getJSONObject(0);
                    JSONObject message = choice.getJSONObject("message");
                    if (message != null) {
                        return message.getStr("content");
                    }
                }

                return "抱歉，未能生成路线建议。请稍后重试。";

            } catch (Exception e) {
                lastException = e;
                System.err.println("豆包API调用失败（尝试 " + (attempt + 1) + "）: " + e.getMessage());
                e.printStackTrace();
                
                // 如果是最后一次尝试，不再重试
                if (attempt == MAX_RETRIES) {
                    break;
                }
            }
        }

        // 所有重试都失败，返回友好的错误提示
        return getFriendlyErrorMessage(lastException);
    }

    /**
     * 获取友好的错误提示信息
     */
    private String getFriendlyErrorMessage(Exception e) {
        if (e == null) {
            return "抱歉，服务暂时不可用，请稍后重试。";
        }
        
        String message = e.getMessage();
        if (message != null) {
            if (message.contains("Read timed out") || message.contains("timeout")) {
                return "抱歉，AI助手响应超时了。请稍后再试，或者尝试简化您的问题。";
            }
            if (message.contains("Connection") || message.contains("connect")) {
                return "抱歉，无法连接到AI服务。请检查网络连接后重试。";
            }
            if (message.contains("401") || message.contains("403")) {
                return "抱歉，服务认证失败，请联系管理员。";
            }
            if (message.contains("429")) {
                return "抱歉，请求过于频繁，请稍后再试。";
            }
        }
        
        return "抱歉，AI助手暂时无法为您服务。请稍后重试，或者尝试其他方式规划路线。";
    }

    public String simpleChat(String userMessage) {
        return chat(userMessage, new ArrayList<>());
    }
}
