package com.cms.smart_home_agent.service;

import com.cms.smart_home_agent.config.PresenceProperties;
import com.cms.smart_home_agent.entity.Device;
import com.cms.smart_home_agent.vo.AwayStatusVo;
import com.cms.smart_home_agent.presence.IrPassageResult;
import com.cms.smart_home_agent.presence.MmWaveOccupancyAdapter;
import com.fasterxml.jackson.core.JsonProcessingException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Lazy;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.util.Locale;
import java.util.Set;
import java.util.concurrent.TimeUnit;

/**
 * 室内人员估计 + 红外顺序通过 + PIR/毫米波动静 + 离家后静默超时自动化。
 * <p>
 * FSM 要点（每人维度简化为估计人数 {@code estCount} + Redis ZSET 离家截止时间）：
 * <ul>
 *   <li>有效通过：仅当同一家庭先后触发两处对射，且间隔 ≤ {@link PresenceProperties#getIrMaxGapMs()}</li>
 *   <li>出门（IN→OUT）：估计人数 −1（下限 0），并写入「离家截止时间」</li>
 *   <li>进门（OUT→IN）：估计人数 +1，并取消离家截止时间</li>
 *   <li>PIR/毫米波动静：取消离家截止时间（表示室内仍有活动迹象）</li>
 *   <li>截止时间到期：判定家中无人，按需 MQTT 批量关断</li>
 * </ul>
 */
@Slf4j
@Service
public class PresenceDetectionService implements MmWaveOccupancyAdapter {

    private static final String IR_PENDING_PREFIX = "presence:ir:pending:";
    private static final String AWAY_DEADLINES_ZSET = "presence:away:deadlines";
    private static final String META_PREFIX = "presence:meta:";

    private final StringRedisTemplate stringRedisTemplate;
    private final PresenceProperties presenceProperties;
    private final DeviceService deviceService;
    private final MqttService mqttService;

    public PresenceDetectionService(
            StringRedisTemplate stringRedisTemplate,
            PresenceProperties presenceProperties,
            DeviceService deviceService,
            @Lazy MqttService mqttService) {
        this.stringRedisTemplate = stringRedisTemplate;
        this.presenceProperties = presenceProperties;
        this.deviceService = deviceService;
        this.mqttService = mqttService;
    }

    /**
     * 红外对射：{@code sensorId} 建议使用 IN（门内）/ OUT（门外），兼容别名。
     */
    public IrPassageResult handleIrBeam(String sensorId, int familyId) {
        String side = normalizeBeamSide(sensorId); // 规范化为 IN/OUT，无法识别时返回 null
        if (side == null) {
            log.warn("无法识别的红外 sensorId={}", sensorId);
            return IrPassageResult.INVALID;
        }

        String key = IR_PENDING_PREFIX + familyId; // 每个家庭一个 key
        String pending = stringRedisTemplate.opsForValue().get(key); // 可能的值格式："{deviceType}:{timestamp}"
        long now = System.currentTimeMillis();

        if (pending == null) {
            stringRedisTemplate.opsForValue().set(
                    key,
                    side + ":" + now,
                    presenceProperties.getIrPendingTtlSeconds(),
                    TimeUnit.SECONDS); // 记录第一触发事件，等待配对
            log.debug("IR 第一触发 family={} deviceType={}", familyId, side);
            return IrPassageResult.PENDING; // 等待配对
        }

        int colon = pending.indexOf(':');
        if (colon <= 0 || colon >= pending.length() - 1) {
            stringRedisTemplate.delete(key);
            return handleIrBeam(sensorId, familyId);
        }
        String firstSide = pending.substring(0, colon).trim();
        long firstTs;
        try {
            firstTs = parseEpochMillisFlexible(pending.substring(colon + 1).trim());
        } catch (NumberFormatException e) {
            log.warn("IR pending 时间戳无法解析，已重置 family={} raw={}", familyId, pending);
            stringRedisTemplate.delete(key);
            return handleIrBeam(sensorId, familyId);
        }

        long deltaMs = now - firstTs;
        long maxGap = presenceProperties.getIrMaxGapMs();
        if (deltaMs > maxGap) {
            stringRedisTemplate.delete(key);
            stringRedisTemplate.opsForValue().set(
                    key,
                    side + ":" + now,
                    presenceProperties.getIrPendingTtlSeconds(),
                    TimeUnit.SECONDS);
            log.info(
                    "IR 配对超时，重新以当前触发为第一事件 family={} side={} pendingRaw={} nowMs={} firstTsMs={} deltaMs={} maxGapMs={}",
                    familyId,
                    side,
                    pending,
                    now,
                    firstTs,
                    deltaMs,
                    maxGap);
            return IrPassageResult.DISCARDED_RESTART;
        }

        if (firstSide.equals(side)) {
            stringRedisTemplate.opsForValue().set(
                    key,
                    side + ":" + now,
                    presenceProperties.getIrPendingTtlSeconds(), // 重置 TTL
                    TimeUnit.SECONDS);
            return IrPassageResult.PENDING;
        }

        stringRedisTemplate.delete(key);

        // 门外 -> 门内 => 进入
        if ("OUT".equals(firstSide) && "IN".equals(side)) {
            onPassageEntry(familyId);
            log.info("IR 序列 OUT→IN ⇒ ENTRY family={}", familyId);
            return IrPassageResult.ENTRY;
        }
        // 门内 -> 门外 => 离开
        if ("IN".equals(firstSide) && "OUT".equals(side)) {
            onPassageExit(familyId);
            log.info("IR 序列 IN→OUT ⇒ EXIT family={}", familyId);
            return IrPassageResult.EXIT;
        }

        return IrPassageResult.INVALID;
    }

