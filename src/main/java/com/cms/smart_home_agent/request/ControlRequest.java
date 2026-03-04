package com.cms.smart_home_agent.request;

import lombok.Data;

/**
 * 设备控制请求 DTO
 * 对应前端 toggleDevice 接口发送的数据
 */
@Data
public class ControlRequest {
    // 设备名称: "led" 或 "buzzer"
    private String devicename;
    // 预期操作: "on" 或 "off"
    private String action;

    // 状态值: true 或 false
    private boolean value;
}
