package com.cms.smart_home_agent.service;

import com.cms.smart_home_agent.entity.Device;
import com.cms.smart_home_agent.presence.PresenceSensorTypeResolver;
import com.cms.smart_home_agent.vo.SensorTriggerVo;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * 记录并查询 PIR / 毫米波按设备维度的最近触发，供前端设备卡片高亮。
 */
@Service
@RequiredArgsConstructor
public class SensorTriggerStateService {

    private static final String KEY_PREFIX = "presence:sensor:last:";
    /** 前端「已触发」高亮保持时长 */
    private static final long ACTIVE_WINDOW_MS = 8000L;
    private static final long REDIS_TTL_SECONDS = 3600L;

    private final StringRedisTemplate stringRedisTemplate;
    private final DeviceService deviceService;

    public void recordTrigger(Integer deviceId, long epochMillis) {
        if (deviceId == null) {
            return;
        }
        stringRedisTemplate
                .opsForValue()
                .set(KEY_PREFIX + deviceId, String.valueOf(epochMillis), REDIS_TTL_SECONDS, TimeUnit.SECONDS);
    }

    public void clearTrigger(Integer deviceId) {
        if (deviceId == null) {
            return;
        }
        stringRedisTemplate.delete(KEY_PREFIX + deviceId);
    }

    public List<SensorTriggerVo> listForFamily(int familyId) {
        long now = System.currentTimeMillis();
        List<SensorTriggerVo> out = new ArrayList<>();
        for (Device d : deviceService.listDevices(familyId)) {
            PresenceSensorTypeResolver.Kind kind = PresenceSensorTypeResolver.resolve(d);
            if (kind == PresenceSensorTypeResolver.Kind.UNKNOWN) {
                continue;
            }
            long triggeredAt = readLastTriggerMs(d.getId());
            boolean active = triggeredAt > 0 && (now - triggeredAt) < ACTIVE_WINDOW_MS;
            String sensorKind =
                    kind == PresenceSensorTypeResolver.Kind.PIR ? "pir" : "mmwave";
            out.add(
                    new SensorTriggerVo(
                            d.getId(),
                            d.getDeviceName(),
                            d.getDeviceType(),
                            sensorKind,
                            triggeredAt,
                            active));
        }
        return out;
    }

    private long readLastTriggerMs(Integer deviceId) {
        if (deviceId == null) {
            return 0L;
        }
        String raw = stringRedisTemplate.opsForValue().get(KEY_PREFIX + deviceId);
        if (raw == null || raw.isBlank()) {
            return 0L;
        }
        try {
            return Long.parseLong(raw.trim());
        } catch (NumberFormatException e) {
            return 0L;
        }
    }
}
