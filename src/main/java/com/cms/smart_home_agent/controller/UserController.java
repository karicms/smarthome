package com.cms.smart_home_agent.controller;


import com.cms.smart_home_agent.entity.Family;
import com.cms.smart_home_agent.entity.FamilyRequest;
import com.cms.smart_home_agent.entity.User;
import com.cms.smart_home_agent.entity.UserRequest;
import com.cms.smart_home_agent.service.FamilyService;
import com.cms.smart_home_agent.service.UserService;
import com.cms.smart_home_agent.vo.Result;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

@RestController
@Slf4j
@RequestMapping("/aihome/user")
public class UserController {

    @Autowired
    private UserService userService;

    @Autowired
    private FamilyService familyService; // 注入 FamilyService

    /** * 注意：此方法已废弃，在实际项目中应通过 Token/Session 获取用户ID。
     * 在下面的方法中，我们直接从请求参数或请求体中获取用户ID。
     */
    private Integer getCurrentUserId() {
        return null;
    }

    // --- 基础用户功能 ---

    @PostMapping("/login")
    public Result login(@RequestBody UserRequest request) {
        log.info("用户 {} 尝试登录", request.getUserName());
        User user = userService.login(request.getUserName(), request.getPassword());
        if (user == null) {
            log.error("登录失败，用户名或密码错误");
            return Result.fail("登录失败，用户名或密码错误");
        }
        // !!! 关键：返回给前端的 ID 必须是 Long 或 String，确保前端不丢失精度 !!!
        // 由于数据库已改为 Integer，这里需要确保返回的 User 对象的 ID 字段也是 Integer
        return Result.success(user);
    }

    @PostMapping("/signup")
    public Result signup(@RequestBody UserRequest request) {
        log.info("用户 {} 尝试注册", request.getUserName());
        boolean success = userService.signup(request.getUserName(), request.getPassword());
        if (!success) {
            log.error("注册失败，用户名已存在");
            return Result.fail("注册失败，用户名已存在");
        }
        return Result.success("注册成功");
    }

    @PutMapping("/update")
    public Result updateUser(@RequestBody User user) {
        log.info("更新用户信息: {}", user);
        // 假设 User.getId() 现在返回 Integer
        if (user.getId() == null) {
            return Result.fail("缺少用户ID");
        }
        boolean success = userService.updateUser(user);
        if (!success) {
            return Result.fail("更新失败");
        }
        return Result.success("用户信息更新成功");
    }

    /**
     * GET /user/info/{id}
     * 获取用户信息，ID改为 Integer
     */
    @GetMapping("/info/{id}")
    public Result getUserInfo(@PathVariable Integer id) { // 🚨 修正：路径变量 ID 改为 Integer
        log.info("获取用户信息，ID: {}", id);
        User user = userService.get(id); // 假设 Service 层方法已接受 Integer
        if (user == null) {
            return Result.fail("用户不存在");
        }
        return Result.success(user);
    }

    // --- 家庭管理功能 (所有 ID 统一使用 Integer) ---

    /**
     * POST /user/family/create
     * 创建新家庭
     * 请求体: { "userId": 123, "familyName": "XX家庭" }
     */
    @PostMapping("/family/create")
    public Result createFamily(@RequestBody FamilyRequest request) {
        String familyName = request.getFamilyName();
        // 从请求体获取 userId (Integer)
        Integer currentUserId = request.getUserid();

        if (currentUserId == null) {
            return Result.fail("用户ID缺失，无法创建家庭");
        }

        try {
            Family newFamily = familyService.createFamily(familyName, currentUserId);
            return Result.success(newFamily);
        } catch (RuntimeException e) {
            log.error("创建家庭失败: {}", e.getMessage());
            return Result.fail(e.getMessage());
        }
    }

