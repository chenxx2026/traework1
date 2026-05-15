package com.example.controller;

import cn.hutool.core.util.StrUtil;
import cn.hutool.json.JSONUtil;
import com.example.common.Result;
import com.example.service.TrainTicketService;
import jakarta.annotation.Resource;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/ticket")
public class TrainTicketController {

    @Resource
    private TrainTicketService trainTicketService;

    @GetMapping("/station")
    public Result searchStation(@RequestParam String station) {
        if (StrUtil.isBlank(station)) {
            return Result.error("车站名称不能为空");
        }
        String result = trainTicketService.searchStation(station);
        Map<String, Object> resultMap = JSONUtil.toBean(result, Map.class);
        if (resultMap.containsKey("error")) {
            return Result.error((String) resultMap.get("error"));
        }
        return Result.success(JSONUtil.parse(result));
    }

    @GetMapping("/query")
    public Result queryTickets(@RequestParam String fromStation,
                               @RequestParam String toStation,
                               @RequestParam String date,
                               @RequestParam(required = false) String trainType) {
        if (StrUtil.isBlank(fromStation) || StrUtil.isBlank(toStation) || StrUtil.isBlank(date)) {
            return Result.error("出发地、目的地和日期不能为空");
        }

        String apiResponse = trainTicketService.queryTickets(fromStation, toStation, date, trainType);

        Map<String, Object> rawResult = JSONUtil.toBean(apiResponse, Map.class);
        if (rawResult.containsKey("error")) {
            return Result.error((String) rawResult.get("error"));
        }

        String parsedData = trainTicketService.parseTicketResponse(apiResponse);
        return Result.success(JSONUtil.parse(parsedData));
    }
}
