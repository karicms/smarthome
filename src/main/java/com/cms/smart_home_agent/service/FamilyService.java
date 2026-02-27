package com.cms.smart_home_agent.service;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.UpdateWrapper;

import com.cms.smart_home_agent.entity.Family;
import com.cms.smart_home_agent.entity.User;
import com.cms.smart_home_agent.mapper.FamilyMapper;
import com.cms.smart_home_agent.mapper.UserMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
public class FamilyService {

    @Autowired
    private FamilyMapper familyMapper;

    @Autowired
    private UserMapper userMapper;

    /** 辅助方法：生成唯一的家庭码 */
    private String generateFamilyCode() {
        return UUID.randomUUID().toString().substring(0, 8).toUpperCase();
    }

    /** 辅助方法：获取用户 */
    private User getUserById(Integer userId) {
        User user = userMapper.selectById(userId);
        if (user == null) {
            throw new RuntimeException("用户不存在。");
        }
        return user;
    }

    // --- C: 创建家庭 ---
    @Transactional
    public Family createFamily(String familyName, Integer currentUserId) {
        User user = getUserById(currentUserId);

        // 1. 业务校验：检查用户是否已在其他家庭
        if (user.getFamilyId() != null) {
            throw new RuntimeException("您已属于一个家庭，无法创建新家庭。请先退出。");
        }

        // 2. 创建 Family 记录
        Family family = new Family();
        family.setFamilyName(familyName);
        family.setFamilyCode(generateFamilyCode());
        familyMapper.insert(family);

        // 3. 将创建者加入家庭 (更新用户 familyId 字段)
        User updateUser = new User();
        updateUser.setId(currentUserId);
        // family.getId() 现在返回 Integer
        updateUser.setFamilyId(family.getId());
        userMapper.updateById(updateUser);

        return family;
    }

    // --- R: 查询家庭 ---
    public Family getMyFamily(Integer currentUserId) {
        User user = getUserById(currentUserId);

        if (user.getFamilyId() == null) {
            return null; // 用户未加入任何家庭
        }

        // user.getFamilyId() 现在返回 Integer
        return familyMapper.selectById(user.getFamilyId());
    }

    // --- U: 修改家庭名称 ---
    // familyId 现为 Integer
    public boolean updateFamilyName(Integer familyId, String newName, Integer currentUserId) {
        User user = getUserById(currentUserId);

        // 1. 权限校验：只检查用户是否属于该家庭
        if (user.getFamilyId() == null || !user.getFamilyId().equals(familyId)) {
            throw new RuntimeException("权限不足，您不是该家庭的成员。");
        }

        // 2. 更新家庭信息
        Family family = new Family();
        family.setId(familyId); // familyId 现为 Integer
        family.setFamilyName(newName);

        return familyMapper.updateById(family) > 0;
    }

    // --- D: 删除家庭 ---
    // familyId 现为 Integer
    @Transactional
    public boolean deleteFamily(Integer familyId, Integer currentUserId) {
        User user = getUserById(currentUserId);

        // 1. 权限校验：只检查用户是否属于该家庭
        if (user.getFamilyId() == null || !user.getFamilyId().equals(familyId)) {
            throw new RuntimeException("权限不足，您不是该家庭的成员。");
        }

        Family family = familyMapper.selectById(familyId); // familyId 现为 Integer
        if (family == null) {
            return true;
        }

        // 2. 解除所有成员的关联 (将 User 表中该 familyId 的用户设为 familyId = null)
        UpdateWrapper<User> userUpdateWrapper = new UpdateWrapper<>();
        userUpdateWrapper.eq("family_id", familyId) // familyId 现为 Integer
                .set("family_id", null);
        userMapper.update(null, userUpdateWrapper);

        // 3. 删除 Family 记录
        return familyMapper.deleteById(familyId) > 0; // familyId 现为 Integer
    }

    // --- 额外功能：通过码加入家庭 ---
    @Transactional
    public Family joinFamilyByCode(String familyCode, Integer currentUserId) {
        User user = getUserById(currentUserId);

        // 1. 检查是否已在其他家庭
        if (user.getFamilyId() != null) {
            throw new RuntimeException("您已属于一个家庭，加入新家庭前请先退出。");
        }

        // 2. 查找家庭
        QueryWrapper<Family> familyWrapper = new QueryWrapper<>();
        familyWrapper.eq("family_code", familyCode);
        Family family = familyMapper.selectOne(familyWrapper);

        if (family == null) {
            throw new RuntimeException("家庭码无效或家庭不存在。");
        }

        // 3. 加入家庭 (更新用户 familyId)
        User updateUser = new User();
        updateUser.setId(currentUserId);
        updateUser.setFamilyId(family.getId()); // family.getId() 现在返回 Integer
        userMapper.updateById(updateUser);

        return family;
    }

    // --- 额外功能：退出家庭 ---
    // 成员可以直接退出家庭
    @Transactional
    public boolean leaveFamily(Integer currentUserId) {
        User user = getUserById(currentUserId);

        if (user.getFamilyId() == null) {
            return true; // 已经不在家庭中
        }

        // 【核心修复】使用 UpdateWrapper 显式设置 family_id = null。
        // 这样可以确保生成的 SQL 语句包含 SET 子句：
        // UPDATE user SET family_id = NULL WHERE id = ?
        UpdateWrapper<User> userUpdateWrapper = new UpdateWrapper<>();
        userUpdateWrapper.eq("id", currentUserId)
                .set("family_id", null);

        // 第一个参数传 null，只依赖 Wrapper 进行更新
        int rows = userMapper.update(null, userUpdateWrapper);

        if (rows != 1) {
            // 确保更新成功，否则抛出异常回滚事务
            throw new RuntimeException("退出家庭操作失败，用户记录未更新。");
        }

        return true;
    }
}
