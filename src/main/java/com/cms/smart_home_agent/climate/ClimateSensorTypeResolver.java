package com.cms.smart_home_agent.climate;

import com.cms.smart_home_agent.entity.Device;

import java.util.Locale;

/**
 * 识别 DB 中「温湿度 / 环境传感器」类设备（与 PIR、红外对射、毫米波区分）。
 */
public final class ClimateSensorTypeResolver {

    private ClimateSensorTypeResolver() {}

    public static boolean isClimateSensor(Device device) {
        if (device == null || device.getDeviceType() == null) {
            return false;
        }
        String type = device.getDeviceType().trim().toLowerCase(Locale.ROOT);
        if ("pir".equals(type) || "ld2410".equals(type) || type.startsWith("ir-")) {
            return false;
        }
        return "sensor".equals(type)
                || "thermostat".equals(type)
                || "climate".equals(type)
                || "temp-humidity".equals(type)
                || type.contains("dht")
                || (type.contains("temp") && type.contains("hum"));
    }
}
