package com.cms.smart_home_agent.request;

import lombok.Data;

@Data
public class ChatRequest {
    private Integer userId;
    private Integer familyId;
    private String message;

}
