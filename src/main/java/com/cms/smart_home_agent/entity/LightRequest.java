package com.cms.smart_home_agent.entity;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyDescription;
import lombok.Data;

@Data
public class LightRequest {
    @JsonPropertyDescription("用户的唯一标识符，用于区分不同用户的请求。请确保每个用户都有一个唯一的ID，以便系统能够正确处理和响应他们的指令。")
    private String userId; // 用户的唯一标识符，用于区分不同用户的请求
    @JsonPropertyDescription("灯所在的位置，如客厅、卧室等，如果用户没有明确指定位置，请保持此字段为空，系统将默认操作客厅的灯。")
    private String location; // 位置，例如：客厅、卧室等
    @JsonPropertyDescription("灯的动作，例如：开灯、关灯、调暗、调亮等，请根据用户的指令准确填写，不要随意猜测。")
    private String action;   // 动作，例如：开灯、关灯、调暗、调亮等
}