    /**
     * GET /user/family?userId=123
     * 查询当前用户所属的家庭信息
     */
    @GetMapping("/family")
    // 从 Query Parameter 获取 userId (Integer)
    public Result getMyFamily(@RequestParam Integer userId) {
        if (userId == null) {
            return Result.fail("用户ID缺失，无法查询家庭信息");
        }

        Family family = familyService.getMyFamily(userId);
        if (family == null) {
            // 如果用户未加入家庭，返回成功但数据为空
            return Result.success(null);
        }
        return Result.success(family);
    }

    /**
     * PUT /user/family
     * 修改家庭名称
     * 请求体: { "userId": 123, "familyId": 456, "familyName": "新家庭名" }
     */
    @PutMapping("/family")
    public Result updateFamilyName(@RequestBody FamilyRequest request) {
        String newName = request.getFamilyName();
        // 从请求体获取 userId (Integer)
        Integer currentUserId = request.getUserid();
        // 🚨 修正：从请求体获取 familyId (Integer)
        Integer familyId = userService.get(currentUserId).getFamilyId();

        if (currentUserId == null || familyId == null) {
            return Result.fail("用户ID或家庭ID缺失");
        }
        if (newName == null || newName.trim().isEmpty()) {
            return Result.fail("家庭名称不能为空");
        }

        try {
            // 传递 familyId (Integer) 和 userId (Integer) 给 Service 层
            familyService.updateFamilyName(familyId, newName, currentUserId);
            return Result.success("家庭名称更新成功");
        } catch (RuntimeException e) {
            log.error("修改家庭名称失败: {}", e.getMessage());
            return Result.fail(e.getMessage());
        }
    }

    /**
     * DELETE /user/family
     * 解散/删除家庭
     * 请求体: { "userId": 123, "familyId": 456 }
     */
    @DeleteMapping("/family")
    public Result deleteFamily(@RequestBody FamilyRequest request) {
        // 从请求体获取 userId (Integer)
        Integer currentUserId = request.getUserid();
        // 🚨 修正：从请求体获取 familyId (Integer)
        Integer familyId = userService.get(currentUserId).getFamilyId();

        if (currentUserId == null || familyId == null) {
            return Result.fail("用户ID或家庭ID缺失");
        }

        try {
            // 传递 familyId (Integer) 和 userId (Integer) 给 Service 层
            familyService.deleteFamily(familyId, currentUserId);
            return Result.success("家庭删除成功，所有成员已解除关联");
        } catch (RuntimeException e) {
            log.error("删除家庭失败: {}", e.getMessage());
            return Result.fail(e.getMessage());
        }
    }

    /**
     * POST /user/family/join
     * 通过家庭码加入家庭
     * 请求体: { "userId": 123, "familyCode": "ABCDEFGH" }
     */
    @PostMapping("/family/join")
    public Result joinFamilyByCode(@RequestBody FamilyRequest request) {
        String familyCode = request.getFamilyCode();
        // 从请求体获取 userId (Integer)
        Integer currentUserId = request.getUserid();

        if (currentUserId == null) {
            return Result.fail("用户ID缺失，无法加入家庭");
        }

        try {
            Family joinedFamily = familyService.joinFamilyByCode(familyCode, currentUserId);
            return Result.success(joinedFamily);
        } catch (RuntimeException e) {
            log.error("加入家庭失败: {}", e.getMessage());
            return Result.fail(e.getMessage());
        }
    }

    /**
     * POST /user/family/leave
     * 退出当前所属的家庭
     * 请求体: { "userId": 123 }
     */
    @PostMapping("/family/leave")
    public Result leaveFamily(@RequestBody FamilyRequest request) {
        // 从请求体获取 userId (Integer)
        Integer currentUserId = request.getUserid();

        if (currentUserId == null) {
            return Result.fail("用户ID缺失，无法退出家庭");
        }

        try {
            familyService.leaveFamily(currentUserId);
            return Result.success("成功退出家庭");
        } catch (RuntimeException e) {
            log.error("退出家庭失败: {}", e.getMessage());
            return Result.fail(e.getMessage());
        }
    }
}
