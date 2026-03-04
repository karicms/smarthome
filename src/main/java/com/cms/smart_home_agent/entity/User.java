package com.cms.smart_home_agent.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import lombok.Data;

import java.time.LocalDateTime;

@Data
public class User {
    @TableId(type = IdType.AUTO) // 主键自增
    private Integer id;
    private String userName;
    private String password;
    private LocalDateTime createTime;
}