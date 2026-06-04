package com.cms.smart_home_agent.presence;

/**
 * 毫米波雷达接入抽象：相对 PIR，可检测微动（呼吸级），用于弥补静止人体漏检。
 * <p>
 * 典型接入方式：MQTT 订阅解析后调用 {@link com.cms.smart_home_agent.service.PresenceDetectionService#onMmWavePresence}，
 * 或由边缘网关直调 REST（若后续扩展）。
 */
public interface MmWaveOccupancyAdapter {

    /**
     * @param familyId      家庭 ID
     * @param humanPresent  算法输出的「是否判定有人」
     * @param epochMillis     事件时间（设备时间戳，毫秒）
     * @param confidenceHint  可选 0~1，未知时可传 {@link Double#NaN}
     */
    void reportOccupancy(int familyId, boolean humanPresent, long epochMillis, double confidenceHint);
}
