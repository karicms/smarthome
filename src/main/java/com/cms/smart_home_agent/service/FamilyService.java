package com.cms.smart_home_agent.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.cms.smart_home_agent.entity.Family;
import com.cms.smart_home_agent.entity.FamilyMember;
import com.cms.smart_home_agent.mapper.FamilyMapper;
import com.cms.smart_home_agent.mapper.FamilyMemberMapper;
import com.cms.smart_home_agent.request.createfamilyrequest;
import com.cms.smart_home_agent.vo.FamilyVo;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
public class FamilyService {

    @Autowired
    private FamilyMapper familyMapper;

    @Autowired
    private FamilyMemberMapper familyMemberMapper;

    /** 辅助方法：生成唯一的家庭码 */
    private String generateUniqueFamilyCode() {
        String code;
        boolean exists;
        int retryCount = 0;

        do {
            // 1. 生成 8 位随机码 (建议去掉容易混淆的字符，如 O, 0, I, 1)
            code = UUID.randomUUID().toString().replace("-", "").substring(0, 8).toUpperCase();

            // 2. 检查数据库中是否存在
            exists = familyMapper.selectCount(new LambdaQueryWrapper<Family>()
                    .eq(Family::getFamilyCode, code)) > 0;

            retryCount++;
            // 理论上 8 位码有 2.8 亿种组合，碰撞概率极低，通常循环 1 次就过了
            if (retryCount > 10) {
                throw new RuntimeException("生成唯一家庭码失败，系统繁忙。");
            }
        } while (exists);

        return code;
    }
    // --- C: 创建家庭
    @Transactional(rollbackFor = Exception.class)
    public Family createFamily(createfamilyrequest request, String remark)
    {
        Family family = new Family();
        family.setFamilyName(request.getFamilyName());
        family.setFamilyCode(generateUniqueFamilyCode());
        family.setCity(request.getCity());
        family.setProvince(request.getProvince());
        family.setAdcode(request.getAdcode());
        familyMapper.insert(family);

        FamilyMember member = new FamilyMember();
        member.setFamilyId(family.getId());
        Integer currentUserId = request.getUserid();
        member.setUserId(currentUserId);
        member.setRemark(remark);
        familyMemberMapper.insert(member);

        return family;
    }



    //查看用户关联的所有家庭
    public List<FamilyVo> getMyFamilies(Integer userId) {
        return familyMapper.selectUserFamilies(userId);
    }

    public List<Integer> getFamilyIdbyuserid(Integer userId) {
        return familyMemberMapper.selectFamilyIdsByUserId(userId);
    }

    //修改家庭名称
    public boolean updateFamilyName(Integer familyId,String newName)
    {
        Family family = new Family();
        family.setId(familyId);
        family.setFamilyName(newName);
        return familyMapper.updateById(family) > 0;

    }

    @Transactional(rollbackFor = Exception.class)
    public boolean deleteFamily(Integer familyId)
    {
        familyMemberMapper.delete(new LambdaQueryWrapper<FamilyMember>()
                .eq(FamilyMember::getFamilyId, familyId));
// 2. 后续这里可以添加删除设备、删除日志的逻辑
        // deviceMapper.delete(new LambdaQueryWrapper<Device>().eq(Device::getFamilyId, familyId));
        // habitDataLogMapper.delete(new LambdaQueryWrapper<HabitDataLog>().eq(HabitDataLog::getFamilyId, familyId));

        // 3. 删除家庭记录 (这里 familyId 是主键，可以用 deleteById)
        // SQL: DELETE FROM family WHERE id = ?
        //3. 删除家庭记录
        return familyMapper.deleteById(familyId) > 0;
    }

    // --- 额外功能：通过码加入家庭 ---
    @Transactional(rollbackFor = Exception.class)
    public Family joinFamilyByCode(Integer userId, String familyCode, String remark) {
        Family family = familyMapper.selectOne(new LambdaQueryWrapper<Family>().eq(Family::getFamilyCode, familyCode));
        if(family == null){
            throw new RuntimeException("家庭码无效或家庭不存在。");
        }

        //检查是否已经在家里
        Long count = familyMemberMapper.selectCount(new LambdaQueryWrapper<FamilyMember>()
                .eq(FamilyMember::getUserId, userId)
                .eq(FamilyMember::getFamilyId, family.getId()));
        if (count > 0) {
            throw new RuntimeException("您已经是该家庭成员。");
        }

        //建立关联
        FamilyMember member = new FamilyMember();
        member.setFamilyId(family.getId());
        member.setUserId(userId);
        member.setRemark(remark);
        familyMemberMapper.insert(member);

        return family;
    }

    // --- 额外功能：退出家庭 ---
    public boolean leaveFamily(Integer userId, Integer familyId) {
        return familyMemberMapper.delete(new LambdaQueryWrapper<FamilyMember>()
                .eq(FamilyMember::getUserId, userId)
                .eq(FamilyMember::getFamilyId, familyId)) > 0;
    }

    public boolean updatefamilyremark(Integer userid,Integer familyId, String remark)
    {
        FamilyMember member = familyMemberMapper.selectOne(new LambdaQueryWrapper<FamilyMember>()
                .eq(FamilyMember::getUserId, userid)
                .eq(FamilyMember::getFamilyId, familyId));
        if(member == null){
            return false;
        }
        member.setRemark(remark);
        return familyMemberMapper.updateById(member) > 0;
    }

    // 获取家庭成员数量
    public int getFamilyMemberCount(Integer familyId) {
        return familyMemberMapper.selectMemberCountByFamilyId(familyId);
    }


}
