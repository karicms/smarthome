package com.cms.smart_home_agent.request;

import lombok.Data;

@Data
public class FamilyupdateRequest {
    private Integer userId;
    private Integer familyId;
    private String familyName;
    private String familyCode;
    private String familyCity;
    private String familyprovince;
    private String remark;
}
