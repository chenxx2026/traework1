package com.example.controller;

import com.example.common.Result;
import com.example.entity.TravelPlan;
import com.example.service.TravelPlanService;
import com.github.pagehelper.PageInfo;
import jakarta.annotation.Resource;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/travelPlan")
public class TravelPlanController {

    @Resource
    private TravelPlanService travelPlanService;

    @PostMapping("/add")
    public Result add(@RequestBody TravelPlan travelPlan) {
        try {
            travelPlanService.add(travelPlan);
            return Result.success(travelPlan);
        } catch (Exception e) {
            return Result.error(e.getMessage());
        }
    }

    @PutMapping("/update")
    public Result update(@RequestBody TravelPlan travelPlan) {
        try {
            travelPlanService.updateById(travelPlan);
            return Result.success();
        } catch (Exception e) {
            return Result.error(e.getMessage());
        }
    }

    @DeleteMapping("/delete/{id}")
    public Result delete(@PathVariable Integer id) {
        try {
            travelPlanService.deleteById(id);
            return Result.success();
        } catch (Exception e) {
            return Result.error(e.getMessage());
        }
    }

    @DeleteMapping("/delete/batch")
    public Result delete(@RequestBody List<Integer> ids) {
        try {
            travelPlanService.deleteBatch(ids);
            return Result.success();
        } catch (Exception e) {
            return Result.error(e.getMessage());
        }
    }

    @GetMapping("/selectById/{id}")
    public Result selectById(@PathVariable Integer id) {
        try {
            TravelPlan travelPlan = travelPlanService.selectById(id);
            return Result.success(travelPlan);
        } catch (Exception e) {
            return Result.error(e.getMessage());
        }
    }

    @GetMapping("/selectAll")
    public Result selectAll(TravelPlan travelPlan) {
        try {
            List<TravelPlan> list = travelPlanService.selectAll(travelPlan);
            return Result.success(list);
        } catch (Exception e) {
            return Result.error(e.getMessage());
        }
    }

    @GetMapping("/selectPage")
    public Result selectPage(TravelPlan travelPlan,
                              @RequestParam(defaultValue = "1") Integer pageNum,
                              @RequestParam(defaultValue = "10") Integer pageSize) {
        try {
            PageInfo<TravelPlan> pageInfo = travelPlanService.selectPage(travelPlan, pageNum, pageSize);
            return Result.success(pageInfo);
        } catch (Exception e) {
            return Result.error(e.getMessage());
        }
    }

}
