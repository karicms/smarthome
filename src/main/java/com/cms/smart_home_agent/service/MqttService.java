package com.cms.smart_home_agent.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.eclipse.paho.client.mqttv3.*;
import org.eclipse.paho.client.mqttv3.persist.MemoryPersistence;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;


import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;

/**
 * MQTT 消息服务 (基于 Paho 客户端实现)
 * 负责与 MQTT Broker 建立连接、订阅和发布消息。
 * 实现了 MqttCallback 接口接收消息，并委托给 MqttMessageProcessor 异步处理。
 */
@Service
@EnableScheduling
@Slf4j
public class MqttService implements MqttCallback {

    // --- MQTT 配置 ---
    private static final String broker = "tcp://broker.emqx.io:1883";
    private static final String CLIENT_ID = "spring_boot_aihome_server";

    // --- 用户指定的主题配置 ---
    private static final String TOPIC_CMD = "cms-sub";
    private static final String TOPIC_SENSOR_STATUS = "cms-pub";
    private static final String TOPIC_DEVICE_STATUS_ACK = "cms-device-status";
    private static final String TOPIC_CONFIG="cms-config";
    private static final String IR_TOPIC = "cms-ir-sensor";

    private final IMqttClient client;
    private final ObjectMapper objectMapper = new ObjectMapper();
    private final MqttMessageProcessor messageProcessor;

    /**
     * 构造函数注入 MqttMessageProcessor。
     */
    public MqttService(MqttMessageProcessor messageProcessor) throws MqttException {
        this.messageProcessor = messageProcessor;
        this.client = new MqttClient(broker, CLIENT_ID, new MemoryPersistence());
    }

    /**
     * 应用程序启动后初始化 MQTT 客户端和连接。
     */
    @PostConstruct
    public void init() {
        connectAndSubscribe(); // 初始化连接和订阅
    }

    /**
     * 核心连接和订阅逻辑。
     * 配置自动重连和心跳，并在成功连接后重新订阅。
     */
    private void connectAndSubscribe() {
        if (client.isConnected()) {
            return;
        }
        try {
            MqttConnectOptions options = new MqttConnectOptions();
            options.setCleanSession(true);
            options.setAutomaticReconnect(true); // 启用 Paho 自动重连
            options.setKeepAliveInterval(60); // 设置心跳间隔

            client.setCallback(this);

            if (!client.isConnected()) {
                client.connect(options);
                System.out.println("MQTT 客户端已连接到 broker：" + broker);
                subscribeTopics();
            }
        } catch (MqttException e) {
            System.err.println("【MQTT ERROR】客户端连接失败: " + e.getMessage() + "，将在定时任务中重试。");
        }
    }

    // ===================================================================
    // MqttCallback 接口实现
    // ===================================================================

    @Override
    public void connectionLost(Throwable throwable) {
        System.err.println("【MQTT ERROR】连接丢失! 原因: " + throwable.getMessage());
    }

    /**
     * 接收到消息的回调方法。
     * 重点：此方法不能阻塞，应立即委托给异步处理器。
     */
    @Override
    public void messageArrived(String topic, MqttMessage mqttMessage) {
        String payload = new String(mqttMessage.getPayload(), StandardCharsets.UTF_8);
        System.out.println("【MQTT IN】接收到主题: " + topic + "，委托给异步线程处理。");

        if (TOPIC_SENSOR_STATUS.equals(topic)) {
            messageProcessor.processSensorData(topic, payload);
        } else if (TOPIC_DEVICE_STATUS_ACK.equals(topic)) {
            messageProcessor.processDeviceStatus(topic, payload);
        }else if (IR_TOPIC.equals(topic)) {
            messageProcessor.processIrSensorData(topic, payload);

        }
    }

    @Override
    public void deliveryComplete(IMqttDeliveryToken iMqttDeliveryToken) {
        // 消息投递完成
    }

    // ===================================================================
    // 订阅和重连调度
    // ===================================================================

    /**
     * 订阅所有必需的状态主题。
     */
    private void subscribeTopics() throws MqttException {
        client.subscribe(TOPIC_SENSOR_STATUS, 1);
        client.subscribe(TOPIC_DEVICE_STATUS_ACK, 1);
        client.subscribe(IR_TOPIC, 1);
        System.out.println("已订阅主题: " + TOPIC_SENSOR_STATUS);
        System.out.println("已订阅主题: " + TOPIC_DEVICE_STATUS_ACK);
        System.out.println("已订阅主题: " + IR_TOPIC);
    }

