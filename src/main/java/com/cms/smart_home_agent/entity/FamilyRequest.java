package com.cms.smart_home_agent.entity;

import lombok.Data;

@Data
public class FamilyRequest {
    private Integer userid;
    private String familyName;
    private String familyCode;
}