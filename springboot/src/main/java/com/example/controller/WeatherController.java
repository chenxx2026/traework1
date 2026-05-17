package com.example.controller;

import com.example.common.Result;
import com.example.service.WeatherService;
import jakarta.annotation.Resource;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/weather")
public class WeatherController {

    @Resource
    private WeatherService weatherService;

    @GetMapping("/query")
    public Result query(@RequestParam String city,
                        @RequestParam(required = false) String startDate,
                        @RequestParam(required = false) String endDate) {
        List<Map<String, Object>> weatherData = weatherService.queryWeather(city, startDate, endDate);
        return Result.success(weatherData);
    }
}
