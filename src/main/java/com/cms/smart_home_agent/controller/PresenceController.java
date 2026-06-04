package com.cms.smart_home_agent.controller;

import com.cms.smart_home_agent.service.PresenceDetectionService;
import com.cms.smart_home_agent.service.SensorTriggerStateService;
import com.cms.smart_home_agent.vo.AwayStatusVo;
import com.cms.smart_home_agent.vo.Result;
import com.cms.smart_home_agent.vo.SensorTriggerVo;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 室内存在估计（Redis），供前端展示。
 */
@Slf4j
@RestController
@RequestMapping("/aihome/presence")
public class PresenceController {

    @Autowired
    private PresenceDetectionService presenceDetectionService;

    @Autowired
    private SensorTriggerStateService sensorTriggerStateService;

    /**
     * GET /aihome/presence/estimated-count?familyId=
     * data 为整数：当前估计在宅人数（红外序列推算）。
     */
    @GetMapping("/estimated-count")
    public Result getEstimatedCount(@RequestParam(required = false) Integer familyId) {
        if (familyId == null) {
            return Result.fail("请提供家庭ID");
        }
        int n = presenceDetectionService.getEstimatedOccupancy(familyId);
        return Result.success(n);
    }

    /**
     * GET /aihome/presence/sensor-triggers?familyId=
     * 返回该家庭下 PIR / 毫米波设备最近触发状态（前端轮询，active=true 时显示「已触发」）。
     */
    @GetMapping("/sensor-triggers")
    public Result listSensorTriggers(@RequestParam(required = false) Integer familyId) {
        if (familyId == null) {
            return Result.fail("请提供家庭ID");
        }
        List<SensorTriggerVo> list = sensorTriggerStateService.listForFamily(familyId);
        return Result.success(list);
    }

    /**
     * GET /aihome/presence/away-status?familyId=
     * 离家自动化状态：在家 / 离家待定 / 已确认离家。
     */
    @GetMapping("/away-status")
    public Result getAwayStatus(@RequestParam(required = false) Integer familyId) {
        if (familyId == null) {
            return Result.fail("请提供家庭ID");
        }
        AwayStatusVo status = presenceDetectionService.getAwayStatus(familyId);
        return Result.success(status);
    }

    /**
     * POST /aihome/presence/trigger-away?familyId=
     * 前端手动触发离家模式（与自动离家共用关灯关门逻辑）。
     */
    @PostMapping("/trigger-away")
    public Result triggerAway(@RequestParam(required = false) Integer familyId) {
        if (familyId == null) {
            return Result.fail("请提供家庭ID");
        }
        int shutOffCount = presenceDetectionService.triggerAwayManually(familyId);
        return Result.success(java.util.Map.of(
                "shutOffCount", shutOffCount,
                "message", "已关闭 " + shutOffCount + " 个灯/门设备"
        ));
    }
}
