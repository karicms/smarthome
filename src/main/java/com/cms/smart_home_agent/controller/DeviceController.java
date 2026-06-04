package com.cms.smart_home_agent.controller;

import com.cms.smart_home_agent.entity.Device;
import com.cms.smart_home_agent.climate.ClimateSensorTopicRegistry;
import com.cms.smart_home_agent.presence.IrBeamTopicRegistry;
import com.cms.smart_home_agent.presence.PresenceSensorTopicRegistry;
import com.cms.smart_home_agent.service.DeviceService;
import com.cms.smart_home_agent.service.SensorTriggerStateService;
import com.cms.smart_home_agent.vo.Result;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Slf4j
@RestController
@RequestMapping("/aihome/device")
public class DeviceController {

    @Autowired
    private DeviceService deviceService;

    @Autowired
    private SensorTriggerStateService sensorTriggerStateService;

    @Autowired
    private IrBeamTopicRegistry irBeamTopicRegistry;

    @Autowired
    private PresenceSensorTopicRegistry presenceSensorTopicRegistry;

    @Autowired
    private ClimateSensorTopicRegistry climateSensorTopicRegistry;

    @PostMapping("/register")
    public Result registerDevice(@RequestBody Device device) {
        // 1. 基础参数校验（避免脏数据进入数据库）
        log.info("尝试注册设备：{} - {} @ {}", device.getDeviceName(), device.getDeviceType(), device.getMqttTopic());
        if (device.getFamilyId() == null || device.getDeviceName() == null
                || device.getDeviceType() == null || device.getMqttTopic() == null) {
            return Result.fail("注册失败：关键字段（家庭、名称、类型、主题）不能为空");
        }

        // 2. 调用服务并获取执行结果
        String msg = deviceService.registerDevice(
                device.getFamilyId(),
                device.getDeviceName(),
                device.getDeviceType(),
                device.getMqttTopic()
        );

        // 3. 根据返回的消息判断逻辑
        if ("设备已存在".equals(msg)) {
            return Result.fail(msg); // 返回 500 或其他错误码
        }

        log.info("新设备注册：{} - {} @ {}", device.getDeviceName(), device.getDeviceType(), device.getMqttTopic());
        irBeamTopicRegistry.refresh();
        presenceSensorTopicRegistry.refresh();
        climateSensorTopicRegistry.refresh();
        return Result.success(msg);
    }

    @GetMapping("/list")
    public Result listDevices(Integer familyId) {
        if (familyId == null) return Result.fail("请提供家庭ID");

        // 直接利用 MyBatis-Plus 的能力查询
        List<Device> devices = deviceService.listDevices(familyId);
        return Result.success(devices); // 需要 DeviceService 继承 ServiceImpl
    }

    @GetMapping("/devnum")
    public Result getDeviceNum(Integer familyId) {
        if(familyId == null) return Result.fail("请提供家庭ID");
        int num = deviceService.Devicenum(familyId);
        return Result.success(num);

    }

    /**
     * DELETE /aihome/device/{deviceId}?familyId=
     * 删除指定设备（须属于该家庭）。
     */
    @DeleteMapping("/{deviceId}")
    public Result deleteDevice(@PathVariable Integer deviceId, @RequestParam Integer familyId) {
        if (deviceId == null || familyId == null) {
            return Result.fail("请提供设备ID与家庭ID");
        }
        String err = deviceService.deleteDevice(familyId, deviceId);
        if (err != null) {
            return Result.fail(err);
        }
        sensorTriggerStateService.clearTrigger(deviceId);
        irBeamTopicRegistry.refresh();
        presenceSensorTopicRegistry.refresh();
        climateSensorTopicRegistry.refresh();
        return Result.success("设备已删除");
    }
}
