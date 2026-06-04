package com.cms.smart_home_agent.service;


import com.cms.smart_home_agent.climate.ClimateSensorTopicRegistry;
import com.cms.smart_home_agent.climate.ClimateSensorTypeResolver;
import com.cms.smart_home_agent.entity.Device;
import com.cms.smart_home_agent.entity.DeviceStatusData;
import com.cms.smart_home_agent.entity.SensorData;
import com.cms.smart_home_agent.mqtt.MqttTopics;
import com.cms.smart_home_agent.presence.IrBeamSideResolver;
import com.cms.smart_home_agent.presence.IrBeamTopicBinding;
import com.cms.smart_home_agent.presence.IrBeamTopicRegistry;
import com.cms.smart_home_agent.presence.IrPassageResult;
import com.cms.smart_home_agent.presence.PresenceSensorTopicRegistry;
import com.cms.smart_home_agent.presence.PresenceSensorTopicRegistry.PresenceSensorTopicBinding;
import com.cms.smart_home_agent.presence.PresenceSensorTypeResolver;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
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

    // 按家庭缓存最新温湿度（topic → familyId 路由后写入）
    private final ConcurrentHashMap<Integer, SensorData> lastSensorDataByFamily = new ConcurrentHashMap<>();

    @Autowired
    private ClimateSensorTopicRegistry climateSensorTopicRegistry;

    @Autowired
    private PresenceDetectionService presenceDetectionService;

    @Autowired
    private DeviceService deviceService;

    @Autowired
    private IrBeamTopicRegistry irBeamTopicRegistry;

    @Autowired
    private PresenceSensorTopicRegistry presenceSensorTopicRegistry;

    @Autowired
    private SensorTriggerStateService sensorTriggerStateService;

    private final AtomicReference<DeviceStatusData> lastDeviceStatusData = new AtomicReference<>(new DeviceStatusData());
    /**
     * 异步处理温湿度传感器数据 (主题: cms-pub)。
     * 关键: 使用 @Async("mqttTaskExecutor") 绑定到专用线程池。
     * @param topic 消息主题
     * @param payload JSON 字符串负载
     */
    @Async("mqttTaskExecutor")
    public void processSensorData(String topic, String payload) {
        System.out.println("异步线程 [" + Thread.currentThread().getName() + "] 处理「" + topic + "」温湿度数据: " + payload);
        try {
            SensorData data = objectMapper.readValue(payload, SensorData.class);
            Optional<Integer> familyId = resolveClimateSensorFamilyId(topic);
            if (familyId.isEmpty()) {
                log.warn("温湿度消息未绑定到任何家庭（topic={}），已忽略。请在设备表注册 mqtt_topic 且类型为 sensor/thermostat 等", topic);
                return;
            }
            lastSensorDataByFamily.put(familyId.get(), data);
            System.out.printf(
                    "-> 家庭 %d 温湿度已更新: humidity=%.1f, temperature=%.1f (topic=%s)\n",
                    familyId.get(), data.getHumidity(), data.getTemperature(), topic);
        } catch (JsonProcessingException e) {
            System.err.println("解析传感器数据失败: " + e.getMessage());
        }
    }

    private Optional<Integer> resolveClimateSensorFamilyId(String topic) {
        Optional<Integer> fromRegistry =
                climateSensorTopicRegistry.resolve(topic).map(ClimateSensorTopicRegistry.ClimateSensorTopicBinding::familyId);
        if (fromRegistry.isPresent()) {
            return fromRegistry;
        }
        Optional<Device> device = deviceService.findByMqttTopic(topic);
        if (device.isPresent() && ClimateSensorTypeResolver.isClimateSensor(device.get())) {
            return Optional.ofNullable(device.get().getFamilyId());
        }
        return Optional.empty();
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

    /**
     * 红外对射处理：
     * <ul>
     *   <li>推荐：每台设备在 DB 中绑定唯一 {@link Device#getMqttTopic()}，上行发到该 topic；
     *       familyId、门内/门外<strong>仅</strong>来自 {@link IrBeamTopicRegistry}（Topic→租户绑定），
     *       载荷<strong>不参与路由</strong>（可为空、或仅 {@code {"triggered":true}} 等；硬件无需再传 familyId/sensorId）。</li>
     *   <li>兼容：主题仍为 {@link MqttTopics#LEGACY_IR_SENSOR} 时，载荷需提供 sensorId + familyId。</li>
     * </ul>
     */
    @Async("mqttTaskExecutor")
    public void processIrSensorData(String topic, String payload) {
        log.info("异步线程 [{}] 处理「{}」红外传感器数据: {}", Thread.currentThread().getName(), topic, payload);
        try {
            if (MqttTopics.LEGACY_IR_SENSOR.equals(topic)) {
                handleLegacyIrSensorPayload(payload);
                return;
            }

            Optional<IrBeamTopicBinding> bound = irBeamTopicRegistry.resolve(topic);
            if (bound.isPresent()) {
                IrBeamTopicBinding b = bound.get();
                IrPassageResult result = presenceDetectionService.handleIrBeam(b.deviceType(), b.familyId());
                logIrPassageResult(b.familyId(), result);
                return;
            }

            Device device = deviceService.findByMqttTopic(topic).orElse(null);
            if (device == null) {
                log.warn("红外上报主题未在缓存也未在 device 表注册: {}", topic);
                return;
            }
            String side = IrBeamSideResolver.resolve(device);
            if (side == null) {
                log.warn(
                        "设备无法解析门内/门外（请在 deviceType 或 deviceName 中标明门内/门外等关键字） id={} name={} type={}",
                        device.getId(),
                        device.getDeviceName(),
                        device.getDeviceType());
                return;
            }
            if (device.getFamilyId() == null) {
                log.warn("device.family_id 为空 id={}", device.getId());
                return;
            }
            log.warn(
                    "红外 Topic 未命中内存映射，已回退数据库（建议确认 IrBeamTopicRegistry 已刷新） topic={} deviceId={}",
                    topic,
                    device.getId());
            IrPassageResult result = presenceDetectionService.handleIrBeam(side, device.getFamilyId());
            logIrPassageResult(device.getFamilyId(), result);
        } catch (JsonProcessingException e) {
            log.error("解析红外载荷失败 topic={}: {}", topic, e.getMessage());
        }
    }

    private void handleLegacyIrSensorPayload(String payload) throws JsonProcessingException {
        @SuppressWarnings("unchecked")
        Map<String, Object> data = objectMapper.readValue(payload, Map.class);
        String sensorId = data.get("sensorId") == null ? null : data.get("sensorId").toString();
        Integer familyId = toInteger(data.get("familyId"));
        if (sensorId == null || familyId == null) {
            log.warn("「{}」载荷缺少 sensorId/familyId: {}", MqttTopics.LEGACY_IR_SENSOR, payload);
            return;
        }
        IrPassageResult result = presenceDetectionService.handleIrBeam(sensorId, familyId);
        logIrPassageResult(familyId, result);
    }

    private void logIrPassageResult(Integer familyId, IrPassageResult result) {
        switch (result) {
            case ENTRY -> log.info("判定结果：【进入】family={}", familyId);
            case EXIT -> log.info("判定结果：【离开】family={}", familyId);
            case PENDING, DISCARDED_RESTART ->
                    log.debug("红外：等待对侧或已超时重启待定 family={} result={}", familyId, result);
            case INVALID -> log.warn("红外：无效序列或参数 family={}", familyId);
        }
    }

    /**
     * PIR 人体红外：推荐每台设备独立 mqtt_topic，载荷 {@code {"motion":true}} 即可；
     * familyId 仅来自 {@link PresenceSensorTopicRegistry} 或 device 表（兼容旧主题 cms-pir-sensor 的载荷 familyId）。
     */
    @Async("mqttTaskExecutor")
    public void processPirSensorData(String topic, String payload) {
        log.info("异步线程 [{}] 处理「{}」PIR 数据: {}", Thread.currentThread().getName(), topic, payload);
        try {
            Optional<PresenceSensorTopicBinding> bound =
                    resolvePresenceBinding(topic, PresenceSensorTypeResolver.Kind.PIR);
            if (bound.isPresent()) {
                boolean motion = parseMotionFlag(payload);
                long ts = parseTimestampMs(payload);
                if (motion) {
                    presenceDetectionService.onPirReport(bound.get().familyId(), true, ts);
                    recordSensorTrigger(bound.get().deviceId(), ts);
                }
                return;
            }
            @SuppressWarnings("unchecked") // 兼容旧主题，载荷需提供 familyId 字段
            Map<String, Object> data = objectMapper.readValue(payload, Map.class);
            Integer familyId = toInteger(data.get("familyId"));
            if (familyId == null) {
                log.warn("PIR 主题未注册且载荷缺少 familyId: topic={} payload={}", topic, payload);
                return;
            }
            Object motionObj = data.get("motion");
            boolean motion = motionObj instanceof Boolean b
                    ? b
                    : motionObj != null && Boolean.parseBoolean(motionObj.toString());
            long ts = System.currentTimeMillis();
            if (data.get("ts") instanceof Number n) {
                ts = n.longValue();
            }
            if (motion) {
                presenceDetectionService.onPirReport(familyId, true, ts);
                long finalTs = ts;
                deviceService.findByMqttTopic(topic).ifPresent(d -> recordSensorTrigger(d.getId(), finalTs));
            }
        } catch (JsonProcessingException e) {
            log.error("解析 PIR 失败: {}", e.getMessage());
        }
    }

    /**
     * 毫米波（LD2410 等）：独立 topic + {@code {"present":true}}；需 presence.mm-wave-enabled=true。
     */
    @Async("mqttTaskExecutor")
    public void processMmWaveSensorData(String topic, String payload) {
        log.info("异步线程 [{}] 处理「{}」毫米波数据: {}", Thread.currentThread().getName(), topic, payload);
        try {
            Optional<PresenceSensorTopicBinding> bound =
                    resolvePresenceBinding(topic, PresenceSensorTypeResolver.Kind.MM_WAVE);
            if (bound.isPresent()) {
                boolean present = parsePresentFlag(payload);
                long ts = parseTimestampMs(payload);
                if (present) {
                    presenceDetectionService.onMmWavePresence(bound.get().familyId(), true, ts);
                    recordSensorTrigger(bound.get().deviceId(), ts);
                }
                return;
            }
            @SuppressWarnings("unchecked")
            Map<String, Object> data = objectMapper.readValue(payload, Map.class);
            Integer familyId = toInteger(data.get("familyId"));
            if (familyId == null) {
                log.warn("毫米波主题未注册且载荷缺少 familyId: topic={} payload={}", topic, payload);
                return;
            }
            Object presentObj = data.containsKey("present") ? data.get("present") : data.get("humanPresent");
            boolean present = presentObj instanceof Boolean b
                    ? b
                    : presentObj != null && Boolean.parseBoolean(presentObj.toString());
            long ts = System.currentTimeMillis();
            if (data.get("ts") instanceof Number n) {
                ts = n.longValue();
            }
            if (present) {
                presenceDetectionService.onMmWavePresence(familyId, true, ts);
                long finalTs = ts;
                deviceService.findByMqttTopic(topic).ifPresent(d -> recordSensorTrigger(d.getId(), finalTs));
            }
        } catch (JsonProcessingException e) {
            log.error("解析毫米波载荷失败: {}", e.getMessage());
        }
    }

    private void recordSensorTrigger(Integer deviceId, long epochMillis) {
        sensorTriggerStateService.recordTrigger(deviceId, epochMillis);
    }

    private Optional<PresenceSensorTopicBinding> resolvePresenceBinding(
            String topic, PresenceSensorTypeResolver.Kind expectedKind) {
        Optional<PresenceSensorTopicBinding> bound = presenceSensorTopicRegistry.resolve(topic);
        if (bound.isPresent()) {
            if (bound.get().kind() != expectedKind) {
                log.warn("Topic 注册为 {}，但按 {} 处理 topic={}", bound.get().kind(), expectedKind, topic);
                return Optional.empty();
            }
            return bound;
        }
        Device device = deviceService.findByMqttTopic(topic).orElse(null);
        if (device == null || device.getFamilyId() == null) {
            return Optional.empty();
        }
        if (PresenceSensorTypeResolver.resolve(device) != expectedKind) {
            return Optional.empty();
        }
        log.warn("人体传感器 Topic 未命中缓存，已回退数据库 topic={} deviceId={}", topic, device.getId());
        return Optional.of(
                new PresenceSensorTopicBinding(
                        topic, device.getFamilyId(), expectedKind, device.getId()));
    }

    /** 有消息即视为触发；空载荷或仅 triggered 时按 true 处理。 */
    private boolean parseMotionFlag(String payload) {
        if (payload == null || payload.isBlank()) {
            return true;
        }
        try {
            @SuppressWarnings("unchecked")
            Map<String, Object> data = objectMapper.readValue(payload, Map.class);
            if (!data.containsKey("motion")) {
                return true;
            }
            Object motionObj = data.get("motion");
            return motionObj instanceof Boolean b
                    ? b
                    : motionObj != null && Boolean.parseBoolean(motionObj.toString());
        } catch (JsonProcessingException e) {
            return true;
        }
    }

    private boolean parsePresentFlag(String payload) {
        if (payload == null || payload.isBlank()) {
            return true;
        }
        try {
            @SuppressWarnings("unchecked")
            Map<String, Object> data = objectMapper.readValue(payload, Map.class);
            Object presentObj = data.containsKey("present") ? data.get("present") : data.get("humanPresent");
            if (presentObj == null) {
                return true;
            }
            return presentObj instanceof Boolean b
                    ? b
                    : Boolean.parseBoolean(presentObj.toString());
        } catch (JsonProcessingException e) {
            return true;
        }
    }

    private long parseTimestampMs(String payload) {
        if (payload == null || payload.isBlank()) {
            return System.currentTimeMillis();
        }
        try {
            @SuppressWarnings("unchecked")
            Map<String, Object> data = objectMapper.readValue(payload, Map.class);
            if (data.get("ts") instanceof Number n) {
                return n.longValue();
            }
        } catch (JsonProcessingException ignored) {
            // 无 ts 字段
        }
        return System.currentTimeMillis();
    }

    private static Integer toInteger(Object o) {
        if (o == null) {
            return null;
        }
        if (o instanceof Number n) {
            return n.intValue();
        }
        try {
            return Integer.parseInt(o.toString().trim());
        } catch (NumberFormatException e) {
            return null;
        }
    }

    // 提供获取最新状态的方法
    public SensorData getLastSensorData(Integer familyId) {
        if (familyId == null) {
            return new SensorData();
        }
        return lastSensorDataByFamily.getOrDefault(familyId, new SensorData());
    }

    /** @deprecated 请使用 {@link #getLastSensorData(Integer)} */
    public SensorData getLastSensorData() {
        return new SensorData();
    }

    public DeviceStatusData getLastDeviceStatusData() {
        return lastDeviceStatusData.get();
    }
}