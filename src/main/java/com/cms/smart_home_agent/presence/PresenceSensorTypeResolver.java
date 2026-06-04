package com.cms.smart_home_agent.presence;

import com.cms.smart_home_agent.entity.Device;

import java.util.Locale;

/**
 * 根据 device_type / device_name 区分 PIR 与毫米波（LD2410 等），用于 Topic→familyId 路由。
 */
public final class PresenceSensorTypeResolver {

    public enum Kind {
        PIR,
        MM_WAVE,
        UNKNOWN
    }

    private PresenceSensorTypeResolver() {}

    public static Kind resolve(Device device) {
        if (device == null) {
            return Kind.UNKNOWN;
        }
        String s = (safe(device.getDeviceType()) + " " + safe(device.getDeviceName()))
                .toLowerCase(Locale.ROOT);
        if (isMmWave(s)) {
            return Kind.MM_WAVE;
        }
        if (isPir(s)) {
            return Kind.PIR;
        }
        return Kind.UNKNOWN;
    }

    private static boolean isPir(String s) {
        return "pir".equals(s.trim())
                || s.contains("pir")
                || s.contains("hc-sr501")
                || s.contains("人体红外")
                || s.contains("热释电");
    }

    private static boolean isMmWave(String s) {
        return "ld2410".equals(s.trim())
                || s.contains("mmwave")
                || s.contains("mm-wave")
                || s.contains("ld2410")
                || s.contains("毫米波")
                || s.contains("雷达");
    }

    private static String safe(String v) {
        return v == null ? "" : v;
    }
}
