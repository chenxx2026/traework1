package com.example.service;

import cn.hutool.core.util.StrUtil;
import cn.hutool.json.JSONArray;
import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;
import com.example.common.config.TrainTicketConfig;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URI;
import java.net.URL;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

@Service
public class TrainTicketService {

    @Resource
    private TrainTicketConfig trainTicketConfig;

    private static final int TIMEOUT = 15000;
    private static final int MAX_RETRIES = 2;

    private long lastCallTime = 0;
    private static final long MIN_CALL_INTERVAL = TimeUnit.MILLISECONDS.toMillis(500);

    private final Map<String, CacheEntry> stationCache = new ConcurrentHashMap<>();
    private final Map<String, CacheEntry> ticketCache = new ConcurrentHashMap<>();
    private static final long STATION_CACHE_TTL = TimeUnit.HOURS.toMillis(24);
    private static final long TICKET_CACHE_TTL = TimeUnit.MINUTES.toMillis(5);

    private static class CacheEntry {
        final String data;
        final long expireTime;

        CacheEntry(String data, long expireTime) {
            this.data = data;
            this.expireTime = expireTime;
        }

        boolean isExpired() {
            return System.currentTimeMillis() > expireTime;
        }
    }

    public String searchStation(String stationName) {
        if (StrUtil.isBlank(stationName)) {
            return JSONUtil.toJsonStr(Map.of("error", "车站名称不能为空"));
        }

        String cacheKey = "station_" + stationName;
        CacheEntry cached = stationCache.get(cacheKey);
        if (cached != null && !cached.isExpired()) {
            return cached.data;
        }

        throttleCall();

        String apiPath = trainTicketConfig.getStationApiPath();
        String queryStr = "station=" + encode(stationName);
        String result = executeGetRequest(apiPath, queryStr, "车站查询");
        if (result != null) {
            stationCache.put(cacheKey, new CacheEntry(result, System.currentTimeMillis() + STATION_CACHE_TTL));
        }
        return result;
    }

    public String queryTickets(String fromStation, String toStation, String date, String trainType) {
        if (StrUtil.isBlank(fromStation) || StrUtil.isBlank(toStation) || StrUtil.isBlank(date)) {
            return JSONUtil.toJsonStr(Map.of("error", "出发地、目的地和日期不能为空"));
        }

        String ishigh = "0";
        if ("G/D".equals(trainType)) {
            ishigh = "1";
        }

        String cacheKey = "ticket_" + fromStation + "_" + toStation + "_" + date + "_" + ishigh;
        CacheEntry cached = ticketCache.get(cacheKey);
        if (cached != null && !cached.isExpired()) {
            return cached.data;
        }

        throttleCall();

        String apiPath = trainTicketConfig.getStationApiPath();
        String queryStr = "start=" + encode(fromStation) + "&end=" + encode(toStation)
                + "&date=" + encode(date) + "&ishigh=" + ishigh;
        String result = executeGetRequest(apiPath, queryStr, "车票查询");
        if (result != null) {
            ticketCache.put(cacheKey, new CacheEntry(result, System.currentTimeMillis() + TICKET_CACHE_TTL));
        }
        return result;
    }

