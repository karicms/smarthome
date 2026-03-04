package com.cms.smart_home_agent.request;

import lombok.Data;

@Data
public class FamilyMemberRequest {
    private Integer userid;
    private Integer familyid;
    private String remark; // 备注是哪个家
}
