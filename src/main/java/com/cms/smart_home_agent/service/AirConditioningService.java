package com.cms.smart_home_agent.service;

import com.cms.smart_home_agent.entity.HabitDataLog;
import com.cms.smart_home_agent.mapper.HabitDataLogMapper;
import com.cms.smart_home_agent.request.AiConditioningRequest;
import com.cms.smart_home_agent.request.WeatherRequest;
import com.fasterxml.jackson.core.JsonProcessingException;
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

    @Autowired
    private DeviceService deviceService;

    @Autowired
    private MqttService mqttService;

    @Autowired
    private IndoorClimateService indoorClimateService;

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

        WeatherRequest weatherRequest = new WeatherRequest();
        weatherRequest.setCity(city);
        String weatherJson = weatherService.apply(weatherRequest);
        double currentOutTemp = parseTempFromJson(weatherJson);
        double currentInTemp = indoorClimateService.getIndoorTemperatureCelsius();
        log.info("环境特征 室外(天气): {} ℃, 室内(传感器): {} ℃", currentOutTemp, currentInTemp);

        if (targetTemp == null) {
            targetTemp = habitLearningService.getPersonalizedTemp(userId, familyId, currentOutTemp, currentInTemp);
            modeMessage = "（已通过预测模型为您设定习惯温度）";
            isAiPredicted = true;
        } else {
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
        String deviceName = request.getDeviceName();
        String deviceType = request.getDeviceType();
        String topic = deviceService.findTopic(familyId, deviceType, deviceName);

        // --- 新增：安全拦截与兜底机制 ---
        if (topic == null || topic.trim().isEmpty()) {
            log.error(">>>>>> 获取 Topic 失败！AI传入参数 -> familyId: {}, deviceType: {}, deviceName: {} <<<<<<",
                    familyId, deviceType, deviceName);
            // 方案 A: 严格模式，直接返回报错，让 AI 知道它找错设备了
            return String.format("控制失败：未在家庭(ID:%d)中找到名称为'%s'的空调设备，请检查设备名称。", familyId, deviceName);

            // 方案 B (如果你想强行测试通过，可以解开下面这行的注释，写死兜底的 topic)
            // topic = "airconditioner-sub";
            // log.warn("使用兜底 topic: {}", topic);
        }

        String action = String.format("{\"temp\":%.1f}", targetTemp);
        log.info("准备发送控制指令 -> deviceName: {}, deviceType: {}, topic: {}, action: {}", deviceName, deviceType, topic, action);

        try {
            mqttService.publishToDevice(topic, deviceName, action);
        } catch (JsonProcessingException e) {
            log.error("MQTT JSON 转换失败", e);
            return "控制失败：内部系统数据格式错误。";
        } catch (Exception e) {
            log.error("MQTT 发布失败", e);
            return "控制失败：MQTT 消息发送异常。";
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