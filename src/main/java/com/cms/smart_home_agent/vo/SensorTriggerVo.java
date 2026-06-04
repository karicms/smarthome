package com.cms.smart_home_agent.vo;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 供前端展示 PIR / 毫米波最近一次触发（轮询拉取）。
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class SensorTriggerVo {
    private Integer deviceId;
    private String deviceName;
    private String deviceType;
    /** pir / mmwave */
    private String sensorKind;
    /** 最近一次触发时间戳（毫秒），无记录为 0 */
    private long triggeredAt;
    /** 是否在展示窗口内（前端高亮「已触发」） */
    private boolean active;
}
