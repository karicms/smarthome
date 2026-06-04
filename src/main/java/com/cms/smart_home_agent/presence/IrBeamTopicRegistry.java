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
 * 多租户安全：仅以库表中的 {@link Device#getMqttTopic()} 为键，缓存 {@code familyId + IN/OUT}，
 * 消息到达时凭 Topic 解析身份，不信任载荷中的 familyId。
 * <p>
 * 建议在数据库对 {@code mqtt_topic} 建唯一索引（或至少索引），见 SQL 注释。
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class IrBeamTopicRegistry {

    /**
     * -- 建议在 MySQL 执行（按你实际表名调整）：
     * -- CREATE UNIQUE INDEX uk_device_mqtt_topic ON device(mqtt_topic);
     */
    private final DeviceService deviceService;

    private final AtomicReference<Map<String, IrBeamTopicBinding>> topicCache =
            new AtomicReference<>(Map.of()); //用于在程序启动时创建一个原子性的可空的缓存，后续查到数据库的device之后，通过resolver分析出方位，然后包装成topicbinding存入map

    @PostConstruct // 启动时加载一次，后续靠定时任务增量刷新（管理员新增/变更设备后无需重启）
    public void initCache() {
        refresh();
    }

    /** 定期与 DB 对齐（管理员新增/变更设备后无需重启）。 */
    @Scheduled(fixedRate = 60000)
    public void scheduledRefresh() {
        refresh();
    }

    /** 当前缓存中的全部绑定（用于 MQTT 订阅列表，与 {@link #refresh()} 结果一致）。 */
    public List<IrBeamTopicBinding> snapshotBindings() {
        return List.copyOf(topicCache.get().values());
    }

    /**
     * 全量重建缓存（原子替换整个 Map，读路径无锁）。
     */
    public void refresh() {
        Map<String, IrBeamTopicBinding> next = new HashMap<>();
        for (Device d : deviceService.listIrBeamDevices()) {
            String topic = d.getMqttTopic();
            if (topic == null || topic.isBlank()) {
                continue;
            }
            String side = IrBeamSideResolver.resolve(d); //判断是门内还是门外
            if (side == null) {
                continue;
            }
            if (d.getFamilyId() == null) {
                log.warn("红外设备缺少 family_id，已跳过 deviceId={}", d.getId());
                continue;
            }
            IrBeamTopicBinding binding =
                    new IrBeamTopicBinding(topic, d.getFamilyId(), side, d.getId(), d.getDeviceName());
            IrBeamTopicBinding existing = next.put(topic, binding);
            if (existing != null) {
                log.error(
                        "mqtt_topic 重复绑定，请数据库排查唯一性 topic={} 已有 deviceId={} 冲突 deviceId={}",
                        topic,
                        existing.deviceId(),
                        d.getId());
            }
        }
        topicCache.set(Map.copyOf(next));
        log.debug("IrBeamTopicRegistry 已刷新，红外对射 topic 条数={}", next.size());
    }

    /** 是否为本系统注册的红外对射上行 topic（用于 MQTT 路由，避免每条消息查库）。 */
    public boolean isIrBeamTopic(String mqttTopic) {
        return mqttTopic != null && topicCache.get().containsKey(mqttTopic);
    }

    /**
     * 凭 Topic 解析租户与侧；载荷中的 familyId 不得覆盖此处结果。
     */
    public Optional<IrBeamTopicBinding> resolve(String mqttTopic) {
        if (mqttTopic == null) {
            return Optional.empty();
        }
        return Optional.ofNullable(topicCache.get().get(mqttTopic));
    }
}
