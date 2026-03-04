package com.cms.smart_home_agent.config;

import com.cms.smart_home_agent.request.AiConditioningRequest;
import com.cms.smart_home_agent.request.DoorRequest;
import com.cms.smart_home_agent.request.LightRequest;
import com.cms.smart_home_agent.request.WeatherRequest;
import com.cms.smart_home_agent.service.AirConditioningService;
import com.cms.smart_home_agent.service.DoorService;
import com.cms.smart_home_agent.service.LightService;
import com.cms.smart_home_agent.service.WeatherService;
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
    private final WeatherService weatherService;

    public Aiconfig(AirConditioningService airConditioningService, DoorService doorService, LightService lightService, WeatherService weatherService) {
        this.doorService = doorService;
        this.lightService = lightService;
        this.airConditioningService = airConditioningService;
        this.weatherService= weatherService;

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

    // 工具 1：获取天气
    @Bean
    @Description("获取当前的室外天气温度，用于查询天气信息，帮助AI做出更智能的决策")
    public Function<WeatherRequest, String> outdoorWeatherFunction() {
        return this.weatherService;
    }

//    // 工具 2：偏好预测（基于数据库历史记录）
//    @Bean
//    @Description("根据当前室外温度，预测并推荐用户最习惯的空调目标温度")
//    public Function<RecommendRequest, Integer> habitRecommendationFunction() {
//        return (req) -> habitService.predict(req.getUserId(), req.getOutsideTemperature());
//    }


    @Bean
    public ChatMemory chatMemory()
    {
        return new InMemoryChatMemory();//这里使用了一个简单的内存聊天记忆实现，实际应用中可以根据需要选择更复杂的实现，例如数据库存储等。
    }

}
