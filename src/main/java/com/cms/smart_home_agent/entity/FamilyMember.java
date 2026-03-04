package com.cms.smart_home_agent.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import lombok.Data;

@Data
public class FamilyMember {
    @TableId(type= IdType.AUTO)
    private Integer id;
    private Integer familyId;
    private Integer userId;
    private String remark; //备注是哪个家

}
