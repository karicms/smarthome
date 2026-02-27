package com.cms.smart_home_agent.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyDescription;
import lombok.Data;

@Data
public class AiConditioningRequest {
    @JsonPropertyDescription("用户的唯一标识符，用于区分不同用户的请求。请确保每个用户都有一个唯一的ID，以便系统能够正确处理和响应他们的指令。")
    private String userId;

    @JsonPropertyDescription("房间名。例如：客厅、卧室。如果用户没指明房间，请忽略此字段或保持为空，系统将默认操作客厅。")
    private String location;

    @JsonPropertyDescription("设定的摄氏度。如果用户没提到具体温度（如只说'开空调'），请不要随意猜测，直接保持为空，系统将自动进入智能预测模式。")
    private Integer temperature; // 推荐用 Integer
}
