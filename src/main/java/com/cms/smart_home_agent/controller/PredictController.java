package com.cms.smart_home_agent.controller;

import com.cms.smart_home_agent.entity.HabitDataLog;
import com.cms.smart_home_agent.mapper.HabitDataLogMapper;
import com.cms.smart_home_agent.service.HabitLearningService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/aihome/predict")
@Slf4j
public class PredictController {
    @Autowired
    private HabitLearningService habitLearningService;

    @Autowired
    private HabitDataLogMapper habitMapper;

    @GetMapping("/history")
    public List<HabitDataLog> getHistoryData(@RequestParam("familyId") Integer familyId) {
        log.info("开始查询家庭历史数据，familyId: {}", familyId);
        List<HabitDataLog> list = habitMapper.findChartLogs(familyId);

        // ✨ 增加这一行，看看查到了多少条
        log.info("查询完成，记录条数: {}", (list != null ? list.size() : 0));

        return list;
    }

    @GetMapping("/habit")
    public double getSuggest(@RequestParam Integer userId, @RequestParam Integer familyId,@RequestParam Double currentOut, @RequestParam Double currentIn){
        log.info("PredictController getSuggest called with userId={}, familyId={}, currentOut={}, currentIn={}", userId, familyId, currentOut, currentIn);
        double suggestTemp = habitLearningService.getPersonalizedTemp(userId, familyId,currentOut,currentIn);
        log.info("PredictController getSuggest result: {}", suggestTemp);
        return suggestTemp;
    }


}
