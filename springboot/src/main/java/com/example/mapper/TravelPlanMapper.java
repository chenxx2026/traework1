package com.example.mapper;

import com.example.entity.TravelPlan;
import org.apache.ibatis.annotations.Select;

import java.util.List;

public interface TravelPlanMapper {

    int insert(TravelPlan travelPlan);

    void updateById(TravelPlan travelPlan);

    void deleteById(Integer id);

    @Select("select travel_plan.*, user.name as userName from `travel_plan`" +
            " left join user on travel_plan.user_id = user.id where travel_plan.id = #{id}")
    TravelPlan selectById(Integer id);

    List<TravelPlan> selectAll(TravelPlan travelPlan);

}
