package com.cms.smart_home_agent.service;


import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.cms.smart_home_agent.entity.Device;
import com.cms.smart_home_agent.mapper.DeviceMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

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

    public String findTopic(Integer familyId,String type,String name)
    {
        QueryWrapper<Device> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("family_id", familyId)
                .eq("device_type", type)
                .eq("device_name", name)
                .last("LIMIT 1"); // 客观优化：只取一条，提高查询效率
        Device device = deviceMapper.selectOne(queryWrapper); // 结果可能是 null，如果没有找到匹配的设备
        return device != null ? device.getMqttTopic() : null;
    }
    public List<Device> listDevices(Integer familyId) {
        QueryWrapper<Device> queryWrapper = new QueryWrapper<>();
        // 按照家庭 ID 查询
        queryWrapper.eq("family_id", familyId);
        // 客观建议：可以加一个排序，让列表显示更整齐
        queryWrapper.orderByDesc("create_time");

        return deviceMapper.selectList(queryWrapper);
    }

    public int Devicenum(Integer familyId)
    {
        QueryWrapper<Device> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("family_id", familyId);
        return deviceMapper.selectCount(queryWrapper).intValue();
    }
}