    /**
     * PIR 周期性上报：{@code motion true} 表示检测到移动。
     */
    public void onPirReport(int familyId, boolean motion, long epochMillis) {
        if (!motion) {
            return;
        }
        onInteriorMotion(familyId, "pir", epochMillis);
    }

    /**
     * 毫米波存在上报（相对 PIR 更可检出静止人体）。
     */
    public void onMmWavePresence(int familyId, boolean humanPresent, long epochMillis) {
        if (!presenceProperties.isMmWaveEnabled()) {
            return;
        }
        if (humanPresent) {
            onInteriorMotion(familyId, "mmwave", epochMillis);
        }
    }

    @Override
    public void reportOccupancy(int familyId, boolean humanPresent, long epochMillis, double confidenceHint) {
        onMmWavePresence(familyId, humanPresent, epochMillis); // 适配毫米波存在报告接口，忽略 confidenceHint
    }

    //记录室内动静，取消离家截止时间（表示室内仍有活动迹象），并记录动静来源（PIR、毫米波等）
    private void onInteriorMotion(int familyId, String source, long epochMillis) {
        Long removed = stringRedisTemplate.opsForZSet().remove(AWAY_DEADLINES_ZSET, String.valueOf(familyId)); // 取消离家截止时间
        stringRedisTemplate.opsForHash().put(
                META_PREFIX + familyId,
                "lastMotionMs",
                String.valueOf(epochMillis)); // 记录最后一次动静时间
        stringRedisTemplate.opsForHash().put(
                META_PREFIX + familyId,
                "lastMotionSource",
                source); // 记录动静来源（PIR、毫米波等）
        if (removed != null && removed > 0) {
            log.info("室内动静来源={}，已取消离家待定 family={} epoch={}", source, familyId, epochMillis);
        }
    }

    //有人进入家庭，估计人数 +1，并取消离家截止时间
    private void onPassageEntry(int familyId) {
        bumpEstimatedCount(familyId, +1);
        stringRedisTemplate.opsForZSet().remove(AWAY_DEADLINES_ZSET, String.valueOf(familyId)); // 取消离家截止时间
    }

    //有人离开家庭，估计人数 −1（下限 0），并写入「离家截止时间」
    private void onPassageExit(int familyId) {
        bumpEstimatedCount(familyId, -1);
        long deadline = System.currentTimeMillis() + presenceProperties.getPirQuietTimeout().toMillis();
        stringRedisTemplate.opsForZSet().add(AWAY_DEADLINES_ZSET, String.valueOf(familyId), deadline);
        stringRedisTemplate.opsForHash().put(
                META_PREFIX + familyId,
                "lastExitMs",
                String.valueOf(System.currentTimeMillis()));
        log.info("进入离家待定：family={} 截止时间 epochMs={}", familyId, deadline);
    }

    // 更新家庭成员数量估计值
    private void bumpEstimatedCount(int familyId, int delta) {
        String hk = META_PREFIX + familyId; // 估计人数存储在 hash 的 "estCount" 字段
        Object raw = stringRedisTemplate.opsForHash().get(hk, "estCount");
        String cur = raw == null ? "0" : raw.toString(); //看看estCount是否有值，没有就当0
        int v;
        try {
            v = Integer.parseInt(cur); // 解析当前估计人数，若格式异常则重置为 0
        } catch (NumberFormatException e) {
            v = 0;
        }
        v = Math.max(0, v + delta);
        stringRedisTemplate.opsForHash().put(hk, "estCount", String.valueOf(v)); // 更新估计人数
    }

    @Scheduled(fixedDelayString = "${presence.scan-away-fixed-delay-ms:30000}")
    public void scanAwayDeadlines() {
        long now = System.currentTimeMillis();
        Set<String> due = stringRedisTemplate.opsForZSet().rangeByScore(AWAY_DEADLINES_ZSET, 0, now);
        if (due == null || due.isEmpty()) {
            return;
        }
        for (String fid : due) {
            stringRedisTemplate.opsForZSet().remove(AWAY_DEADLINES_ZSET, fid);
            int familyId = Integer.parseInt(fid);
            confirmNobodyHomeAndShutDown(familyId);
        }
    }

    private void confirmNobodyHomeAndShutDown(int familyId) {
        applyAwayConfirmedState(familyId, "auto");
        log.warn("离家确认（静默超时）family={}，开始批量关断灯和门", familyId);
        turnOffLightsAndDoors(familyId);
    }

