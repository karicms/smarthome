package com.cms.smart_home_agent.request;

import com.fasterxml.jackson.annotation.JsonPropertyDescription;
import lombok.Data;

@Data
public class AiConditioningRequest {
    @JsonPropertyDescription("用户的唯一ID")
    private Integer userId;

    @JsonPropertyDescription("家庭的唯一ID。请务必根据用户提到的家（如'新cms的家'）从上下文列表中找到对应的 ID 并填入。这是必填项！")
    private Integer familyId;

    @JsonPropertyDescription("房间名，如：客厅、卧室")
    private String location;

    @JsonPropertyDescription("设定的温度。如果用户没说具体温度，请保持为空。")
    private Double temperature; // 改为 Double 更合适

    @JsonPropertyDescription("该家庭所在的城市名称，例如：北京。请从家庭信息中提取。")
    private String familycity;
    @JsonPropertyDescription("设备的名称，用于区分不同设备的请求。设备名称可能跟设备的位置有关，比如客厅灯、卧室灯等，请根据用户的指令准确填写，不要随意猜测")
    private String deviceName;
    @JsonPropertyDescription("设备的类型，用于区分不同设备的请求。设备类型可能跟设备的功能有关，比如灯、空调等，请根据用户的指令准确填写，不要随意猜测")
    private String deviceType;
}