    private void throttleCall() {
        long currentTime = System.currentTimeMillis();
        if (currentTime - lastCallTime < MIN_CALL_INTERVAL) {
            try {
                Thread.sleep(MIN_CALL_INTERVAL - (currentTime - lastCallTime));
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }
        lastCallTime = System.currentTimeMillis();
    }

    private String executeGetRequest(String apiPath, String queryStr, String operationName) {
        Exception lastException = null;

        for (int attempt = 0; attempt <= MAX_RETRIES; attempt++) {
            HttpURLConnection connection = null;
            try {
                if (attempt > 0) {
                    Thread.sleep(2000);
                    System.out.println("火车票API调用重试，第" + (attempt + 1) + "次尝试，操作：" + operationName);
                }

                String fullUrl = trainTicketConfig.getApiHost() + apiPath + "?" + queryStr;
                System.out.println("调用火车票API: " + fullUrl);

                URL url = URI.create(fullUrl).toURL();
                connection = (HttpURLConnection) url.openConnection();
                connection.setRequestMethod("GET");
                connection.setConnectTimeout(TIMEOUT);
                connection.setReadTimeout(TIMEOUT);
                connection.setRequestProperty("Authorization", "APPCODE " + trainTicketConfig.getAppCode());
                connection.setRequestProperty("Content-Type", "application/json; charset=UTF-8");

                int statusCode = connection.getResponseCode();
                System.out.println("火车票API响应状态码: " + statusCode);

                if (statusCode == 200) {
                    StringBuilder response = new StringBuilder();
                    try (BufferedReader reader = new BufferedReader(
                            new InputStreamReader(connection.getInputStream(), StandardCharsets.UTF_8))) {
                        String line;
                        while ((line = reader.readLine()) != null) {
                            response.append(line);
                        }
                    }
                    return response.toString();
                }

                String errorBody = "";
                try (BufferedReader reader = new BufferedReader(
                        new InputStreamReader(connection.getErrorStream(), StandardCharsets.UTF_8))) {
                    StringBuilder eb = new StringBuilder();
                    String line;
                    while ((line = reader.readLine()) != null) {
                        eb.append(line);
                    }
                    errorBody = eb.toString();
                } catch (Exception ignored) {
                }

                System.err.println("火车票API返回非200状态码: " + statusCode + ", 响应: " + errorBody);

                if (statusCode == 403 || statusCode == 401) {
                    JSONObject errorResult = new JSONObject();
                    errorResult.set("error", "API认证失败，请检查AppCode配置");
                    return errorResult.toString();
                }

                if (statusCode == 429) {
                    JSONObject errorResult = new JSONObject();
                    errorResult.set("error", "请求过于频繁，请稍后再试");
                    return errorResult.toString();
                }

                if (statusCode >= 400 && attempt < MAX_RETRIES) {
                    throw new RuntimeException("API返回错误状态码: " + statusCode);
                }

                JSONObject errorResult = new JSONObject();
                errorResult.set("error", "API返回错误(" + statusCode + ")，请稍后再试");
                return errorResult.toString();

            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                lastException = e;
                break;
            } catch (Exception e) {
                lastException = e;
                String errMsg = e.getMessage();
                System.err.println("火车票API调用失败（尝试 " + (attempt + 1) + "）: " + errMsg);

                if (errMsg != null && (errMsg.contains("UnknownHost") || errMsg.contains("unresolved"))) {
                    JSONObject errorResult = new JSONObject();
                    errorResult.set("error", "无法连接到API服务（DNS解析失败），请检查网络连接或代理设置");
                    return errorResult.toString();
                }

                if (attempt == MAX_RETRIES) {
                    break;
                }
            } finally {
                if (connection != null) {
                    try {
                        connection.disconnect();
                    } catch (Exception ignored) {
                    }
                }
            }
        }

        JSONObject errorResult = new JSONObject();
        errorResult.set("error", buildErrorMessage(lastException, operationName));
        return errorResult.toString();
    }

    private String buildErrorMessage(Exception e, String operationName) {
        if (e == null) {
            return operationName + "失败，服务暂时不可用";
        }
        String message = e.getMessage();
        if (message != null) {
            if (message.contains("timeout") || message.contains("Read timed out") || message.contains("timed out")) {
                return operationName + "超时，请稍后再试";
            }
            if (message.contains("Connection") || message.contains("connect") || message.contains("UnknownHost")) {
                return "网络连接失败，请检查网络连接或代理设置";
            }
        }
        return operationName + "失败：服务暂时不可用，请稍后再试";
    }

    private String encode(String value) {
        try {
            return URLEncoder.encode(value, StandardCharsets.UTF_8);
        } catch (Exception e) {
            return value;
        }
    }

    public String parseTicketResponse(String apiResponse) {
        if (StrUtil.isBlank(apiResponse)) {
            return JSONUtil.toJsonStr(List.of());
        }

        System.out.println("车票API原始响应: " + apiResponse);

        try {
            JSONObject response = JSONUtil.parseObj(apiResponse);

            if (response.containsKey("error")) {
                return apiResponse;
            }

            String listKey = null;
            for (String key : response.keySet()) {
                if (key.toLowerCase(Locale.ROOT).contains("list")) {
                    listKey = key;
                    break;
                }
            }
            if (listKey != null) {
                JSONArray arr = response.getJSONArray(listKey);
                if (arr != null && !arr.isEmpty()) {
                    System.out.println("从根节点直接提取列表，key=" + listKey + ", 条数=" + arr.size());
                    return arr.toString();
                }
            }

            JSONArray resultList = findTrainList(response);
            if (resultList != null && !resultList.isEmpty()) {
                System.out.println("提取到车次列表，条数=" + resultList.size());
                return resultList.toString();
            }

            System.out.println("未能从响应中提取车次列表，返回空数组");
            return JSONUtil.toJsonStr(List.of());

        } catch (Exception e) {
            System.err.println("解析车票API响应失败: " + e.getMessage());
            e.printStackTrace();
            return JSONUtil.toJsonStr(List.of());
        }
    }

    private JSONArray findTrainList(JSONObject node) {
        if (node == null) {
            return null;
        }

        for (String key : node.keySet()) {
            Object value = node.get(key);
            if (value instanceof JSONArray) {
                JSONArray arr = (JSONArray) value;
                if (!arr.isEmpty()) {
                    JSONObject first = arr.getJSONObject(0);
                    if (first != null && (first.containsKey("trainno") || first.containsKey("train_no")
                            || first.containsKey("departuretime") || first.containsKey("departure_time")
                            || first.containsKey("station"))) {
                        System.out.println("递归找到车次列表，key=" + key + ", 条数=" + arr.size());
                        return arr;
                    }
                }
            } else if (value instanceof JSONObject) {
                JSONArray found = findTrainList((JSONObject) value);
                if (found != null) {
                    return found;
                }
            }
        }
        return null;
    }
}
