package com.example.controller;

import cn.hutool.core.util.StrUtil;
import cn.hutool.json.JSONArray;
import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;
import com.example.common.Result;
import com.example.service.DeepseekService;
import com.example.service.TrainTicketService;
import jakarta.annotation.Resource;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/ticket")
public class TrainTicketController {

    @Resource
    private TrainTicketService trainTicketService;

    @Resource
    private DeepseekService deepseekService;

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

        if ("ALL".equals(trainType)) {
            return queryAllTypes(fromStation, toStation, date);
        }

        if ("PLANE".equals(trainType)) {
            String flightResult = deepseekService.queryFlight(fromStation, toStation, date);
            if (StrUtil.isBlank(flightResult)) {
                return Result.error("航班查询失败，请稍后重试");
            }
            try {
                JSONArray flightList = JSONUtil.parseArray(flightResult);
                return Result.success(flightList);
            } catch (Exception e) {
                return Result.error("航班数据解析失败");
            }
        }

        if ("BUS".equals(trainType)) {
            String busResult = deepseekService.queryBus(fromStation, toStation, date);
            if (StrUtil.isBlank(busResult)) {
                return Result.error("汽车票查询失败，请稍后重试");
            }
            try {
                JSONArray busList = JSONUtil.parseArray(busResult);
                return Result.success(busList);
            } catch (Exception e) {
                return Result.error("汽车票数据解析失败");
            }
        }

        String apiResponse = trainTicketService.queryTickets(fromStation, toStation, date, trainType);

        Map<String, Object> rawResult = JSONUtil.toBean(apiResponse, Map.class);
        if (rawResult.containsKey("error")) {
            return Result.error((String) rawResult.get("error"));
        }

        String parsedData = trainTicketService.parseTicketResponse(apiResponse);
        return Result.success(JSONUtil.parse(parsedData));
    }

    private Result queryAllTypes(String fromStation, String toStation, String date) {
        JSONArray mergedList = new JSONArray();

        String apiResponse = trainTicketService.queryTickets(fromStation, toStation, date, "");
        Map<String, Object> rawResult = JSONUtil.toBean(apiResponse, Map.class);
        if (!rawResult.containsKey("error")) {
            String parsedData = trainTicketService.parseTicketResponse(apiResponse);
            JSONArray trainList = JSONUtil.parseArray(parsedData);
            if (trainList != null) {
                for (int i = 0; i < trainList.size(); i++) {
                    JSONObject item = trainList.getJSONObject(i);
                    item.set("source", "train");
                    mergedList.add(item);
                }
            }
        }

        String flightResult = deepseekService.queryFlight(fromStation, toStation, date);
        if (StrUtil.isNotBlank(flightResult)) {
            try {
                JSONArray flightList = JSONUtil.parseArray(flightResult);
                if (flightList != null) {
                    for (int i = 0; i < flightList.size(); i++) {
                        JSONObject item = flightList.getJSONObject(i);
                        item.set("source", "plane");
                        mergedList.add(item);
                    }
                }
            } catch (Exception e) {
                System.err.println("合并航班数据失败: " + e.getMessage());
            }
        }

        String busResult = deepseekService.queryBus(fromStation, toStation, date);
        if (StrUtil.isNotBlank(busResult)) {
            try {
                JSONArray busList = JSONUtil.parseArray(busResult);
                if (busList != null) {
                    for (int i = 0; i < busList.size(); i++) {
                        JSONObject item = busList.getJSONObject(i);
                        item.set("source", "bus");
                        mergedList.add(item);
                    }
                }
            } catch (Exception e) {
                System.err.println("合并汽车票数据失败: " + e.getMessage());
            }
        }

        return Result.success(mergedList);
    }
}
