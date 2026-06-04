package com.cms.smart_home_agent.controller;


import com.cms.smart_home_agent.request.AiConditioningRequest;
import com.cms.smart_home_agent.service.AirConditioningService;
import com.cms.smart_home_agent.vo.Result;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Slf4j
@RestController // 这个注解表明这是一个控制器类，负责处理HTTP请求
@RequestMapping("/aihome/air-conditioning")
public class AirConditioningController {

    @Autowired
    private AirConditioningService airConditioningService;

    @PostMapping("/control")  // ✨ 添加这一行
    public Result controlleAirConditioning(@RequestBody AiConditioningRequest request) {
        log.info("收到空调控制请求: {}", request);
        String success = airConditioningService.apply(request);
        return Result.success(success);
    }
}
