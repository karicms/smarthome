package com.cms.smart_home_agent.presence;

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
 * Topic → familyId + 传感器种类（PIR / 毫米波），上行载荷无需 familyId。
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class PresenceSensorTopicRegistry {

    public record PresenceSensorTopicBinding(
            String mqttTopic, Integer familyId, PresenceSensorTypeResolver.Kind kind, Integer deviceId) {}

    private final DeviceService deviceService;

    private final AtomicReference<Map<String, PresenceSensorTopicBinding>> topicCache =
            new AtomicReference<>(Map.of()); //用于在程序启动时创建一个原子性的可空的缓存，后续查到数据库的device之后，通过resolver分析出传感器种类，然后包装成topicbinding存入map

    @PostConstruct
    public void initCache() {
        refresh();
    }

    @Scheduled(fixedRate = 60000)
    public void scheduledRefresh() {
        refresh();
    }

    public List<PresenceSensorTopicBinding> snapshotBindings() {
        return List.copyOf(topicCache.get().values());
    } //用于获取当前缓存中的全部绑定，返回一个不可变的列表，避免外部修改缓存内容

    public void refresh() {
        Map<String, PresenceSensorTopicBinding> next = new HashMap<>();
        for (Device d : deviceService.listPresenceSensorDevices()) {
            String topic = d.getMqttTopic();
            if (topic == null || topic.isBlank()) {
                continue;
            }
            PresenceSensorTypeResolver.Kind kind = PresenceSensorTypeResolver.resolve(d);
            if (kind == PresenceSensorTypeResolver.Kind.UNKNOWN) {
                continue;
            }
            if (d.getFamilyId() == null) {
                log.warn("人体传感器缺少 family_id，已跳过 deviceId={}", d.getId());
                continue;
            }
            PresenceSensorTopicBinding binding =
                    new PresenceSensorTopicBinding(topic, d.getFamilyId(), kind, d.getId());
            PresenceSensorTopicBinding existing = next.put(topic, binding);
            if (existing != null) {
                log.error(
                        "mqtt_topic 重复绑定 topic={} 已有 deviceId={} 冲突 deviceId={}",
                        topic,
                        existing.deviceId(),
                        d.getId());
            }
        }
        topicCache.set(Map.copyOf(next));
        log.debug("PresenceSensorTopicRegistry 已刷新，条数={}", next.size());
    }

    public boolean isPresenceSensorTopic(String mqttTopic) {
        return mqttTopic != null && topicCache.get().containsKey(mqttTopic);
    }

    public Optional<PresenceSensorTopicBinding> resolve(String mqttTopic) {
        if (mqttTopic == null) {
            return Optional.empty();
        }
        return Optional.ofNullable(topicCache.get().get(mqttTopic));
    }
}
