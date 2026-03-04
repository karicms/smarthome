package com.cms.smart_home_agent.DTO;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor  // 生成全参构造函数
@NoArgsConstructor  // 生成无参构造函数
public class LocationDTO {
    private String province;
    private String city;
    private String adcode;
}
