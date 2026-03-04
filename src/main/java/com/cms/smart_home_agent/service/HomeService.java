package com.cms.smart_home_agent.service;


import com.cms.smart_home_agent.request.ControlRequest;
import com.cms.smart_home_agent.entity.HomestatusResponse;
import com.cms.smart_home_agent.entity.tempconfig;
import com.fasterxml.jackson.core.JsonProcessingException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class HomeService {

    // 注入 MQTT 服务
    @Autowired
    private MqttService mqttService;

    @Autowired
    private MqttMessageProcessor mqttMessageProcessor;

    /**
     * 【任务 1】获取当前设备状态 (数据来自 MQTT 上报的最新 SensorData 和 DeviceStatusData)
     */
    public HomestatusResponse getstatus()
    {
        double temp =mqttMessageProcessor.getLastSensorData().getTemperature();
        double hum=mqttMessageProcessor.getLastSensorData().getHumidity();
        boolean ledStatus=mqttMessageProcessor.getLastDeviceStatusData().isLedStatus();
        boolean buzzerStatus=mqttMessageProcessor.getLastDeviceStatusData().isBuzzerStatus();
        boolean monitorStatus = mqttMessageProcessor.getLastDeviceStatusData().isMonitorStatus();
        HomestatusResponse response = new HomestatusResponse();
        response.setHumidity(hum);
        response.setTemperature(temp);
        response.setLedStatus(ledStatus);
        response.setBuzzerStatus(buzzerStatus);
        response.setMonitorStatus(monitorStatus);
        return response;
    }

    /**
     * 【任务 2】控制设备状态 - 通过 MQTT 发送指令
     * @param deviceName 设备名称 ("led" 或 "buzzer")
     * @param request 控制请求 (on/off, true/false)
     */
    public boolean controlDevice(String deviceName, ControlRequest request) {
        String action = request.getAction();

        // 核心逻辑：通过 MQTT 发布消息到设备控制主题 (例如: led-sub)
        try {
            mqttService.publishControlCommand(deviceName, action);
            // 乐观更新：发送成功即返回 true
            return true;

        } catch (JsonProcessingException e) {
            throw new RuntimeException(e);
        }
    }

    public boolean controlconfig(tempconfig temp)
    {
        float config= temp.getTempThreshold();
        try{
            mqttService.publishConfigCommand(config);
            return true;
        } catch (JsonProcessingException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * 【任务 3】处理 AI 自然语言指令 - 在 AI 决策后发布 MQTT 消息
     */
//    public AiCommandResponse handleAiCommand(String command) {
//        command = command.toLowerCase();
//        String action = "";
//        String deviceName = "";
//
//        // 1. AI 决策逻辑
//        if (command.contains("开灯") || command.contains("打开灯")) {
//            deviceName = "led";
//            action = "ON";
//        } else if (command.contains("关灯") || command.contains("关闭灯")) {
//            deviceName = "led";
//            action = "OFF";
//        } else if (command.contains("太热") || command.contains("报警")) {
//            deviceName = "buzzer";
//            action = "ON";
//        } else {
//            return new AiCommandResponse("AI 无法识别指令。请尝试'开灯'或'太热了'。");
//        }
//
//        // 2. 执行控制（调用 controlDevice）
//        boolean published = controlDevice(deviceName, new ControlRequest() {{
//            setAction(action);
//            setValue(action.equals("ON"));
//        }});
//
//        // 3. 返回结果
//        if (published) {
//            return new AiCommandResponse("AI 决策：已执行【" + action + "】指令，通过 MQTT 发送到硬件。");
//        } else {
//            return new AiCommandResponse("AI 决策：指令已解析，但 MQTT 消息发送失败。");
//        }
//    }
}