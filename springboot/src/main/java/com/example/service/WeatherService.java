package com.example.service;

import cn.hutool.core.util.StrUtil;
import cn.hutool.json.JSONArray;
import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;
import com.example.common.config.WeatherConfig;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URI;
import java.net.URL;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

@Service
public class WeatherService {

    @Resource
    private WeatherConfig weatherConfig;

    private static final int TIMEOUT = 10000;
    private static final int MAX_RETRIES = 1;

    private final Map<String, CacheEntry> weatherCache = new ConcurrentHashMap<>();
    private static final long CACHE_TTL = TimeUnit.MINUTES.toMillis(30);

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

    public List<Map<String, Object>> queryWeather(String city, String startDate, String endDate) {
        if (StrUtil.isBlank(city)) {
            return new ArrayList<>();
        }

        String cacheKey = city;
        CacheEntry cached = weatherCache.get(cacheKey);
        if (cached != null && !cached.isExpired()) {
            List<Map<String, Object>> cachedResult = parseCachedData(cached.data, startDate, endDate);
            if (cachedResult != null) {
                return cachedResult;
            }
        }

        List<Map<String, Object>> result;
        if (StrUtil.isNotBlank(weatherConfig.getAppid()) && StrUtil.isNotBlank(weatherConfig.getAppsecret())) {
            result = queryFromTianqiApi(city);
        } else {
            result = queryFromFallbackApi(city);
        }

        if (result != null && !result.isEmpty()) {
            weatherCache.put(cacheKey, new CacheEntry(JSONUtil.toJsonStr(result),
                    System.currentTimeMillis() + CACHE_TTL));
        }

        return filterByDateRange(result, startDate, endDate);
    }

    private List<Map<String, Object>> queryFromTianqiApi(String city) {
        try {
            String apiUrl = weatherConfig.getApiUrl();
            String queryStr = "version=v9&appid=" + encode(weatherConfig.getAppid())
                    + "&appsecret=" + encode(weatherConfig.getAppsecret())
                    + "&city=" + encode(city);

            String fullUrl = apiUrl + "?" + queryStr;
            System.out.println("调用天启天气API: " + fullUrl);

            URL url = URI.create(fullUrl).toURL();
            HttpURLConnection connection = (HttpURLConnection) url.openConnection();
            connection.setRequestMethod("GET");
            connection.setConnectTimeout(TIMEOUT);
            connection.setReadTimeout(TIMEOUT);

            int statusCode = connection.getResponseCode();
            System.out.println("天启天气API响应状态码: " + statusCode);

            if (statusCode == 200) {
                StringBuilder response = new StringBuilder();
                try (BufferedReader reader = new BufferedReader(
                        new InputStreamReader(connection.getInputStream(), StandardCharsets.UTF_8))) {
                    String line;
                    while ((line = reader.readLine()) != null) {
                        response.append(line);
                    }
                }
                System.out.println("天启天气API原始响应: " + response);
                return parseTianqiResponse(response.toString());
            }
        } catch (Exception e) {
            System.out.println("天启天气API调用失败: " + e.getMessage() + "，尝试使用备用API");
            return queryFromFallbackApi(city);
        }
        return new ArrayList<>();
    }

    private List<Map<String, Object>> queryFromFallbackApi(String city) {
        try {
            String apiUrl = weatherConfig.getFallbackApiUrl();
            String fullUrl = apiUrl + encode(city) + "?format=j1";
            System.out.println("调用备用天气API: " + fullUrl);

            URL url = URI.create(fullUrl).toURL();
            HttpURLConnection connection = (HttpURLConnection) url.openConnection();
            connection.setRequestMethod("GET");
            connection.setConnectTimeout(TIMEOUT);
            connection.setReadTimeout(TIMEOUT);

            int statusCode = connection.getResponseCode();
            if (statusCode == 200) {
                StringBuilder response = new StringBuilder();
                try (BufferedReader reader = new BufferedReader(
                        new InputStreamReader(connection.getInputStream(), StandardCharsets.UTF_8))) {
                    String line;
                    while ((line = reader.readLine()) != null) {
                        response.append(line);
                    }
                }
                return parseWttrResponse(response.toString());
            }
        } catch (Exception e) {
            System.out.println("备用天气API调用失败: " + e.getMessage());
        }
        return new ArrayList<>();
    }

    private List<Map<String, Object>> parseTianqiResponse(String jsonStr) {
        List<Map<String, Object>> result = new ArrayList<>();
        try {
            JSONObject response = JSONUtil.parseObj(jsonStr);
            JSONArray dataList = response.getJSONArray("data");
            if (dataList != null) {
                for (int i = 0; i < dataList.size(); i++) {
                    JSONObject dayData = dataList.getJSONObject(i);
                    Map<String, Object> day = new LinkedHashMap<>();
                    day.put("date", dayData.getStr("date"));
                    day.put("dayWeather", dayData.getStr("wea_day"));
                    day.put("nightWeather", dayData.getStr("wea_night"));
                    day.put("highTemp", dayData.getStr("tem1"));
                    day.put("lowTemp", dayData.getStr("tem2"));
                    day.put("wind", dayData.getStr("win"));
                    day.put("windSpeed", dayData.getStr("win_speed"));
                    day.put("airQuality", dayData.getStr("air"));
                    day.put("humidity", dayData.getStr("humidity"));
                    result.add(day);
                }
            }
        } catch (Exception e) {
            System.out.println("解析天启天气响应失败: " + e.getMessage());
        }
        return result;
    }

