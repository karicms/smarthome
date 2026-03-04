package com.cms.smart_home_agent.service;

import com.cms.smart_home_agent.request.WeatherRequest;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import java.util.function.Function;
import java.util.zip.GZIPInputStream;


@Service
public class WeatherService implements Function<WeatherRequest, String> {
    @Value("${weather.qweather.key}")
    private String apikey;

    private final RestTemplate restTemplate; // 注入RestTemplate

    public WeatherService(RestTemplate restTemplate) {
        this.restTemplate = restTemplate;
    }


    @Override
    public String apply(WeatherRequest weatherRequest) {
        // 1. 获取并校验城市名
        String cityName = weatherRequest.getCity();
        if (cityName == null || cityName.trim().isEmpty()) {
            return "AI 没有提供有效的城市名称，请指定一个城市（如：北京）。";
        }

        // 2. 调试日志：检查配置是否生效（记得确认控制台打印的内容）
        System.out.println(">>> [WeatherService] 收到查询请求: " + cityName);
        System.out.println(">>> [WeatherService] 当前使用的 Key: " + (apikey != null ? "已读取" : "未读取！请检查配置"));

        try {
            // --- 步骤一：查询城市 ID ---
            // 使用更稳健的 URL 构建方式
            String city = weatherRequest.getCity();
            String locationUrl = "https://kf7p4385uj.re.qweatherapi.com/geo/v2/city/lookup?location="+city+ "&key=" + apikey;

            byte[] geoBytes = restTemplate.getForObject(locationUrl, byte[].class);
            String geoJson = handleResponse(geoBytes);

            String locationId = parseLocationId(geoJson);

//            List<Map<String, Object>> locationList = (List<Map<String, Object>>) geoResponse.get("location");
//            if (locationList == null || locationList.isEmpty()) {
//                return "没找到名为 '" + cityName + "' 的城市。";
//            }


            // --- 步骤二：查询实时天气 ---
            // 注意：如果是免费开发版，域名必须是 devapi
            String weatherLUrl = "https://kf7p4385uj.re.qweatherapi.com/v7/weather/now?location={id}&key={key}";
//            Map<String, Object> weatherResponse = restTemplate.getForObject(weatherLUrl, Map.class, locationId, apikey);
            byte[] weatherBytes = restTemplate.getForObject(weatherLUrl, byte[].class, locationId, apikey);
            String weatherJson = handleResponse(weatherBytes);


//            if (weatherResponse != null && "200".equals(weatherResponse.get("code"))) {
//                Map<String, Object> now = (Map<String, Object>) weatherResponse.get("now");
//                String temp = (String) now.get("temp");
//                String text = (String) now.get("text"); // 加上天气描述，比如“晴”
//                return "当前" + cityName + "温度为：" + temp + "°C，天气情况：" + text;
//            }
            return weatherJson;

        } catch (Exception e) {
            // 这里打印堆栈跟踪，方便你看到具体的 URL 错误
            e.printStackTrace();
            return "获取天气异常: " + e.getMessage();
        }
//        return "获取天气信息失败。";
    }

    private String handleResponse(byte[] bytes) {
        if(bytes == null || bytes.length == 0)
        return "";
        //检查Gzip压缩标志（31 139）
        if(bytes.length > 0 && bytes[0] == 31)
        {
            try(GZIPInputStream gis = new GZIPInputStream(new ByteArrayInputStream(bytes))) {
                return new String(gis.readAllBytes(), StandardCharsets.UTF_8);

            }
                catch (Exception e) {
                    e.printStackTrace();
                    return "解压失败"+ e.getMessage(); // 解压失败
                }
        }
        return new String(bytes, StandardCharsets.UTF_8); // 没有压缩，直接转换
    }

    /**
     * 含义：解析城市搜索返回的 JSON
     * 目标：拿到 location 数组中第一个匹配城市的 id
     */
    private String parseLocationId(String geoJson) {
        try {
            ObjectMapper mapper = new ObjectMapper();
            JsonNode root = mapper.readTree(geoJson);

            // 1. 检查 API 状态码
            String code = root.get("code").asText();
            if (!"200".equals(code)) {
                System.err.println("城市搜索返回错误码: " + code);
                return null;
            }

            // 2. 获取 location 数组
            JsonNode locations = root.get("location");

            // 3. 健壮性检查：确保找到了至少一个地方
            if (locations != null && locations.isArray() && !locations.isEmpty()) {
                // 获取第一个城市节点（通常是 Rank 评分最高的，最准确）
                JsonNode firstCity = locations.get(0);

                String id = firstCity.get("id").asText();
                String name = firstCity.get("name").asText();
                String adm1 = firstCity.get("adm1").asText(); // 省份/直辖市

                System.out.println(">>> 匹配到城市: " + name + " (" + adm1 + "), ID: " + id);
                return id;
            }
        } catch (Exception e) {
            System.err.println("解析城市 JSON 失败: " + e.getMessage());
        }
        return null;
    }
}
