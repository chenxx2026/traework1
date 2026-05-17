package com.example.service;

import cn.hutool.core.util.StrUtil;
import cn.hutool.json.JSONArray;
import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;
import com.example.common.config.DeepseekConfig;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.net.HttpURLConnection;
import java.net.URI;
import java.net.URL;
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
public class DeepseekService {

    @Resource
    private DeepseekConfig deepseekConfig;

    private static final int TIMEOUT = 60000;

    private long lastCallTime = 0;
    private static final long MIN_CALL_INTERVAL = TimeUnit.SECONDS.toMillis(2);

    private final Map<String, CacheEntry> flightCache = new ConcurrentHashMap<>();
    private final Map<String, CacheEntry> busCache = new ConcurrentHashMap<>();
    private static final long CACHE_TTL = TimeUnit.MINUTES.toMillis(60);

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

    public String queryFlight(String fromCity, String toCity, String date) {
        String cacheKey = fromCity + "_" + toCity + "_" + date;
        CacheEntry cached = flightCache.get(cacheKey);
        if (cached != null && !cached.isExpired()) {
            return cached.data;
        }

        String prompt = buildFlightPrompt(fromCity, toCity, date);
        String result = callDeepSeekAPI(prompt);
        if (result != null) {
            flightCache.put(cacheKey, new CacheEntry(result, System.currentTimeMillis() + CACHE_TTL));
        }
        return result;
    }

    public String queryBus(String fromCity, String toCity, String date) {
        String cacheKey = fromCity + "_" + toCity + "_" + date;
        CacheEntry cached = busCache.get(cacheKey);
        if (cached != null && !cached.isExpired()) {
            return cached.data;
        }

        String prompt = buildBusPrompt(fromCity, toCity, date);
        String result = callDeepSeekAPI(prompt);
        if (result != null) {
            busCache.put(cacheKey, new CacheEntry(result, System.currentTimeMillis() + CACHE_TTL));
        }
        return result;
    }

    private String buildFlightPrompt(String fromCity, String toCity, String date) {
        return "请根据" + fromCity + "到" + toCity + "的主要航线情况，在" + date + "这天，" +
               "生成4个合理的航班信息。要求如下：\n" +
               "1. 航班号和航空公司要符合中国民航实际情况（如CA、MU、CZ、HU、3U、MF等真实航空公司代码）\n" +
               "2. 出发和到达机场使用真实存在的机场名称\n" +
               "3. 飞行时间要符合航线的实际距离（如北京-上海约2.5小时，北京-广州约3.5小时）\n" +
               "4. 机票价格要符合市场行情（经济舱参考价）\n\n" +
               "请严格按照以下JSON格式返回（只返回JSON，不要任何其他文字）：\n" +
               "[\n" +
               "  {\"flightNo\":\"航班号\",\"airline\":\"航空公司\",\"fromAirport\":\"出发机场\",\"toAirport\":\"到达机场\"," +
               "\"departureTime\":\"出发时间(如08:30)\",\"arrivalTime\":\"到达时间\",\"duration\":\"飞行时长(如2小时30分)\"," +
               "\"price\":经济舱价格(整数),\"aircraftType\":\"机型\",\"onTimeRate\":\"准点率(如92%)\"},\n" +
               "  ...\n" +
               "]\n" +
               "注意：时间分散在早中晚不同时段，价格要有层次差异，机型要真实存在。";
    }

