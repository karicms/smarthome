package com.cms.smart_home_agent.request;

import lombok.Data;

@Data
public class createfamilyrequest {
    private Integer userid;
    private String province;
    private String familyName;
    private String adcode;
    private String city;
    private String remark; //备注是哪个家
}
