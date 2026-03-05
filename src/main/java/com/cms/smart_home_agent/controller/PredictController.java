package com.cms.smart_home_agent.controller;

import com.cms.smart_home_agent.mapper.HabitDataLogMapper;
import com.cms.smart_home_agent.service.HabitLearningService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/predict")
@Slf4j
public class PredictController {
    @Autowired
    private HabitLearningService habitLearningService;

    @GetMapping("/habit")
    public double getSuggest(@RequestParam Integer userId, @RequestParam Integer familyId,@RequestParam Double currentOut, @RequestParam Double currentIn){
        log.info("PredictController getSuggest called with userId={}, familyId={}, currentOut={}, currentIn={}", userId, familyId, currentOut, currentIn);
        double suggestTemp = habitLearningService.getPersonalizedTemp(userId, familyId,currentOut,currentIn);
        log.info("PredictController getSuggest result: {}", suggestTemp);
        return suggestTemp;
    }

}
