package com.cms.smart_home_agent.service;

import com.cms.smart_home_agent.climate.ClimateSensorTopicRegistry;
import com.cms.smart_home_agent.climate.ClimateSensorTypeResolver;
import com.cms.smart_home_agent.mqtt.MqttTopics;
import com.cms.smart_home_agent.presence.IrBeamSideResolver;
import com.cms.smart_home_agent.presence.IrBeamTopicBinding;
import com.cms.smart_home_agent.presence.IrBeamTopicRegistry;
import com.cms.smart_home_agent.presence.PresenceSensorTopicRegistry;
import com.cms.smart_home_agent.presence.PresenceSensorTopicRegistry.PresenceSensorTopicBinding;
import com.cms.smart_home_agent.presence.PresenceSensorTypeResolver;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.eclipse.paho.client.mqttv3.*;
import org.eclipse.paho.client.mqttv3.persist.MemoryPersistence;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;


import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;

/**
 * MQTT 消息服务 (基于 Paho 客户端实现)
 * 负责与 MQTT Broker 建立连接、订阅和发布消息。
 * 实现 {@link MqttCallbackExtended}：在每次连接成功（含自动重连）后重新订阅，
 * 否则 cleanSession 下会收不到硬件发到 cms-device-status 等的定时上报。
 */
@Service
@Slf4j
public class MqttService implements MqttCallbackExtended {

    // --- MQTT 配置 ---
    private static final String broker = "tcp://broker.emqx.io:1883";
    /** 避免与其它实例/调试工具冲突导致互踢 */
    private static final String CLIENT_ID =
            "spring_aihome_" + java.util.UUID.randomUUID().toString().substring(0, 8);

    // --- 用户指定的主题配置 ---
    private static final String TOPIC_CMD = "cms-sub";
    private static final String TOPIC_SENSOR_STATUS = "cms-pub";
    private static final String TOPIC_DEVICE_STATUS_ACK = "cms-device-status";
    private static final String TOPIC_CONFIG="cms-config";
    private static final String PIR_TOPIC = "cms-pir-sensor";
    private static final String MMWAVE_TOPIC = "cms-mmwave-sensor";

    private final IMqttClient client;
    private final ObjectMapper objectMapper = new ObjectMapper();
    private final MqttMessageProcessor messageProcessor;
    private final IrBeamTopicRegistry irBeamTopicRegistry;
    private final PresenceSensorTopicRegistry presenceSensorTopicRegistry;
    private final ClimateSensorTopicRegistry climateSensorTopicRegistry;
    private final DeviceService deviceService;

    /**
     * 构造函数注入 MqttMessageProcessor、Topic 注册表；DeviceService 仅用于路由兜底（缓存未刷新的新设备）。
     */
    public MqttService(
            MqttMessageProcessor messageProcessor,
            IrBeamTopicRegistry irBeamTopicRegistry,
            PresenceSensorTopicRegistry presenceSensorTopicRegistry,
            ClimateSensorTopicRegistry climateSensorTopicRegistry,
            DeviceService deviceService)
            throws MqttException {
        this.messageProcessor = messageProcessor;
        this.irBeamTopicRegistry = irBeamTopicRegistry;
        this.presenceSensorTopicRegistry = presenceSensorTopicRegistry;
        this.climateSensorTopicRegistry = climateSensorTopicRegistry;
        this.deviceService = deviceService;
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
                /* 订阅见 connectComplete（自动重连后也会重新订阅） */
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

    @Override
    public void connectComplete(boolean reconnect, String serverURI) {
        try {
            subscribeTopics();
            log.info("MQTT 已订阅主题（connectComplete reconnect={}, uri={}）", reconnect, serverURI);
        } catch (MqttException e) {
            log.error("MQTT connectComplete 后订阅失败", e);
        }
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
            log.debug("收到设备状态上报 payload={}", payload);
            messageProcessor.processDeviceStatus(topic, payload);
        } else if (MqttTopics.LEGACY_IR_SENSOR.equals(topic)) {
            messageProcessor.processIrSensorData(topic, payload);
        } else if (PIR_TOPIC.equals(topic)) {
            messageProcessor.processPirSensorData(topic, payload);
        } else if (MMWAVE_TOPIC.equals(topic)) {
            messageProcessor.processMmWaveSensorData(topic, payload);
        } else if (shouldRouteAsIrBeamSensor(topic)) {
            messageProcessor.processIrSensorData(topic, payload);
        } else if (shouldRouteAsPirSensor(topic)) {
            messageProcessor.processPirSensorData(topic, payload);
        } else if (shouldRouteAsMmWaveSensor(topic)) {
            messageProcessor.processMmWaveSensorData(topic, payload);
        } else if (shouldRouteAsClimateSensor(topic)) {
            messageProcessor.processSensorData(topic, payload);
        }
    }

    /**
     * 优先内存映射；若新建设备尚未纳入缓存，则一次 DB 命中兜底，避免丢消息。
     */
    private boolean shouldRouteAsIrBeamSensor(String topic) {
        if (irBeamTopicRegistry.isIrBeamTopic(topic)) {
            return true;
        }
        return deviceService.findByMqttTopic(topic)
                .map(IrBeamSideResolver::resolve)
                .filter(s -> s != null && !s.isEmpty())
                .isPresent();
    }

    private boolean shouldRouteAsPirSensor(String topic) {
        if (presenceSensorTopicRegistry.resolve(topic)
                .filter(b -> b.kind() == PresenceSensorTypeResolver.Kind.PIR)
                .isPresent()) {
            return true;
        }
        return deviceService.findByMqttTopic(topic)
                .filter(d -> PresenceSensorTypeResolver.resolve(d) == PresenceSensorTypeResolver.Kind.PIR)
                .isPresent();
    }

