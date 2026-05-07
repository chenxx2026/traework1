package com.example.service;

import cn.hutool.core.date.DateUtil;
import cn.hutool.core.util.StrUtil;
import com.example.common.Result;
import com.example.entity.Account;
import com.example.entity.TravelPlan;
import com.example.mapper.TravelPlanMapper;
import com.example.utils.TokenUtils;
import com.github.pagehelper.PageHelper;
import com.github.pagehelper.PageInfo;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;

@Service
public class TravelPlanService {

    @Resource
    private TravelPlanMapper travelPlanMapper;

    /**
     * 添加行程前的数据验证
     */
    private void validateTravelPlan(TravelPlan travelPlan) {
        // 验证目的地
        if (StrUtil.isBlank(travelPlan.getDestination())) {
            throw new RuntimeException("目的地不能为空");
        }
        if (travelPlan.getDestination().length() > 50) {
            throw new RuntimeException("目的地名称不能超过50个字符");
        }

        // 验证日期
        if (StrUtil.isBlank(travelPlan.getStartDate())) {
            throw new RuntimeException("开始日期不能为空");
        }
        if (StrUtil.isBlank(travelPlan.getEndDate())) {
            throw new RuntimeException("结束日期不能为空");
        }

        // 验证日期格式和逻辑
        try {
            SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");
            sdf.setLenient(false);
            Date start = sdf.parse(travelPlan.getStartDate());
            Date end = sdf.parse(travelPlan.getEndDate());

            if (end.before(start)) {
                throw new RuntimeException("结束日期不能早于开始日期");
            }
        } catch (ParseException e) {
            throw new RuntimeException("日期格式不正确，请使用YYYY-MM-DD格式");
        }

        // 验证路线规划内容
        if (StrUtil.isBlank(travelPlan.getPlanData())) {
            throw new RuntimeException("路线规划内容不能为空");
        }
        if (travelPlan.getPlanData().length() < 10) {
            throw new RuntimeException("路线规划内容至少需要10个字符");
        }

        // 验证预算（如果提供了）
        if (travelPlan.getTotalBudget() != null && travelPlan.getTotalBudget() < 0) {
            throw new RuntimeException("总预算不能为负数");
        }
        if (travelPlan.getDailyBudget() != null && travelPlan.getDailyBudget() < 0) {
            throw new RuntimeException("每日预算不能为负数");
        }
    }

    public void add(TravelPlan travelPlan) {
        // 数据验证
        validateTravelPlan(travelPlan);

        travelPlan.setCreateTime(DateUtil.now());
        Account currentUser = TokenUtils.getCurrentUser();
        if (currentUser == null) {
            throw new RuntimeException("请先登录");
        }
        travelPlan.setUserId(currentUser.getId());
        travelPlan.setUserName(currentUser.getUsername());
        travelPlanMapper.insert(travelPlan);
    }

    public void updateById(TravelPlan travelPlan) {
        // 验证数据
        if (travelPlan.getId() == null) {
            throw new RuntimeException("行程ID不能为空");
        }
        // 其他字段验证（如果提供了）
        if (StrUtil.isNotBlank(travelPlan.getDestination()) || StrUtil.isNotBlank(travelPlan.getStartDate())) {
            validateTravelPlan(travelPlan);
        }
        travelPlanMapper.updateById(travelPlan);
    }

    public void deleteById(Integer id) {
        if (id == null) {
            throw new RuntimeException("行程ID不能为空");
        }
        travelPlanMapper.deleteById(id);
    }

    public void deleteBatch(List<Integer> ids) {
        if (ids == null || ids.isEmpty()) {
            throw new RuntimeException("请选择要删除的行程");
        }
        for (Integer id : ids) {
            travelPlanMapper.deleteById(id);
        }
    }

    public TravelPlan selectById(Integer id) {
        if (id == null) {
            throw new RuntimeException("行程ID不能为空");
        }
        return travelPlanMapper.selectById(id);
    }

    public List<TravelPlan> selectAll(TravelPlan travelPlan) {
        Account currentUser = TokenUtils.getCurrentUser();
        if (currentUser != null) {
            travelPlan.setUserId(currentUser.getId());
        }
        return travelPlanMapper.selectAll(travelPlan);
    }

    public PageInfo<TravelPlan> selectPage(TravelPlan travelPlan, Integer pageNum, Integer pageSize) {
        Account currentUser = TokenUtils.getCurrentUser();
        if (currentUser != null) {
            travelPlan.setUserId(currentUser.getId());
        }
        PageHelper.startPage(pageNum, pageSize);
        List<TravelPlan> list = travelPlanMapper.selectAll(travelPlan);
        return PageInfo.of(list);
    }

}
