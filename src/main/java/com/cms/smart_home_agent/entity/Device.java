package com.cms.smart_home_agent.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import lombok.Data;

@Data
public class Device {
    @TableId(type= IdType.AUTO)
    private Integer id;
    private String deviceName;
    private Integer familyId;
    private String deviceType; // 设备类型，例如：灯、门锁、空调等
    private String mqttTopic; // 设备对应的 MQTT 主题
}
