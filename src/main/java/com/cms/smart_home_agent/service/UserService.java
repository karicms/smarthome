package com.cms.smart_home_agent.service;


import com.cms.smart_home_agent.entity.User;
import com.cms.smart_home_agent.mapper.UserMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class UserService {

    @Autowired
    private UserMapper userMapper;

    /**
     * 登录逻辑：返回查到的 User 或者 null
     */
    public User login(String userName, String password) {
        return userMapper.findByUsernameAndPassword(userName, password);
    }

    /**
     * 注册逻辑：先用 countByUsername 查重，再插入新用户
     */
    public boolean signup(String userName, String password) {
        Integer count = userMapper.countByUsername(userName);
        if (count != null && count > 0) {
            // 已有相同用户名
            return false;
        }
        User u = new User();
        u.setUserName(userName);
        u.setPassword(password);
        userMapper.insert(u);
        return true;
    }

    /**
     * 获取用户信息
     * ID 现为 Integer 类型
     */
    public User get(Integer id) { // 🚨 修正：id 参数改为 Integer
        return userMapper.selectById(id);
    }

    /**
     * 更新用户信息
     */
    public boolean updateUser(User user) {
        // 假设 user.getId() 字段现在是 Integer
        int rows = userMapper.updateById(user);
        return rows == 1;
    }
}
