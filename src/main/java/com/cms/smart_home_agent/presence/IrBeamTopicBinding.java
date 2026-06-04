package com.cms.smart_home_agent.presence;

/**
 * 红外对射 topic 与租户（家庭）、物理侧（门内/门外）的绑定快照，来源仅为数据库，不可由 MQTT 载荷覆盖。
 */
public record IrBeamTopicBinding(
        String mqttTopic,
        int familyId,
        String deviceType,
        Integer deviceId,
        String deviceName
) {}
