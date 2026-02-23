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
        String location = (request.getLocation() == null) ? "客厅" : request.getLocation();//处理默认位置

        Integer targetTemp = request.getTemperature();
        String modeMessage = "";

        if(targetTemp == null)
        {
            //这里添加预测逻辑调用预测模型

            targetTemp = 24; //假设预测结果是24度
            modeMessage = "（已经通过预测模型为您预测最适温度）";
        }
        // 1. 核心逻辑：目前先打印到控制台，模拟控制过程
        log.info(">>>>>> [硬件指令执行中] <<<<<<");
        log.info("目标房间: {}", location);
        log.info("设定温度: {} ℃", targetTemp);
        log.info(">>>>>> [指令发送成功] <<<<<<");

        // 2. 返回给 AI 的执行结果
        // 这句话很重要，AI 会根据这段文字来组织它最后对用户说的话
        return String.format("成功！%s 的空调已经调整为 %d 度,%s",
                location,
                targetTemp,modeMessage);
    }
}