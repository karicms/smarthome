package com.cms.smart_home_agent.vo;

import lombok.Data;

/**
 * 离家自动化状态，供前端展示。
 */
@Data
public class AwayStatusVo {
    /** home | away_pending | away_confirmed */
    private String state;
    /** 当前估计在宅人数（红外序列推算），仅 home 时有值 */
    private Integer estimatedOccupancy;
    /** 离家待定截止时间（毫秒时间戳），仅 away_pending 时有值 */
    private Long awayDeadlineMs;
    /** 最近一次后端确认离家并关断的时间（毫秒时间戳） */
    private Long lastAwayConfirmMs;
}
