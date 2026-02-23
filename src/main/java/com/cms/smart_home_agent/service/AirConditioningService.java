package com.cms.smart_home_agent.service;

import com.cms.smart_home_agent.entity.AiConditioningRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.function.Function;

@Slf4j
@Service
public class AirConditioningService implements Function<AiConditioningRequest, String> {

    @Override
    public String apply(AiConditioningRequest request) {
        // 1. 核心逻辑：目前先打印到控制台，模拟控制过程
        log.info(">>>>>> [硬件指令执行中] <<<<<<");
        log.info("目标房间: {}", request.getLocation());
        log.info("设定温度: {} ℃", request.getTemperature());
        log.info(">>>>>> [指令发送成功] <<<<<<");

        // 2. 返回给 AI 的执行结果
        // 这句话很重要，AI 会根据这段文字来组织它最后对用户说的话
        return String.format("成功！%s 的空调已经调整为 %d 度。",
                request.getLocation(),
                request.getTemperature());
    }
}