    private String buildBusPrompt(String fromCity, String toCity, String date) {
        return "请根据" + fromCity + "到" + toCity + "的长途客运情况，在" + date + "这天，" +
               "生成4个合理的汽车票（长途大巴）信息。要求如下：\n" +
               "1. 客运站名称要符合" + fromCity + "和" + toCity + "的真实客运站情况\n" +
               "2. 行驶时间要符合城际间长途客运的实际情况\n" +
               "3. 票价要符合中国长途客运市场行情\n\n" +
               "请严格按照以下JSON格式返回（只返回JSON，不要任何其他文字）：\n" +
               "[\n" +
               "  {\"busNo\":\"班次号\",\"company\":\"客运公司\",\"fromStation\":\"出发客运站\",\"toStation\":\"到达客运站\"," +
               "\"departureTime\":\"发车时间(如08:30)\",\"arrivalTime\":\"预计到达时间\",\"duration\":\"行驶时长(如5小时30分)\"," +
               "\"price\":票价(整数),\"busType\":\"车型(如豪华大巴/卧铺车/普通客车)\",\"seatCount\":剩余座位数(整数)},\n" +
               "  ...\n" +
               "]\n" +
               "注意：发车时间分散在早中晚，票价合理，车型名称要符合中国客运实际。";
    }

    private String callDeepSeekAPI(String prompt) {
        throttleCall();

        try {
            JSONObject requestBody = new JSONObject();
            requestBody.set("model", deepseekConfig.getModelName());
            requestBody.set("stream", false);

            JSONArray messages = new JSONArray();
            JSONObject systemMsg = new JSONObject();
            systemMsg.set("role", "system");
            systemMsg.set("content", "You are a transportation information assistant. Reply with valid JSON only, no markdown formatting, no extra text.");
            messages.add(systemMsg);

            JSONObject userMsg = new JSONObject();
            userMsg.set("role", "user");
            userMsg.set("content", prompt);
            messages.add(userMsg);

            requestBody.set("messages", messages);

            String jsonBody = requestBody.toString();
            System.out.println("调用DeepSeek API, 请求内容长度: " + jsonBody.length());

            URL url = URI.create(deepseekConfig.getApiUrl()).toURL();
            HttpURLConnection connection = (HttpURLConnection) url.openConnection();
            connection.setRequestMethod("POST");
            connection.setConnectTimeout(TIMEOUT);
            connection.setReadTimeout(TIMEOUT);
            connection.setRequestProperty("Content-Type", "application/json; charset=UTF-8");
            connection.setRequestProperty("Authorization", "Bearer " + deepseekConfig.getApiKey());
            connection.setDoOutput(true);

            try (OutputStreamWriter writer = new OutputStreamWriter(connection.getOutputStream(), StandardCharsets.UTF_8)) {
                writer.write(jsonBody);
                writer.flush();
            }

            int statusCode = connection.getResponseCode();
            System.out.println("DeepSeek API响应状态码: " + statusCode);

            if (statusCode == 200) {
                StringBuilder response = new StringBuilder();
                try (BufferedReader reader = new BufferedReader(
                        new InputStreamReader(connection.getInputStream(), StandardCharsets.UTF_8))) {
                    String line;
                    while ((line = reader.readLine()) != null) {
                        response.append(line);
                    }
                }
                String rawResponse = response.toString();
                System.out.println("DeepSeek API原始响应: " + rawResponse);
                return extractContent(rawResponse);
            } else {
                System.err.println("DeepSeek API返回非200状态码: " + statusCode);
                return null;
            }

        } catch (Exception e) {
            System.err.println("DeepSeek API调用失败: " + e.getMessage());
            e.printStackTrace();
            return generateFallbackData(prompt.contains("航班") ? "flight" : "bus", prompt);
        }
    }

    private String extractContent(String rawResponse) {
        try {
            JSONObject obj = JSONUtil.parseObj(rawResponse);
            if (obj.containsKey("choices")) {
                JSONArray choices = obj.getJSONArray("choices");
                if (choices != null && !choices.isEmpty()) {
                    JSONObject choice = choices.getJSONObject(0);
                    JSONObject message = choice.getJSONObject("message");
                    if (message != null) {
                        String content = message.getStr("content");
                        if (StrUtil.isNotBlank(content)) {
                            content = content.trim();
                            if (content.startsWith("```")) {
                                int start = content.indexOf("\n");
                                int end = content.lastIndexOf("```");
                                if (start > 0 && end > start) {
                                    content = content.substring(start, end).trim();
                                }
                            }
                            JSONUtil.parseArray(content);
                            return content;
                        }
                    }
                }
            }
        } catch (Exception e) {
            System.err.println("解析DeepSeek响应失败: " + e.getMessage());
        }
        return null;
    }

