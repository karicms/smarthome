package com.cms.smart_home_agent.entity;

import lombok.Data;

@Data
public class HomestatusResponse {
    private boolean ledStatus; // true: 开, false: 关
    private boolean buzzerStatus;
    private boolean monitorStatus;
    private double humidity;
    private double temperature;
}
