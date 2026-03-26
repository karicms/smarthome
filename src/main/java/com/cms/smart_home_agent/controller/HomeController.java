package com.cms.smart_home_agent.controller;

import com.cms.smart_home_agent.request.ControlRequest;
import com.cms.smart_home_agent.entity.HomestatusResponse;
import com.cms.smart_home_agent.entity.tempconfig;
import com.cms.smart_home_agent.service.HomeService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * 智能家居 API 控制器
 * 所有接口路径前缀为 /api/v1/home
 */
@Slf4j
@RestController
@RequestMapping("/api/v1/home")
public class HomeController {

    @Autowired
    private HomeService homeService;

    /**
     * 【任务 1 实现】GET /status
     * 用于获取实时温湿度和设备状态
     */
    @GetMapping("/status")
    public ResponseEntity<HomestatusResponse> getStatus() {
        HomestatusResponse status = homeService.getstatus();
        return ResponseEntity.ok(status);
    }


    /**
     * 【任务 2 实现】POST /control/{device}
     * 用于手动控制设备 (LED/Buzzer)
     * 示例 URL: POST /api/v1/home/control/led
     */
    @PostMapping("/control")
    public ResponseEntity<?> controlDevice(
            @RequestBody ControlRequest request) {
        String deviceName = request.getDevicename();
        Integer familyId = request.getFamilyId();
        boolean success = homeService.controlDevice(familyId,deviceName, request);
        log.info("收到控制请求: 家庭={}, 设备={}, 动作={}", familyId, deviceName, request.getAction());
        if(familyId == null){
            return ResponseEntity.badRequest().body("家庭ID不能为空");
        }
        if (success) {
            return ResponseEntity.ok().body("Control command sent successfully.");
        } else {
            return ResponseEntity.badRequest().body("Unknown device: " + deviceName);
        }
    }

    //设置温度阈值
    @PostMapping("/tempconfig")
    public ResponseEntity<?> controlconfig(
            @RequestBody tempconfig temp) {
        boolean success = homeService.controlconfig(temp);
        if (success) {
            return ResponseEntity.ok().body("Control command sent successfully.");
        } else {
            return ResponseEntity.badRequest().body("Unknown device: ");
        }
    }
    /**
     * 【任务 3 实现】POST /ai-command
     * 用于接收用户的自然语言指令，并由 AI 决策处理
     */
//    @PostMapping("/ai-command")
//    public ResponseEntity<AiCommandResponse> sendAiCommand(
//            @RequestBody AiCommandRequest request) {
//
//        AiCommandResponse response = homeService.handleAiCommand(request.getCommand());
//        return ResponseEntity.ok(response);
//    }
}
