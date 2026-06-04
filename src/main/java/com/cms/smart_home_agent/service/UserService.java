package com.cms.smart_home_agent.service;


import com.cms.smart_home_agent.entity.User;
import com.cms.smart_home_agent.mapper.UserMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class UserService {
    @Autowired
    private UserMapper userMapper;

    public User login(String userName, String password) {
        return userMapper.findByUsernameAndPassword(userName, password);
    }

    public boolean signup(String userName, String password) {
        if (userMapper.countByUsername(userName) > 0) return false;
        User u = new User();
        u.setUserName(userName);
        u.setPassword(password);
        // u.setCreateTime(LocalDateTime.now()); // 如果数据库没设默认值，这里手动设
        return userMapper.insert(u) > 0;
    }

    public User get(Integer id) {
        return userMapper.selectById(id);
    }

    public boolean updateUser(User user) {
        return userMapper.updateById(user) == 1; // 只更新一条记录，返回值应该是 1
    }

    public String changePassword(Integer userId, String oldPassword, String newPassword) {
        if (userId == null) return "用户ID不能为空";
        if (oldPassword == null || oldPassword.trim().isEmpty()) return "旧密码不能为空";
        if (newPassword == null || newPassword.trim().isEmpty()) return "新密码不能为空";
        if (newPassword.length() < 6) return "新密码长度至少6位";

        User user = userMapper.selectById(userId);
        if (user == null) return "用户不存在";
        if (user.getPassword() == null || !user.getPassword().equals(oldPassword)) {
            return "旧密码错误";
        }
        if (oldPassword.equals(newPassword)) {
            return "新密码不能与旧密码相同";
        }

        user.setPassword(newPassword);
        return userMapper.updateById(user) == 1 ? "密码修改成功" : "密码修改失败";
    }
}