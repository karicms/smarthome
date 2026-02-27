package com.cms.smart_home_agent.entity;

import lombok.Data;
import lombok.ToString;

@Data
@ToString
public class SensorData {
    private double humidity;
    private double temperature;
}
