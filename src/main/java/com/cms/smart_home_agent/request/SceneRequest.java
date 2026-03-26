package com.cms.smart_home_agent.request;

import com.fasterxml.jackson.annotation.JsonPropertyDescription;
import lombok.Data;
import org.springframework.context.annotation.Description;

@Data
public class SceneRequest {
    @JsonPropertyDescription("是否需要控制灯光。如果不需要则为null")
    private LightRequest lightAction;

    @JsonPropertyDescription("是否需要控制门锁。如果不需要则为null")
    private DoorRequest doorAction;

    @JsonPropertyDescription("是否需要控制空调。如果不需要则为null")
    private AiConditioningRequest acAction;

    @JsonPropertyDescription("场景名称，如：睡觉模式、离家模式")
    private String sceneName;
}