package com.example.common.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Component
@ConfigurationProperties(prefix = "weather")
public class WeatherConfig {
    private String appid;
    private String appsecret;
    private String apiUrl;
    private String fallbackApiUrl;

    public String getAppid() {
        return appid;
    }

    public void setAppid(String appid) {
        this.appid = appid;
    }

    public String getAppsecret() {
        return appsecret;
    }

    public void setAppsecret(String appsecret) {
        this.appsecret = appsecret;
    }

    public String getApiUrl() {
        return apiUrl;
    }

    public void setApiUrl(String apiUrl) {
        this.apiUrl = apiUrl;
    }

    public String getFallbackApiUrl() {
        return fallbackApiUrl;
    }

    public void setFallbackApiUrl(String fallbackApiUrl) {
        this.fallbackApiUrl = fallbackApiUrl;
    }
}
