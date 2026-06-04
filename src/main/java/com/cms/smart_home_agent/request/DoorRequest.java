package com.cms.smart_home_agent.request;

import com.fasterxml.jackson.annotation.JsonPropertyDescription;
import lombok.Data;

@Data
public class DoorRequest{
    @JsonPropertyDescription("用户的唯一标识符，用于区分不同用户的请求。请确保每个用户都有一个唯一的ID，以便系统能够正确处理和响应他们的指令。")
    private Integer userId; // 用户的唯一标识符，用于区分不同用户的请求
    @JsonPropertyDescription("门所在的位置，如客厅、卧室等，如果用户没有明确指定位置，请保持此字段为空，系统将默认操作客厅的门。")
    private String location; // 位置，例如：客厅、卧室等
    @JsonPropertyDescription("门的控制动作。如果是开启动作（如打开、请进、开门等），请务必填写 'true'；如果是关闭动作（如关门、锁门、带上门等），请务必填写 'false'。")
    private String action;
    @JsonPropertyDescription("家庭的唯一标识符，用于区分不同家庭的请求。请确保每个家庭都有一个唯一的ID，以便系统能够正确处理和响应他们的指令。")
    private Integer familyId;
    @JsonPropertyDescription("设备的名称，用于区分不同设备的请求。设备名称可能跟设备的位置有关，比如客厅灯、卧室灯等，请根据用户的指令准确填写，不要随意猜测")
    private String deviceName;
    @JsonPropertyDescription("设备的类型，用于区分不同设备的请求。设备类型可能跟设备的功能有关，比如灯、空调等，请根据用户的指令准确填写，不要随意猜测")
    private String deviceType;

}
