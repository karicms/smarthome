package com.cms.smart_home_agent.presence;

/**
 * 双红外对射一次「有效通过」的判定结果。
 */
public enum IrPassageResult {
    /** 已记下第一束，等待另一侧 */
    PENDING,
    /** 门外 → 门内 */
    ENTRY,
    /** 门内 → 门外 */
    EXIT,
    /** 次序不对或超出时间窗，已丢弃待配对状态 */
    DISCARDED_RESTART,
    /** 载荷或参数不合法 */
    INVALID
}
