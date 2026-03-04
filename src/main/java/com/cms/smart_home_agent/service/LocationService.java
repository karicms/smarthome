package com.cms.smart_home_agent.service;

import com.cms.smart_home_agent.DTO.LocationDTO;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

@Service
@Slf4j
public class LocationService {
    private final ObjectMapper objectMapper;
    @Value("${map.gaodemap.key}")
    private String ampakey;

    private final RestClient restClient = RestClient.create(); //创建RestClient实例

    public LocationService(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    public LocationDTO getCityByIp(String ip)
    {
        if(ip.equals("127.0.0.1"))
        {
            return new LocationDTO("上海市","上海市","310000");
        }
        try{
            String response = restClient.get()
                    .uri("https://restapi.amap.com/v3/ip?ip={ip}&output=json&key={key}",ip,ampakey)// 构建请求URI，替换参数
                    .retrieve() // 发送GET请求并获取响应
                    .body(String.class); // 将响应体转换为字符串
            log.info(">>> [LocationService] 获取到的位置信息: {}", response);
            JsonNode root = objectMapper.readTree(response); // 解析JSON响应
            // 高德 API 成功状态 status 为 "1"
            if ("1".equals(root.path("status").asText())) {
                String city = root.path("city").asText();
                String adcode = root.path("adcode").asText();
                String province = root.path("province").asText();

                // 有时候 IP 只能定位到省，城市字段可能是空字符串或 "[]"
                if (city != null && !city.isEmpty() && !city.equals("[]")) {
                    return new LocationDTO(province, city, adcode);
                }

                // 如果没有城市，尝试返回省份
                if (province != null && !province.isEmpty() && !province.equals("[]")) {
                    return new LocationDTO(province, province, adcode);
                }
            }
        }catch (Exception e){
            log.error(e.getMessage());
        }
        return new LocationDTO("未知","未知","000000");
    }
    /**
     * 获取真实 IP 的核心逻辑
     * 原理：穿透 Nginx 等代理服务器获取客户端真实地址
     */
    public String getRealIp(HttpServletRequest request) {
        String ip = request.getHeader("X-Forwarded-For");
        if (ip != null && ip.length() != 0 && !"unknown".equalsIgnoreCase(ip)) {
            // 多层代理时，取第一个 IP
            if (ip.contains(",")) {
                ip = ip.split(",")[0];
            }
        }
        if (ip == null || ip.length() == 0 || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getHeader("Proxy-Client-IP");
        }
        if (ip == null || ip.length() == 0 || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getHeader("WL-Proxy-Client-IP");
        }
        if (ip == null || ip.length() == 0 || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getHeader("X-Real-IP");
        }
        if (ip == null || ip.length() == 0 || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getRemoteAddr();
        }
        // 处理本地 IPv6 地址
        return "0:0:0:0:0:0:0:1".equals(ip) ? "127.0.0.1" : ip;
    }

}
