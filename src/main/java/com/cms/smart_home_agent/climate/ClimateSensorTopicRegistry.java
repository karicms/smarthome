package com.cms.smart_home_agent.climate;

import com.cms.smart_home_agent.entity.Device;
import com.cms.smart_home_agent.service.DeviceService;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicReference;

/**
 * Topic → 家庭温湿度传感器绑定（上行按 mqtt_topic 路由到对应 familyId）。
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class ClimateSensorTopicRegistry {

    public record ClimateSensorTopicBinding(String mqttTopic, Integer familyId, Integer deviceId) {}

    private final DeviceService deviceService;

    private final AtomicReference<Map<String, ClimateSensorTopicBinding>> topicCache =
            new AtomicReference<>(Map.of());

    @PostConstruct
    public void initCache() {
        refresh();
    }

    @Scheduled(fixedRate = 60000)
    public void scheduledRefresh() {
        refresh();
    }

    public List<ClimateSensorTopicBinding> snapshotBindings() {
        return List.copyOf(topicCache.get().values());
    }

    public void refresh() {
        Map<String, ClimateSensorTopicBinding> next = new HashMap<>();
        for (Device d : deviceService.listClimateSensorDevices()) {
            String topic = d.getMqttTopic();
            if (topic == null || topic.isBlank() || d.getFamilyId() == null) {
                continue;
            }
            ClimateSensorTopicBinding binding =
                    new ClimateSensorTopicBinding(topic, d.getFamilyId(), d.getId());
            ClimateSensorTopicBinding existing = next.put(topic, binding);
            if (existing != null) {
                log.error(
                        "温湿度 mqtt_topic 重复绑定 topic={} 已有 deviceId={} 冲突 deviceId={}",
                        topic,
                        existing.deviceId(),
                        d.getId());
            }
        }
        topicCache.set(Map.copyOf(next));
        log.debug("ClimateSensorTopicRegistry 已刷新，条数={}", next.size());
    }

    public boolean isClimateSensorTopic(String mqttTopic) {
        return mqttTopic != null && topicCache.get().containsKey(mqttTopic);
    }

    public Optional<ClimateSensorTopicBinding> resolve(String mqttTopic) {
        if (mqttTopic == null) {
            return Optional.empty();
        }
        return Optional.ofNullable(topicCache.get().get(mqttTopic));
    }
}
