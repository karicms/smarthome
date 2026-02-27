package com.cms.smart_home_agent.entity;

import lombok.Data;

@Data
public class User {
    private Integer id;
    private String userName;
    private String password;
    private Integer FamilyId;
}