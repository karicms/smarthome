package com.cms.smart_home_agent.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.cms.smart_home_agent.entity.HabitDataLog;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

@Mapper
public interface HabitDataLogMapper extends BaseMapper<HabitDataLog> {

        /**
         * 获取指定用户最近的行为日志
         * @param userId 用户ID
         * @param limit 取多少条记录进行拟合（建议 10-30 条）
         * @return 行为日志列表
         */
        @Select("SELECT * FROM habit_data_log WHERE user_id = #{userId} " + // 这里最后加个空格
                "ORDER BY create_time DESC LIMIT #{limit}") // 或者这里开头加个空格
        List<HabitDataLog> findRecentLogsByUserId(@Param("userId") Integer userId, @Param("limit") Integer limit);


    // 如果你已经写了根据特定家庭查询的方法，也请检查一下：
    @Select("SELECT * FROM habit_data_log " +
            "WHERE user_id = #{userId} AND family_id = #{familyId} " + // 确保末尾有空格
            "ORDER BY create_time DESC LIMIT #{limit}")
    List<HabitDataLog> findRecentLogsBySpecificContext(@Param("userId") Integer userId,
                                                       @Param("familyId") Integer familyId,
                                                       @Param("limit") Integer limit);
        /**
         * 当用户操作空调时，保存这一次的行为数据
         */
        @Insert("INSERT INTO habit_data_log(family_id, user_id, outdoor_temp, indoor_temp, target_temp, create_time) " +
                "VALUES(#{familyId}, #{userId}, #{outdoorTemp}, #{indoorTemp}, #{targetTemp}, NOW())")
        int insertHabitLog(HabitDataLog log);
}
