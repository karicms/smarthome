package com.cms.smart_home_agent.service;

import com.cms.smart_home_agent.mapper.FamilyMemberMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.util.concurrent.TimeUnit;

@Service
@Slf4j
public class DirectionalPresenceService {

    @Autowired
    private StringRedisTemplate stringRedisTemplate;

    private static final String IR_WAIT_KEY = "ir_wait:"; // Redis key prefix for waiting IR signals
    private static final int MATCH_WINDOW= 5;// 定义一个时间窗口（秒），在这个窗口内接收到的 IR 信号才算匹配

    public String processIrTrigger(String sensorId,Integer familyId)
    {
        String currentkey = IR_WAIT_KEY + familyId+":"+sensorId;

        String otherSensorId = sensorId.equals("OUT") ? "IN" : "OUT"; // 假设只有两个传感器，分别是 OUT 和 IN，根据当前触发的传感器 ID 来确定另一个传感器 ID
        String otherKey = IR_WAIT_KEY + familyId+":"+otherSensorId;

        Boolean hasOther = stringRedisTemplate.hasKey(otherKey);
        if(Boolean.TRUE.equals(hasOther))
        {
            stringRedisTemplate.delete(otherKey); // 删除另一个传感器的等待状态
            if (sensorId.equals("IN")) {
                log.info("===> 检测到逻辑序列: OUT -> IN。判定为: 【人员进入】 (家庭: {})", familyId);
                return "ENTRY";
            } else {
                log.info("===> 检测到逻辑序列: IN -> OUT。判定为: 【人员离开】 (家庭: {})", familyId);
                return "EXIT";
            }
        }else
        {
            stringRedisTemplate.opsForValue().set(currentkey,"1",MATCH_WINDOW, TimeUnit.SECONDS);
            log.info("传感器 {} 触发，存入缓存，有效期 {}s，等待配对传感器...", sensorId, MATCH_WINDOW);
            return "PENDING";
        }
    }
}
