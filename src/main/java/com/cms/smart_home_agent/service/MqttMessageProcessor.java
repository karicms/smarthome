package com.cms.smart_home_agent.service;


import com.cms.smart_home_agent.entity.DeviceStatusData;
import com.cms.smart_home_agent.entity.SensorData;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;

// 注意：为了代码能在纯文本中独立显示，我将 SensorData 和 DeviceStatusData 的简化定义放在这里。
// 在您的实际项目中，它们应该在 com.example.aihome.entity 包下定义。

/**
 * MQTT 消息异步处理器。
 * 负责接收 MqttService 委托的消息，进行反序列化和状态更新。
 * 所有方法都使用了 @Async("mqttTaskExecutor") 来确保在专用的线程池中执行，
 * 从而避免阻塞 MQTT 客户端的 I/O 线程，解决消息处理卡顿和连接丢失问题。
 */
@Slf4j
@Component
public class MqttMessageProcessor {

    private final ObjectMapper objectMapper = new ObjectMapper();

    // 使用 AtomicReference 确保在多线程环境下对最新状态的引用是线程安全的
    private final AtomicReference<SensorData> lastSensorData = new AtomicReference<>(new SensorData());
    private final AtomicReference<DeviceStatusData> lastDeviceStatusData = new AtomicReference<>(new DeviceStatusData());

    @Autowired
    private DirectionalPresenceService directionalPresenceService;
    /**
     * 异步处理温湿度传感器数据 (主题: cms-pub)。
     * 关键: 使用 @Async("mqttTaskExecutor") 绑定到专用线程池。
     * @param topic 消息主题
     * @param payload JSON 字符串负载
     */
    @Async("mqttTaskExecutor")
    public void processSensorData(String topic, String payload) {
        // 打印出当前处理线程，证明其来自专用线程池 (如: MQTT-Processor-1)
        System.out.println("异步线程 [" + Thread.currentThread().getName() + "] 处理「" + topic + "」温湿度数据: " + payload);
        try {
            // 注意：此处需要您引入 com.example.aihome.entity.SensorData
            SensorData data = objectMapper.readValue(payload, SensorData.class);
            lastSensorData.set(data);
            System.out.printf("-> 最新传感器数据已异步更新: SensorData(humidity=%.1f, temperature=%.1f)\n",
                    data.getHumidity(), data.getTemperature());
        } catch (JsonProcessingException e) {
            System.err.println("解析传感器数据失败: " + e.getMessage());
        }
    }

    /**
     * 异步处理设备状态确认数据 (主题: cms-device-status)。
     * 关键: 使用 @Async("mqttTaskExecutor") 绑定到专用线程池。
     * @param topic 消息主题
     * @param payload JSON 字符串负载
     */
    @Async("mqttTaskExecutor")
    public void processDeviceStatus(String topic, String payload) {
        System.out.println("异步线程 [" + Thread.currentThread().getName() + "] 处理「" + topic + "」设备状态确认: " + payload);
        try {
            // 注意：此处需要您引入 com.example.aihome.entity.DeviceStatusData
            DeviceStatusData data = objectMapper.readValue(payload, DeviceStatusData.class);
            lastDeviceStatusData.set(data);
            System.out.printf("-> 设备状态已异步更新: LED=%b, Buzzer=%b\n",
                    data.isLedStatus(), data.isBuzzerStatus());
        } catch (JsonProcessingException e) {
            System.err.println("解析设备状态数据失败: " + e.getMessage());
        }
    }

    public void processIrSensorData(String topic,String payload)
    {
        log.info("异步线程 [{}] 处理「{}」红外传感器数据: {}", Thread.currentThread().getName(), topic, payload);
        try{
            Map<String,Object> data = objectMapper.readValue(payload,Map.class);
            String sensorId = (String)data.get("sensorId");
            Integer familyId = (Integer)data.get("familyId");
            if(sensorId == null || familyId == null)
            {
                log.warn("红外传感器数据缺少必要字段: {}", payload);
                return;
            }

            String result = directionalPresenceService.processIrTrigger(sensorId,familyId);
            // 3. 根据结果执行不同的业务逻辑
            if ("ENTRY".equals(result)) {
                log.info("🎉 判定结果：【进入房间】。可以在此触发开灯等逻辑。");
                // TODO: 之后在这里调用数据库保存方法
            } else if ("EXIT".equals(result)) {
                log.info("🚪 判定结果：【离开房间】。可以在此触发关灯等逻辑。");
                // TODO: 之后在这里调用数据库保存方法
            } else {
                log.info("⏳ 判定结果：等待另一侧传感器触发...");
            }
        } catch (JsonMappingException e) {
            throw new RuntimeException(e);
        } catch (JsonProcessingException e) {
            throw new RuntimeException(e);
        }
    }

    // 提供获取最新状态的方法
    public SensorData getLastSensorData() {
        return lastSensorData.get();
    }

    public DeviceStatusData getLastDeviceStatusData() {
        return lastDeviceStatusData.get();
    }
}