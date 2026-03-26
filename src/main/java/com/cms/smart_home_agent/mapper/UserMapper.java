package com.cms.smart_home_agent.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.cms.smart_home_agent.entity.User;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

@Mapper
public interface UserMapper extends BaseMapper<User> {

    @Select("SELECT id, user_name AS userName, password " +
            "FROM `user` WHERE user_name = #{userName} AND password = #{password}") // 注意：实际项目中密码应该加密存储，这里为了简单演示直接明文查询
    User findByUsernameAndPassword(@Param("userName") String username, @Param("password") String password);

    @Select("SELECT id, user_name AS userName, password " +
            "FROM `user` WHERE user_name = #{userName}")
    User findByUsername(@Param("userName") String username);

    @Select("SELECT COUNT(1) FROM `user` WHERE user_name = #{userName}")
    Integer countByUsername(@Param("userName") String username); // 返回用户名出现的次数，0 表示不存在，1 表示已存在
}