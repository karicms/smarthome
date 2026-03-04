package com.cms.smart_home_agent.vo;

import lombok.Data;

@Data
public class FamilyVo {
    private Integer id;
    private String familyName;
    private String familyCode; // 核心：入场券
    private String adcode;
    private String city;
    private String remark; //备注是哪个家
}
