package com.cms.smart_home_agent.entity;

import lombok.Data;

@Data
public class RecommendRequest {
    private Integer userId;
    private int insideTemperature; // 室内温度，例如：22.5
    private int outsideTemperature; // 室外温度，例如：30.0
}

