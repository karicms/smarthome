package com.cms.smart_home_agent.entity;


import lombok.Data;

import java.io.Serializable;

@Data
public class DeviceStatusData implements Serializable {
    // 设备状态
    private boolean ledStatus; // true: 开, false: 关
    private boolean buzzerStatus;
    private boolean monitorStatus;

}
