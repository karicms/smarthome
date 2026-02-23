package com.cms.smart_home_agent.config;

import com.cms.smart_home_agent.entity.AiConditioningRequest;
import com.cms.smart_home_agent.entity.DoorRequest;
import com.cms.smart_home_agent.entity.LightRequest;
import com.cms.smart_home_agent.service.AirConditioningService;
import com.cms.smart_home_agent.service.DoorService;
import com.cms.smart_home_agent.service.LightService;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.ai.chat.memory.InMemoryChatMemory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Description;

import java.util.function.Function;

@Configuration //作用：标识这是一个配置类，Spring会扫描并加载其中定义的Bean。
public class Aiconfig {
    private final AirConditioningService airConditioningService;
    private final DoorService doorService;
    private final LightService lightService;

    public Aiconfig(AirConditioningService airConditioningService,DoorService doorService,LightService lightService) {
        this.doorService = doorService;
        this.lightService = lightService;
        this.airConditioningService = airConditioningService;

    }

    @Bean
    @Description("控制家里的空调系统，可以设置指定房间的温度")
    public Function<AiConditioningRequest,String> airConditioningControl()
    {
        return this.airConditioningService;
    }
    @Bean
    @Description("控制家里的灯，可以开灯或关灯等操作")
     public Function<LightRequest,String> lightControl()
    {
        return this.lightService;
    }
    @Bean
    @Description("控制家里的门，可以开门、关门和锁门等操作")
    public Function<DoorRequest,String> doorControl()
    {
        return this.doorService;
    }

    @Bean
    public ChatMemory chatMemory()
    {
        return new InMemoryChatMemory();//这里使用了一个简单的内存聊天记忆实现，实际应用中可以根据需要选择更复杂的实现，例如数据库存储等。
    }

}
