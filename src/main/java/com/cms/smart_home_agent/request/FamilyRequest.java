package com.cms.smart_home_agent.request;

import lombok.Data;

@Data
public class FamilyRequest {
    private Integer userid;
    private Integer familyId;
    private String familyName;
    private String familyCode;
    private String remark;
}