    private String generateFallbackData(String type, String prompt) {
        List<Map<String, Object>> result = new ArrayList<>();

        String fromCity = extractCity(prompt, 0);
        String toCity = extractCity(prompt, 1);

        if (type.equals("flight")) {
            result.add(createFlightItem("CA1234", "中国国际航空", fromCity + "国际机场",
                    toCity + "国际机场", "07:30", "10:00", "2小时30分", 980, "波音737-800", "95%"));
            result.add(createFlightItem("MU5678", "中国东方航空", fromCity + "国际机场",
                    toCity + "国际机场", "12:20", "14:50", "2小时30分", 1200, "空客A320", "92%"));
            result.add(createFlightItem("CZ9012", "中国南方航空", fromCity + "国际机场",
                    toCity + "国际机场", "16:10", "18:40", "2小时30分", 850, "空客A321", "90%"));
            result.add(createFlightItem("HU3456", "海南航空", fromCity + "国际机场",
                    toCity + "国际机场", "20:50", "23:20", "2小时30分", 760, "波音787-9", "88%"));
        } else {
            result.add(createBusItem("BC001", "省际客运集团", fromCity + "客运中心",
                    toCity + "客运总站", "07:00", "12:30", "5小时30分", 168, "豪华大巴", 35));
            result.add(createBusItem("BC002", "省际客运集团", fromCity + "客运中心",
                    toCity + "客运总站", "09:30", "15:00", "5小时30分", 158, "豪华大巴", 28));
            result.add(createBusItem("BC003", "交通客运公司", fromCity + "汽车站",
                    toCity + "汽车站", "14:00", "20:30", "6小时30分", 128, "普通客车", 42));
            result.add(createBusItem("BC004", "交通客运公司", fromCity + "客运中心",
                    toCity + "客运总站", "18:00", "23:30", "5小时30分", 188, "卧铺大巴", 15));
        }

        return JSONUtil.toJsonStr(result);
    }

    private String extractCity(String prompt, int index) {
        try {
            String[] parts = prompt.split("[到，,]");
            if (index == 0) {
                for (String part : parts) {
                    if (part.contains("从")) {
                        return part.substring(part.indexOf("从") + 1).trim();
                    }
                }
            } else if (parts.length > 1) {
                String city = parts[1].split("[的在，,]")[0].trim();
                if (StrUtil.isNotBlank(city) && city.length() <= 5) {
                    return city;
                }
            }
        } catch (Exception ignored) {
        }
        return "目的地";
    }

    private Map<String, Object> createFlightItem(String flightNo, String airline, String fromAirport,
                                                  String toAirport, String depTime, String arrTime,
                                                  String duration, int price, String aircraftType, String onTimeRate) {
        Map<String, Object> item = new LinkedHashMap<>();
        item.put("flightNo", flightNo);
        item.put("airline", airline);
        item.put("fromAirport", fromAirport);
        item.put("toAirport", toAirport);
        item.put("departureTime", depTime);
        item.put("arrivalTime", arrTime);
        item.put("duration", duration);
        item.put("price", price);
        item.put("aircraftType", aircraftType);
        item.put("onTimeRate", onTimeRate);
        return item;
    }

    private Map<String, Object> createBusItem(String busNo, String company, String fromStation,
                                               String toStation, String depTime, String arrTime,
                                               String duration, int price, String busType, int seatCount) {
        Map<String, Object> item = new LinkedHashMap<>();
        item.put("busNo", busNo);
        item.put("company", company);
        item.put("fromStation", fromStation);
        item.put("toStation", toStation);
        item.put("departureTime", depTime);
        item.put("arrivalTime", arrTime);
        item.put("duration", duration);
        item.put("price", price);
        item.put("busType", busType);
        item.put("seatCount", seatCount);
        return item;
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
}
