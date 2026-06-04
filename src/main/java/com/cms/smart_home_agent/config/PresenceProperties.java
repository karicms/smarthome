package com.cms.smart_home_agent.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

import java.time.Duration;

/**
 * 室内人员 / 离家判定相关配置。
 */
@Data
@ConfigurationProperties(prefix = "presence")
public class PresenceProperties {

    /**
     * 门内、门外两次红外触发允许的最大间隔（先后次序已在逻辑中固定）。
     */
    private long irMaxGapMs = 10000L;

    /**
     * 第一次触发后等待第二次触发的 Redis 键存活时间（应略大于 {@link #irMaxGapMs}）。
     */
    private long irPendingTtlSeconds = 15L;

    /**
     * 判定为「出门」后，若在此时间内未观察到室内动静，则确认离家。
     */
    private Duration pirQuietTimeout = Duration.ofMinutes(8); // 8 分钟

    /**
     * 是否把毫米波存在信号与 PIR 同等对待（取消离家待定、刷新动静时间）。
     */
    private boolean mmWaveEnabled = false;

    /**
     * 离家确认后是否连门锁类设备一同下发关闭（默认否，避免误锁）。
     */
    private boolean awayTurnOffDoor = false;

    /**
     * 离家后批量关断时是否跳过空调（仅示例开关，可按家庭扩展）。
     */
    private boolean awaySkipAc = false;
}
