package com.cms.smart_home_agent.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("chat_log")
public class ChatLog {
    @TableId(type= IdType.AUTO)
    private Integer id;
    private Integer userId;
    private String role; // "user" 或 "assistant"
    private String content;
    @TableField(fill= FieldFill.INSERT) // 创建时间在插入时自动填充
    private LocalDateTime createTime;

}
