package com.cms.smart_home_agent.service;

import com.cms.smart_home_agent.entity.HabitDataLog;
import com.cms.smart_home_agent.mapper.HabitDataLogMapper;
import com.cms.smart_home_agent.request.AiConditioningRequest;
import com.cms.smart_home_agent.request.WeatherRequest;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.function.Function;

@Slf4j
@Service
public class AirConditioningService implements Function<AiConditioningRequest, String> {

    @Autowired
    private HabitLearningService habitLearningService;

    @Autowired
    private HabitDataLogMapper habitDataLogMapper;

    @Autowired
    private WeatherService weatherService;

    // 删除了刚才那行多余的 @Autowired private

    @Override
    public String apply(AiConditioningRequest request) {
        Integer userId = request.getUserId();
        Integer familyId = request.getFamilyId();
        String location = (request.getLocation() == null) ? "客厅" : request.getLocation();
        String city = request.getFamilycity();
        boolean isAiPredicted = false;
        // 兜底逻辑：如果 AI 没传 familyId，尝试从它要控制的那个家提取（或者报错提示 AI）
        if (familyId == null) {
            log.warn("AI 未提供 familyId，尝试使用默认值 1");
            return "familyid 为空，不调空调";
        }

        String modeMessage = "";
        Double targetTemp = (request.getTemperature() == null) ? null : request.getTemperature().doubleValue();

        // --- 第一步：获取实时天气 ---
        // 无论是否预测，我们都需要天气数据来作为“特征”存入数据库
        WeatherRequest weatherRequest = new WeatherRequest();
        weatherRequest.setCity(city);
        String weatherJson = weatherService.apply(weatherRequest);
        double currentOutTemp = parseTempFromJson(weatherJson);
        double currentInTemp = 0.0;
        if (targetTemp == null) {

            // 2. 解析温度
            currentOutTemp = parseTempFromJson(weatherJson);
             currentInTemp = 27.0; // 模拟室内温度

            // 3. 预测逻辑
            targetTemp = habitLearningService.getPersonalizedTemp(userId, familyId, currentOutTemp, currentInTemp);
            modeMessage = "（已通过预测模型为您设定习惯温度）";
            isAiPredicted = true;
        }
        else {
            modeMessage = "（已按您的要求设定）";
            isAiPredicted = false;
        }

// --- 第三步：执行控制（模拟） ---
        log.info(">>>>>> 执行控制：温度为 {} ℃ <<<<<<", targetTemp);

        // --- 第四步：【条件闭环】只记录用户的真实操作 ---
        if (!isAiPredicted) { // 只有不是 AI 预测的时候才存库
            try {
                HabitDataLog logEntity = new HabitDataLog();
                logEntity.setUserId(userId);
                logEntity.setFamilyId(familyId);
                logEntity.setTargetTemp(targetTemp); // 这里的 targetTemp 是用户亲口说的
                logEntity.setOutdoorTemp(currentOutTemp);
                logEntity.setIndoorTemp(currentInTemp);
                habitDataLogMapper.insertHabitLog(logEntity);
                log.info(">>>>>> [模型学习] 记录了用户的一次真实偏好：{}℃ <<<<<<", targetTemp);
            } catch (Exception e) {
                log.error("数据记录失败", e);
            }
        } else {
            log.info(">>>>>> [仅执行] 本次为 AI 预测，不计入训练集以防数据污染 <<<<<<");
        }

        return String.format("成功！%s 的空调已经调整为 %.1f 度, %s",
                location, targetTemp, modeMessage);
    }
    private Double parseTempFromJson(String json) {
        try {
            ObjectMapper mapper = new ObjectMapper();
            JsonNode root = mapper.readTree(json);
            if ("200".equals(root.get("code").asText())) {
                return root.get("now").get("temp").asDouble();
            }
        } catch (Exception e) {
            log.error("解析天气 JSON 失败: {}", e.getMessage());
        }
        return 26.0; // 兜底温度
    }
}