    private boolean shouldRouteAsMmWaveSensor(String topic) {
        if (presenceSensorTopicRegistry.resolve(topic)
                .filter(b -> b.kind() == PresenceSensorTypeResolver.Kind.MM_WAVE)
                .isPresent()) {
            return true;
        }
        return deviceService.findByMqttTopic(topic)
                .filter(d -> PresenceSensorTypeResolver.resolve(d) == PresenceSensorTypeResolver.Kind.MM_WAVE)
                .isPresent();
    }

    private boolean shouldRouteAsClimateSensor(String topic) {
        if (climateSensorTopicRegistry.isClimateSensorTopic(topic)) {
            return true;
        }
        return deviceService.findByMqttTopic(topic)
                .filter(ClimateSensorTypeResolver::isClimateSensor)
                .isPresent();
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
        client.subscribe(MqttTopics.LEGACY_IR_SENSOR, 1);
        client.subscribe(PIR_TOPIC, 1);
        client.subscribe(MMWAVE_TOPIC, 1);
        subscribeIrBeamTopicsFromDatabase();
        subscribePresenceSensorTopicsFromDatabase();
        subscribeClimateSensorTopicsFromDatabase();
        System.out.println("已订阅主题: " + TOPIC_SENSOR_STATUS);
        System.out.println("已订阅主题: " + TOPIC_DEVICE_STATUS_ACK);
        System.out.println("已订阅主题: " + MqttTopics.LEGACY_IR_SENSOR + " (兼容旧红外)");
        System.out.println("已订阅主题: " + PIR_TOPIC);
        System.out.println("已订阅主题: " + MMWAVE_TOPIC);
    }

    /**
     * 先刷新 topic→租户缓存，再订阅其中全部 mqtt_topic（与下行 publish 使用同一字符串）。
     */
    private void subscribeIrBeamTopicsFromDatabase() throws MqttException {
        irBeamTopicRegistry.refresh();
        for (IrBeamTopicBinding b : irBeamTopicRegistry.snapshotBindings()) {
            client.subscribe(b.mqttTopic(), 1);
            log.debug(
                    "已订阅红外对射 topic={} (deviceName={}, familyId={})",
                    b.mqttTopic(),
                    b.deviceName(),
                    b.familyId());
        }
    }

    /**
     * 运行期新增/修改 device 表后，无需重启即可尝试订阅新的红外 topic（重复订阅无害）。
     */
    private void subscribePresenceSensorTopicsFromDatabase() throws MqttException {
        presenceSensorTopicRegistry.refresh();
        for (PresenceSensorTopicBinding b : presenceSensorTopicRegistry.snapshotBindings()) {
            client.subscribe(b.mqttTopic(), 1);
            log.debug(
                    "已订阅人体传感器 topic={} kind={} familyId={}",
                    b.mqttTopic(),
                    b.kind(),
                    b.familyId());
        }
    }

    private void subscribeClimateSensorTopicsFromDatabase() throws MqttException {
        climateSensorTopicRegistry.refresh();
        for (ClimateSensorTopicRegistry.ClimateSensorTopicBinding b :
                climateSensorTopicRegistry.snapshotBindings()) {
            if (TOPIC_SENSOR_STATUS.equals(b.mqttTopic())) {
                continue;
            }
            client.subscribe(b.mqttTopic(), 1);
            log.debug(
                    "已订阅温湿度传感器 topic={} familyId={} deviceId={}",
                    b.mqttTopic(),
                    b.familyId(),
                    b.deviceId());
        }
    }

    @Scheduled(fixedRate = 60000)
    public void syncIrBeamSubscriptionsFromDatabase() {
        if (client == null || !client.isConnected()) {
            return;
        }
        try {
            subscribeIrBeamTopicsFromDatabase();
            subscribePresenceSensorTopicsFromDatabase();
            subscribeClimateSensorTopicsFromDatabase();
        } catch (MqttException e) {
            log.warn("同步动态 topic 订阅失败: {}", e.getMessage());
        }
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
    public void publishControlCommand(String deviceName, String action,String devicetype) throws JsonProcessingException {
        if (client == null || !client.isConnected()) {
            throw new RuntimeException("MQTT 客户端未连接，无法发送控制指令。");
        }

        boolean value = action.equalsIgnoreCase("ON");

        try {
            Map<String, Object> payloadMap = new HashMap<>();
            payloadMap.put("deviceType", devicetype);
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


    //固定mqtttopic，直接通过外部传送的topic进行发布，适用于精准推送场景topic
    public void publishToDevice(String topic, String deviceName, String action) throws JsonProcessingException {
        if (client == null || !client.isConnected()) {
            throw new RuntimeException("MQTT 客户端未连接");
        }

        try {
            // 构建 payload，依然保留 deviceName 是为了让硬件多一层判断，更安全
            Map<String, Object> payloadMap = new HashMap<>();
            payloadMap.put("deviceName", deviceName);

            String trimmed = action == null ? "" : action.trim();
            // 空调等场景：AirConditioningService 传入 JSON，例如 {"temp":26.0}
            if (trimmed.startsWith("{")) {
                @SuppressWarnings("unchecked")
                Map<String, Object> parsed = objectMapper.readValue(trimmed, Map.class);
                if (parsed.containsKey("temp")) {
                    Object t = parsed.get("temp");
                    double temp = t instanceof Number ? ((Number) t).doubleValue()
                            : Double.parseDouble(t.toString());
                    payloadMap.put("action", "SET_TEMP");
                    payloadMap.put("value", temp);
                } else {
                    payloadMap.put("value", false);
                }
            } else {
                boolean value = "ON".equalsIgnoreCase(trimmed);
                payloadMap.put("value", value);
            }

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