package com.cms.smart_home_agent.controller;


import com.cms.smart_home_agent.entity.Family;
import com.cms.smart_home_agent.request.*;
import com.cms.smart_home_agent.entity.User;
import com.cms.smart_home_agent.service.FamilyService;
import com.cms.smart_home_agent.service.UserService;
import com.cms.smart_home_agent.vo.FamilyVo;
import com.cms.smart_home_agent.vo.Result;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@Slf4j
@RequestMapping("/aihome/user")
public class UserController {

    @Autowired
    private UserService userService;
    @Autowired
    private FamilyService familyService;

    // --- 基础用户功能 ---

    @PostMapping("/login")
    public Result login(@RequestBody UserRequest request) {
        User user = userService.login(request.getUserName(), request.getPassword());
        return user == null ? Result.fail("用户名或密码错误") : Result.success(user);
    }

    @PostMapping("/signup")
    public Result signup(@RequestBody UserRequest request) {
        return userService.signup(request.getUserName(), request.getPassword())
                ? Result.success("注册成功") : Result.fail("用户名已存在");
    }

    // --- 家庭管理功能 (适配一人多房架构) ---

    /**
     * 获取用户关联的所有家庭列表
     * 改动：不再返回单个 Family，而是返回该用户下的所有家庭
     */
    @GetMapping("/families")
    public Result getMyFamilies(@RequestParam Integer userId) {
        if (userId == null) return Result.fail("用户ID缺失");
        List<FamilyVo> families = familyService.getMyFamilies(userId);
        // 调用我们之前讨论的连表查询方法
        return Result.success(families);
    }

    @PostMapping("/family/create")
    public Result createFamily(@RequestBody createfamilyrequest request) {
        if (request.getUserid() == null) return Result.fail("用户ID缺失");
        try {
            // 注意：这里需要传入用户ID，因为创建完要自动建立关系
            Family newFamily = familyService.createFamily(request,request.getRemark());
            return Result.success(newFamily);
        } catch (Exception e) {
            return Result.fail(e.getMessage());
        }
    }

    @PostMapping("/family/join")
    public Result joinFamilyByCode(@RequestBody JoinFamilyRequest request) {
        if (request.getUserid() == null || request.getFamilyCode() == null) {
            return Result.fail("参数不完整");
        }
        try {
            // 建议在 request 里增加一个 remark 字段，比如 "我的公寓"
            familyService.joinFamilyByCode(request.getUserid(), request.getFamilyCode(), request.getRemark());
            return Result.success("成功加入家庭");
        } catch (Exception e) {
            return Result.fail(e.getMessage());
        }
    }

    /**
     * 修改家庭名称
     * 改动：直接从 request 获取 familyId，不再从 user 对象里拿
     */
    @PutMapping("/family")
    public Result updateFamilyName(@RequestBody FamilyRequest request) {
        if (request.getFamilyName() == null) {
            return Result.fail("家庭ID或名称缺失");
        }
        familyService.updateFamilyName(request.getFamilyId(),request.getFamilyName());
        return Result.success("更新成功");
    }

    @GetMapping("/family/membercount")
    public Result getFamilyMemberCount(@RequestParam Integer familyId) {
        if (familyId == null) return Result.fail("家庭ID缺失");
        try {
            int count = familyService.getFamilyMemberCount(familyId);
            return Result.success(count);
        } catch (Exception e) {
            return Result.fail(e.getMessage());
        }
    }

    @DeleteMapping("/family/leave")
    public Result deleteFamilyMember(@RequestBody FamilyMemberRequest request) {
        if (request.getFamilyid() == null || request.getUserid() == null) return Result.fail("家庭ID或用户ID缺失");
        // 这里需要判断该用户是否有权离开（比如他不是家庭创建者，或者简单处理直接允许离开）
        familyService.leaveFamily(request.getUserid(), request.getFamilyid());
        return Result.success("成功离开家庭");
    }

    @PutMapping("/family/updateremark")
    public Result updateFamilyRemark(@RequestBody FamilyMemberRequest request) {

        if(request.getUserid()==null || request.getFamilyid()==null)
        {
            return Result.fail("用户ID或家庭ID缺失");
        }
        boolean status = familyService.updatefamilyremark(request.getUserid(),request.getFamilyid(),request.getRemark());
        //更改家庭备注的话只需要验证是否有userid和famliyid，不需要判断是否存在remark
        if(status)
        return Result.success("更新成功");
        else
            return Result.fail("更新失败");
    }

}
