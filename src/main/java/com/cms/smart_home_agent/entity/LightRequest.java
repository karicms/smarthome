package com.cms.smart_home_agent.entity;

import com.fasterxml.jackson.annotation.JsonPropertyDescription;
import lombok.Data;

@Data
public class LightRequest {
    @JsonPropertyDescription("灯所在的位置，如客厅、卧室等，如果用户没有明确指定位置，请保持此字段为空，系统将默认操作客厅的灯。")
    private String location; // 位置，例如：客厅、卧室等
    @JsonPropertyDescription("灯的动作，例如：开灯、关灯、调暗、调亮等，请根据用户的指令准确填写，不要随意猜测。")
    private String action;   // 动作，例如：开灯、关灯、调暗、调亮等
}
