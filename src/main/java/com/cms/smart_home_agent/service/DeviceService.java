package com.cms.smart_home_agent.service;


import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.cms.smart_home_agent.entity.Device;
import com.cms.smart_home_agent.mapper.DeviceMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.cms.smart_home_agent.climate.ClimateSensorTypeResolver;
import com.cms.smart_home_agent.presence.IrBeamSideResolver;
import com.cms.smart_home_agent.presence.PresenceSensorTypeResolver;

import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;

@Slf4j
@Service
public class DeviceService {
    @Autowired
    private DeviceMapper deviceMapper;

    public boolean existsByName(Integer familyId, String name) {
        // 1. 创建一个条件构造器
        QueryWrapper<Device> queryWrapper = new QueryWrapper<>();

        // 2. 拼接条件：family_id = ? AND device_name = ?
        queryWrapper.eq("family_id", familyId)
                .eq("device_name", name);

        // 3. 执行计数查询，如果数量 > 0 则说明重名了
        Long count = deviceMapper.selectCount(queryWrapper);
        return count > 0;
    }
    public String registerDevice(Integer familyId,String name,String type,String mqttTopic){
        if(existsByName(familyId,name))
        {
            return "设备已存在";
        }
        Device device = new Device();
        device.setDeviceName(name);
        device.setDeviceType(type);
        device.setFamilyId(familyId);
        device.setMqttTopic(mqttTopic);
        deviceMapper.insert(device);
        return "设备注册成功";
    }

    //根据家庭id，设备类型和设备名称查询mqtt topic
    public String findTopic(Integer familyId, String type, String name) {
        log.info("🔍 正在查询设备主题 - 家庭ID: {}, 类型: {}, 名称: {}", familyId, type, name);

        QueryWrapper<Device> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("family_id", familyId)
                .eq("device_type", type)
                .eq("device_name", name)
                .last("LIMIT 1");

        Device device = deviceMapper.selectOne(queryWrapper);

        if (device == null) {
            log.warn("⚠️ 查询结束：未找到匹配设备！请检查数据库中 family_id={} AND device_type='{}' AND device_name='{}' 是否存在。",
                    familyId, type, name);
            return null;
        }

        if (device.getMqttTopic() == null || device.getMqttTopic().isEmpty()) {
            log.error("❌ 查询结束：找到设备但其 mqtt_topic 为空！设备ID: {}", device.getId());
            return null;
        }

        return device.getMqttTopic();
    }
    public List<Device> listDevices(Integer familyId) {
        QueryWrapper<Device> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("family_id", familyId);
        queryWrapper.orderByDesc("id");  // ✅ 改成按 ID 排序
        return deviceMapper.selectList(queryWrapper);
    }

    /**
     * 按 MQTT 主题精确查找设备（用于上行消息路由：topic → familyId / 设备角色）。
     */
    public Optional<Device> findByMqttTopic(String mqttTopic) {
        if (mqttTopic == null || mqttTopic.isEmpty()) {
            return Optional.empty();
        }
        QueryWrapper<Device> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("mqtt_topic", mqttTopic).last("LIMIT 1");
        return Optional.ofNullable(deviceMapper.selectOne(queryWrapper));
    }

    /**
     * 所有已绑定 topic、且能解析出门内/门外的红外对射设备（用于动态订阅）。
     */
    public List<Device> listIrBeamDevices() {
        QueryWrapper<Device> queryWrapper = new QueryWrapper<>();
        queryWrapper.isNotNull("mqtt_topic");
        queryWrapper.ne("mqtt_topic", "");
        List<Device> rows = deviceMapper.selectList(queryWrapper);
        return rows.stream()
                .filter(d -> IrBeamSideResolver.resolve(d) != null)
                .toList();
    }

    /**
     * 已绑定 topic 的 PIR / 毫米波设备（用于动态订阅与 Topic→familyId 路由）。
     */
    /** 已绑定 topic 的温湿度类传感器（用于动态订阅与按家庭缓存读数）。 */
    public List<Device> listClimateSensorDevices() {
        QueryWrapper<Device> queryWrapper = new QueryWrapper<>();
        queryWrapper.isNotNull("mqtt_topic");
        queryWrapper.ne("mqtt_topic", "");
        return deviceMapper.selectList(queryWrapper).stream()
                .filter(ClimateSensorTypeResolver::isClimateSensor)
                .toList();
    }

    public boolean hasClimateSensor(Integer familyId) {
        if (familyId == null) {
            return false;
        }
        return listDevices(familyId).stream().anyMatch(ClimateSensorTypeResolver::isClimateSensor);
    }

    public List<Device> listPresenceSensorDevices() {
        QueryWrapper<Device> queryWrapper = new QueryWrapper<>();
        queryWrapper.isNotNull("mqtt_topic");
        queryWrapper.ne("mqtt_topic", ""); //作用：过滤掉 mqtt_topic 为空字符串的设备，确保只返回那些真正绑定了 MQTT 主题的设备。因为如果 mqtt_topic 是空字符串，虽然不为 null，但实际上也没有有效的主题可用，这可能会导致后续处理逻辑出错或产生误导。
        return deviceMapper.selectList(queryWrapper).stream()
                .filter(
                        d ->
                                PresenceSensorTypeResolver.resolve(d)
                                        != PresenceSensorTypeResolver.Kind.UNKNOWN)
                .toList();
    }

    public int Devicenum(Integer familyId)
    {
        QueryWrapper<Device> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("family_id", familyId);
        return deviceMapper.selectCount(queryWrapper).intValue();
    }

    /**
     * 删除设备（仅校验家庭归属并删库表；Redis/MQTT 缓存清理由 Controller 编排）。
     *
     * @return null 表示成功；否则为错误说明
     */
    public String deleteDevice(Integer familyId, Integer deviceId) {
        if (familyId == null || deviceId == null) {
            return "家庭ID与设备ID不能为空";
        }
        Device device = deviceMapper.selectById(deviceId);
        if (device == null) {
            return "设备不存在";
        }
        if (!Objects.equals(familyId, device.getFamilyId())) {
            return "无权删除该设备";
        }
        deviceMapper.deleteById(deviceId);
        log.info("已删除设备 id={} name={} family={}", deviceId, device.getDeviceName(), familyId);
        return null;
    }

    // 专门为 AI 生成设备清单描述
    public String getDeviceListForAi(Integer familyId) {
        List<Device> devices = this.listDevices(familyId);
        if (devices.isEmpty()) return "该家庭暂无绑定设备。";

        return devices.stream()
                .map(d -> String.format("- %s (类型: %s)", d.getDeviceName(), d.getDeviceType()))
                .collect(Collectors.joining("\n"));
    }
}