    /**
     * 前端手动触发离家模式：与自动确认离家共用关灯关门逻辑。
     */
    public int triggerAwayManually(int familyId) {
//        applyAwayConfirmedState(familyId, "manual");
        log.info("前端手动触发离家模式 family={}", familyId);
        return turnOffLightsAndDoors(familyId);
    }

    private void applyAwayConfirmedState(int familyId, String triggerSource) {
        String hk = META_PREFIX + familyId;
        stringRedisTemplate.opsForZSet().remove(AWAY_DEADLINES_ZSET, String.valueOf(familyId));
        stringRedisTemplate.opsForHash().put(hk, "estCount", "0");
        stringRedisTemplate.opsForHash().put(hk, "lastAwayConfirmMs", String.valueOf(System.currentTimeMillis()));
        stringRedisTemplate.opsForHash().put(hk, "lastAwayTrigger", triggerSource);
    }

    /** 离家确认后仅关断灯与门（传感器、空调等不参与），返回实际下发关断指令的设备数 */
    private int turnOffLightsAndDoors(int familyId) {
        int count = 0;
        for (Device d : deviceService.listDevices(familyId)) {
            if (d.getMqttTopic() == null || d.getMqttTopic().isEmpty()) {
                continue;
            }
            String type = d.getDeviceType() == null ? "" : d.getDeviceType().toLowerCase(Locale.ROOT);
            boolean isLight = isLightType(type);
            boolean isDoor = isDoorType(type);
            if (!isLight && !isDoor) {
                continue;
            }
            if (isDoor && !presenceProperties.isAwayTurnOffDoor()) {
                log.debug("离家关断跳过门锁类 device={}", d.getDeviceName());
                continue;
            }
            try {
                mqttService.publishToDevice(d.getMqttTopic(), d.getDeviceName(), "OFF");
                log.info("离家关断 device={} type={} topic={}", d.getDeviceName(), type, d.getMqttTopic());
                count++;
            } catch (JsonProcessingException e) {
                log.error("MQTT 关断失败 family={} device={}", familyId, d.getDeviceName(), e);
            }
        }
        return count;
    }

    private static boolean isLightType(String typeLower) {
        return "led".equals(typeLower) || "light".equals(typeLower);
    }

    private static boolean isDoorType(String typeLower) {
        return typeLower.contains("door") || typeLower.contains("门")
                || typeLower.contains("lock") || typeLower.contains("锁");
    }

    /**
     * 读取离家自动化状态（待定 / 已确认 / 在家）。
     */
    public AwayStatusVo getAwayStatus(int familyId) {
        AwayStatusVo vo = new AwayStatusVo();
        vo.setEstimatedOccupancy(getEstimatedOccupancy(familyId));

        Double deadline = stringRedisTemplate.opsForZSet().score(AWAY_DEADLINES_ZSET, String.valueOf(familyId));
        long now = System.currentTimeMillis();
        if (deadline != null && deadline > now) {
            vo.setState("away_pending");
            vo.setAwayDeadlineMs(deadline.longValue());
        } else {
            vo.setState("home");
        }

        String hk = META_PREFIX + familyId;
        Object lastConfirm = stringRedisTemplate.opsForHash().get(hk, "lastAwayConfirmMs");
        if (lastConfirm != null) {
            try {
                vo.setLastAwayConfirmMs(Long.parseLong(lastConfirm.toString()));
            } catch (NumberFormatException ignored) {
                // ignore malformed value
            }
        }

        if (!"away_pending".equals(vo.getState())
                && vo.getLastAwayConfirmMs() != null
                && vo.getEstimatedOccupancy() != null
                && vo.getEstimatedOccupancy() == 0) {
            vo.setState("away_confirmed");
        }

        return vo;
    }

    /**
     * 解析 Redis 中存储的时间戳：支持毫秒（13 位左右）；若为秒级 Unix 时间（常见于误写 / 旧数据），自动 ×1000。
     */
    private static long parseEpochMillisFlexible(String tsPart) {
        String s = tsPart.trim();
        long v = Long.parseLong(s);
        if (v > 0 && v < 10_000_000_000L) {
            return v * 1000L;
        }
        return v;
    }

    private static String normalizeBeamSide(String sensorId) {
        if (sensorId == null) {
            return null;
        }
        String s = sensorId.trim(); // 先去掉首尾空白
        if (s.isEmpty()) {
            return null;
        }
        String u = s.toUpperCase(Locale.ROOT);
        if ("IN".equals(u) || "门内".equals(s) || "内".equals(s)) {
            return "IN";
        }
        if ("OUT".equals(u) || "门外".equals(s) || "外".equals(s)) {
            return "OUT";
        }
        return null;
    }

    /**
     * 读取 Redis {@code presence:meta:{familyId}} 中 {@code estCount}，供前端展示「估计在宅人数」。
     */
    public int getEstimatedOccupancy(int familyId) {
        String hk = META_PREFIX + familyId;
        Object raw = stringRedisTemplate.opsForHash().get(hk, "estCount");
        if (raw == null) {
            return 0;
        }
        try {
            return Math.max(0, Integer.parseInt(raw.toString()));
        } catch (NumberFormatException e) {
            return 0;
        }
    }
}
