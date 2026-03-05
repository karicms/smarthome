package com.cms.smart_home_agent.entity;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class HabitDataLog {
    private Integer id;
    private Integer familyId;
    private double  outdoorTemp;
    private double indoorTemp;
    private double targetTemp;
    private LocalDateTime createTime;
    private Integer userId;
}
