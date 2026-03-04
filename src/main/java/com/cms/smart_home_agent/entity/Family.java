package com.cms.smart_home_agent.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import lombok.Data;

import java.time.LocalDateTime;

@Data
public class Family {
    @TableId(type = IdType.AUTO)
    private Integer id;
    private String familyName;
    private String familyCode; // 核心：入场券
    private String province;
    private String city;
    private String adcode;     // 核心：天气查询代码
    private LocalDateTime createTime; // 创建时间
}
