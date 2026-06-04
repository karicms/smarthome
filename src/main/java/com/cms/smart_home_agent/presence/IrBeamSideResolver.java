package com.cms.smart_home_agent.presence;

import com.cms.smart_home_agent.entity.Device;

import java.util.Locale;

/**
 * 根据设备表中名称、类型推断红外对射侧：门内 IN / 门外 OUT。
 * <p>
 * 约定（命中任一即可）：名称或类型中包含「门内 / 门外 / inner / outer / _IN / _OUT」等关键字，
 * 便于不把家庭 ID 写死在固件里，仅靠 DB 注册区分。
 */
public final class IrBeamSideResolver {

    private IrBeamSideResolver() {}

    /**
     * @return {@code IN}、{@code OUT}，无法区分时返回 {@code null}
     */
    public static String resolve(Device device) {
        if (device == null) {
            return null;
        }
        String combined = safe(device.getDeviceType()) + " " + safe(device.getDeviceName());
        String s = combined.toLowerCase(Locale.ROOT);

        boolean inner =
                s.contains("门内")
                        || s.contains("inner")
                        || s.contains("_in")
                        || s.contains("-in")
                        || s.contains(" ir-in")
                        || s.contains("对内");
        boolean outer =
                s.contains("门外")
                        || s.contains("outer")
                        || s.contains("_out")
                        || s.contains("-out")
                        || s.contains(" ir-out")
                        || s.contains("对外");

        if (inner && !outer) {
            return "IN";
        }
        if (outer && !inner) {
            return "OUT";
        }
        return null;
    }

    private static String safe(String v) {
        return v == null ? "" : v;
    }
}
