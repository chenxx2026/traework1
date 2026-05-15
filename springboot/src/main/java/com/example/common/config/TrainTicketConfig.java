package com.example.common.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Component
@ConfigurationProperties(prefix = "train")
public class TrainTicketConfig {
    private String appCode;
    private String apiHost;
    private String stationApiPath;
    private String ticketApiPath;

    public String getAppCode() {
        return appCode;
    }

    public void setAppCode(String appCode) {
        this.appCode = appCode;
    }

    public String getApiHost() {
        return apiHost;
    }

    public void setApiHost(String apiHost) {
        this.apiHost = apiHost;
    }

    public String getStationApiPath() {
        return stationApiPath;
    }

    public void setStationApiPath(String stationApiPath) {
        this.stationApiPath = stationApiPath;
    }

    public String getTicketApiPath() {
        return ticketApiPath;
    }

    public void setTicketApiPath(String ticketApiPath) {
        this.ticketApiPath = ticketApiPath;
    }
}
