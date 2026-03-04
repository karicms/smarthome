package com.cms.smart_home_agent.request;

import lombok.Data;

@Data
public class JoinFamilyRequest {
    private Integer userid;      // 谁要加入
    private String familyCode;   // 凭证是什么
    private String remark;       // 进去后备注叫什么（可选）
}