    /**
     * 【定时任务】每隔 10 秒检查连接状态并尝试重连/重新订阅。
     */
    @Scheduled(fixedRate = 10000)
    public void scheduledReconnect() {
        if (client != null && !client.isConnected()) {
            System.out.println("MQTT 后台检测到连接断开，尝试手动重连...");
            connectAndSubscribe();
        }
    }

    /**
     * 发布 JSON 格式的控制指令到 cms-sub 主题。
     * @param deviceName 设备名称 (如 "LED", "BUZZER")
     * @param action 控制动作 (如 "ON", "OFF")
     * @throws RuntimeException 如果客户端未连接或发布失败
     */
    public void publishControlCommand(String deviceName, String action) throws JsonProcessingException {
        if (client == null || !client.isConnected()) {
            throw new RuntimeException("MQTT 客户端未连接，无法发送控制指令。");
        }

        boolean value = action.equalsIgnoreCase("ON");

        try {
            Map<String, Object> payloadMap = new HashMap<>();
            payloadMap.put("deviceName", deviceName);
            payloadMap.put("value", value);

            String jsonPayload = objectMapper.writeValueAsString(payloadMap);

            MqttMessage message = new MqttMessage(jsonPayload.getBytes(StandardCharsets.UTF_8));
            message.setQos(1);

            client.publish(TOPIC_CMD, message);
            System.out.printf("向指令主题「%s」发布 JSON -> %s\n", TOPIC_CMD, jsonPayload);

        } catch (MqttException e) {
            System.err.println("【MQTT ERROR】发送控制指令失败: " + e.getMessage());
            throw new RuntimeException("MQTT 消息发布失败，原因: " + e.getMessage(), e);
        }
    }

    public void publishToDevice(String topic, String deviceName, String action) throws JsonProcessingException {
        if (client == null || !client.isConnected()) {
            throw new RuntimeException("MQTT 客户端未连接");
        }

        boolean value = action.equalsIgnoreCase("ON");

        try {
            // 构建 payload，依然保留 deviceName 是为了让硬件多一层判断，更安全
            Map<String, Object> payloadMap = new HashMap<>();
            payloadMap.put("deviceName", deviceName);
            payloadMap.put("value", value);

            String jsonPayload = objectMapper.writeValueAsString(payloadMap);
            MqttMessage message = new MqttMessage(jsonPayload.getBytes(StandardCharsets.UTF_8));
            message.setQos(1);

            // 【关键改动】这里不再用 TOPIC_CMD，而是用参数传进来的 topic
            client.publish(topic, message);

            log.info(">> [精准推送] 主题: {}, 内容: {}", topic, jsonPayload);

        } catch (MqttException e) {
            log.error("MQTT 发送失败: {}", e.getMessage());
            throw new RuntimeException("发送失败", e);
        }
    }
    /**
     * 【新增】发布 JSON 格式的配置指令到 cms-config 主题，用于更新温度阈值。
     * JSON 格式: {"tempThreshold": 32.5}
     * @param temperatureThreshold 新的温度报警阈值 (例如 32.5)
     * @throws RuntimeException 如果客户端未连接或发布失败
     */
    public void publishConfigCommand(float temperatureThreshold) throws JsonProcessingException {
        if (client == null || !client.isConnected()) {
            throw new RuntimeException("MQTT 客户端未连接，无法发送配置指令。");
        }

        try {
            Map<String, Object> payloadMap = new HashMap<>();
            payloadMap.put("tempThreshold", temperatureThreshold);

            String jsonPayload = objectMapper.writeValueAsString(payloadMap);
            MqttMessage message = new MqttMessage(jsonPayload.getBytes(StandardCharsets.UTF_8));
            message.setQos(1);

            client.publish(TOPIC_CONFIG, message);
            System.out.printf("向配置主题「%s」发布 JSON -> %s\n", TOPIC_CONFIG, jsonPayload);

        } catch (MqttException e) {
            System.err.println("【MQTT ERROR】发送配置指令失败: " + e.getMessage());
            throw new RuntimeException("MQTT 消息发布失败，原因: " + e.getMessage(), e);
        }
    }
}