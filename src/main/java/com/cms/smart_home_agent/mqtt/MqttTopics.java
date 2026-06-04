package com.cms.smart_home_agent.mqtt;

/**
 * 全局 MQTT 主题常量（与  区分：
 * 后者为库表中每台设备绑定的下行/上行通道）。
 */
public final class MqttTopics {

    /** 旧版红外统一上报主题（载荷内需 sensorId + familyId），建议迁移为「每设备独立 topic」 */
    public static final String LEGACY_IR_SENSOR = "cms-ir-sensor";

    private MqttTopics() {}
}
