package com.cms.smart_home_agent.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;

import com.cms.smart_home_agent.entity.User;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

@Mapper
public interface UserMapper extends BaseMapper<User> {

    /**
     * 登录校验：根据用户名和明文密码查询用户记录。
     * 注意：确保查询结果中的字段名与User实体类的属性名保持一致（通过AS别名）。
     */
    @Select(
            "SELECT id,\n" +
                    "       user_name AS userName,\n" +
                    "       password,\n" +
                    "       family_id AS familyId\n" +
                    "  FROM `user`\n" +
                    " WHERE user_name = #{userName}\n" +
                    "   AND password = #{password}"
    )
    User findByUsernameAndPassword(
            @Param("userName") String username,
            @Param("password")  String password
    );

    /**
     * 注册时查重：统计同名用户名的数量。
     * 实际项目中，此功能更推荐在Service层使用MyBatis-Plus的QueryWrapper实现：
     * userMapper.selectCount(new QueryWrapper<User>().eq("user_name", username));
     */
    @Select("SELECT COUNT(1) FROM `user` WHERE user_name = #{userName}")
    Integer countByUsername(@Param("userName") String username);

    /**
     * 查询用户名是否存在（获取完整用户信息）。
     * 实际项目中，此功能更推荐在Service层使用MyBatis-Plus的QueryWrapper实现：
     * userMapper.selectOne(new QueryWrapper<User>().eq("user_name", username));
     */
    @Select("SELECT id, user_name AS userName, password, family_id AS familyId\n" +
            "  FROM `user`\n" +
            " WHERE user_name = #{userName}")
    User findByUsername(@Param("userName") String username);
}
