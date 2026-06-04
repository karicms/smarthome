package com.cms.smart_home_agent.service;

import com.cms.smart_home_agent.request.LightRequest;
import com.fasterxml.jackson.core.JsonProcessingException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.function.Function;

@Service
@Slf4j
public class LightService implements Function<LightRequest, String> {
    @Autowired
    private MqttService mqttService;

    @Autowired
    private DeviceService deviceService;

    @Override
    public String apply(LightRequest lightRequest) {

            String location = lightRequest.getLocation();
            String actionStr = lightRequest.getAction();
            boolean isOpen = Boolean.parseBoolean(actionStr);
            String action = isOpen ? "ON" : "OFF"; // 转换成硬件识别的指令
            Integer userId = lightRequest.getUserId();
            Integer familyId = lightRequest.getFamilyId();
            String deviceName = lightRequest.getDeviceName();
            String deviceType = lightRequest.getDeviceType();
        //后续通过用户id去查询对应的设备id，从而发送对这个设备的控制指令
            log.info("lightRequest:{}", lightRequest);
            log.info("location:{}", location);
            log.info("action:{}", action);
            log.info("userId:{}", userId);
            log.info("familyId:{}", familyId);
            log.info("deviceName:{}", deviceName);

            String topic = deviceService.findTopic(familyId,deviceType,deviceName);
        try {
            mqttService.publishToDevice(topic,deviceName,action);
        } catch (JsonProcessingException e) {
            throw new RuntimeException(e);
        }

        // 这里可以根据doorRequest中的location和action来执行相应的操作
            // 例如：
            // 根据location和action执行开灯或关灯的操作
            // 这里仅返回一个示例字符串，实际应用中应该调用相应的硬件接口来控制灯的状态
            return "已执行 " + action + " " + (location.isEmpty() ? "客厅" : location) + " 的灯";
    }
}
