package com.cms.smart_home_agent.controller;

import com.cms.smart_home_agent.DTO.LocationDTO;
import com.cms.smart_home_agent.service.LocationService;
import com.cms.smart_home_agent.vo.Result;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController// 这个注解表明这是一个控制器类，负责处理HTTP请求
@RequestMapping("/aihome/location")
public class LocationController {
    @Autowired
    private LocationService locationService;

    @GetMapping("/suggest")
    public Result<String> getSuggestedLocation(HttpServletRequest request) {
        String ip = locationService.getRealIp(request);
        if(ip == null) {
            return Result.fail("无法获取用户IP地址");
        }
        LocationDTO suggestion = locationService.getCityByIp(ip);

        return Result.success(suggestion);
    }
}