    private List<Map<String, Object>> parseWttrResponse(String jsonStr) {
        List<Map<String, Object>> result = new ArrayList<>();
        try {
            JSONObject response = JSONUtil.parseObj(jsonStr);
            JSONArray weatherList = response.getJSONArray("weather");
            if (weatherList != null) {
                for (int i = 0; i < weatherList.size() && i < 7; i++) {
                    JSONObject dayData = weatherList.getJSONObject(i);
                    Map<String, Object> day = new LinkedHashMap<>();
                    day.put("date", dayData.getStr("date"));
                    day.put("highTemp", dayData.getStr("maxtempC") + "℃");
                    day.put("lowTemp", dayData.getStr("mintempC") + "℃");

                    JSONArray hourly = dayData.getJSONArray("hourly");
                    if (hourly != null && !hourly.isEmpty()) {
                        JSONObject midDay = hourly.getJSONObject(Math.min(hourly.size() / 2, hourly.size() - 1));
                        JSONArray weatherDesc = midDay.getJSONArray("weatherDesc");
                        if (weatherDesc != null && !weatherDesc.isEmpty()) {
                            day.put("dayWeather", weatherDesc.getJSONObject(0).getStr("value"));
                        }
                        day.put("windSpeed", midDay.getStr("windspeedKmph") + "km/h");
                        day.put("humidity", midDay.getStr("humidity") + "%");
                    }

                    if (!day.containsKey("dayWeather")) {
                        day.put("dayWeather", "--");
                    }
                    day.put("dayWeather", translateWeather((String) day.get("dayWeather")));
                    day.put("nightWeather", translateWeather((String) day.get("dayWeather")));
                    day.put("wind", "--");
                    day.put("airQuality", "--");
                    result.add(day);
                }
            }
        } catch (Exception e) {
            System.out.println("解析备用天气响应失败: " + e.getMessage());
        }
        return result;
    }

    private String translateWeather(String weather) {
        if (StrUtil.isBlank(weather) || "--".equals(weather)) {
            return "--";
        }
        String w = weather.toLowerCase();
        if (w.contains("sunny") || w.contains("clear")) return "晴";
        if (w.contains("partly cloudy")) return "多云";
        if (w.contains("cloudy") || w.contains("overcast")) return "阴";
        if (w.contains("light rain") || w.contains("drizzle") || w.contains("patchy rain")) return "小雨";
        if (w.contains("moderate rain") || w.contains("rain")) return "雨";
        if (w.contains("heavy rain") || w.contains("torrential")) return "大雨";
        if (w.contains("thunder") || w.contains("lightning")) return "雷阵雨";
        if (w.contains("snow") || w.contains("sleet") || w.contains("ice")) return "雪";
        if (w.contains("fog") || w.contains("mist") || w.contains("haze")) return "雾";
        if (w.contains("wind")) return "风";
        return weather;
    }

    private List<Map<String, Object>> filterByDateRange(List<Map<String, Object>> weatherData,
                                                         String startDate, String endDate) {
        if (weatherData == null || weatherData.isEmpty()) {
            return weatherData;
        }
        if (StrUtil.isBlank(startDate) || StrUtil.isBlank(endDate)) {
            return weatherData;
        }

        try {
            DateTimeFormatter fmt = DateTimeFormatter.ofPattern("yyyy-MM-dd");
            LocalDate start = LocalDate.parse(startDate, fmt);
            LocalDate end = LocalDate.parse(endDate, fmt);

            List<Map<String, Object>> filtered = new ArrayList<>();
            for (Map<String, Object> day : weatherData) {
                String dateStr = (String) day.get("date");
                if (StrUtil.isNotBlank(dateStr)) {
                    LocalDate dayDate = LocalDate.parse(dateStr, fmt);
                    if (!dayDate.isBefore(start) && !dayDate.isAfter(end)) {
                        filtered.add(day);
                    }
                }
            }
            return filtered;
        } catch (Exception e) {
            return weatherData;
        }
    }

    private List<Map<String, Object>> parseCachedData(String cachedJson, String startDate, String endDate) {
        try {
            JSONArray arr = JSONUtil.parseArray(cachedJson);
            List<Map<String, Object>> result = new ArrayList<>();
            for (int i = 0; i < arr.size(); i++) {
                result.add(new LinkedHashMap<>(arr.getJSONObject(i)));
            }
            return filterByDateRange(result, startDate, endDate);
        } catch (Exception e) {
            return null;
        }
    }

    private String encode(String value) {
        try {
            return URLEncoder.encode(value, StandardCharsets.UTF_8);
        } catch (Exception e) {
            return value;
        }
    }
}
