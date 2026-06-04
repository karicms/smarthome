package com.cms.smart_home_agent.controller;

import com.cms.smart_home_agent.DTO.LocationDTO;
import com.cms.smart_home_agent.service.LocationService;
import com.cms.smart_home_agent.vo.Result;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

@Slf4j
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
    @PostMapping("/cityadcode")
    public Result<String> getCityAdcode(@RequestBody LocationDTO locaiton) {
        String cityname = locaiton.getCity();
        log.info("cityname:{}",cityname);
        if (cityname == null || cityname.isEmpty()) {
            return Result.fail("城市名称不能为空");
        }
        return Result.success(locationService.getlocationbycityname(cityname));
    }
}
