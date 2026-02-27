package com.cms.smart_home_agent.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;

import com.cms.smart_home_agent.entity.Family;
import org.apache.ibatis.annotations.Mapper;

/**
 * 家庭实体 Family 的 Mapper 接口
 * 继承 BaseMapper 即可获得常用的 CRUD 方法
 */
@Mapper
public interface FamilyMapper extends BaseMapper<Family> {
    // 可以在这里定义自定义的家庭相关的数据库操作方法
}
