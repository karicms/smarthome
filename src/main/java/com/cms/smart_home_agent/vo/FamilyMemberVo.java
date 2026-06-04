package com.cms.smart_home_agent.vo;

import lombok.Data;

@Data
public class FamilyMemberVo {
    private Integer id;
    private Integer familyId;
    private Integer userId;
    private String userName;
    private String remark;
}
