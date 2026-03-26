package com.cms.smart_home_agent.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.cms.smart_home_agent.entity.FamilyMember;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;

import java.util.List;

@Mapper
public interface FamilyMemberMapper extends BaseMapper<FamilyMember> {

        // 直接返回 ID 列表
        @Select("SELECT family_id FROM family_member WHERE user_id = #{userId}")
        List<Integer> selectFamilyIdsByUserId(Integer userId);

        // 获取家庭成员数量
        @Select("SELECT COUNT(*) FROM family_member WHERE family_id = #{familyId}")
        int countMembersByFamilyId(Integer familyId);

        // 获取家庭成员数量
        @Select("SELECT COUNT(*) FROM family_member WHERE family_id = #{familyId}")
        int selectMemberCountByFamilyId(Integer familyId);
}
