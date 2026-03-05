package com.cms.smart_home_agent.controller;

import com.cms.smart_home_agent.DTO.LocationDTO;
import com.cms.smart_home_agent.mapper.UserMapper;
import com.cms.smart_home_agent.request.ChatRequest;
import com.cms.smart_home_agent.service.FamilyService;
import com.cms.smart_home_agent.service.LocationService;
import com.cms.smart_home_agent.service.UserService;
import com.cms.smart_home_agent.vo.FamilyVo;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.advisor.MessageChatMemoryAdvisor;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@RestController
@RequestMapping("/ai")
public class ChatController {

    private final ChatClient chatClient;
    private final ChatMemory chatMemory;
    private final LocationService locationService;
    private final FamilyService familyService;

    // 系统角色描述：增加了动态参数占位符 {currentCity} 和 {userId}
    private static final String SYSTEM_PROMPT = """
            你是一个专业且贴心的智能管家。
            1. 当前用户 ID 是：{userId}，用户当前所在城市是：{currentCity}。用户的房子在{familylocations}。
            2. 你可以控制空调(airConditioningControl)、灯光(lightControl)和门(doorControl)。
            3. 当用户指令模糊时，优先默认操作"客厅"，或礼貌询问。如果用户询问推荐温度或要求按习惯设置空调，请务必调用 airConditioningControl 工具，且不要传递 temperature 参数，由系统自动预测。。
            4. 你的目标是让居家环境更舒适。
            5. 如果用户询问当地天气或环境，请参考用户所在的当前城市 {currentCity}。
            6. 如果用户问你别的领域的问题，你也会尽力回答。
            7、用户问你家庭相关问题是优先给出用户所在地的家庭，如果没有的话就问用户是想要操作哪个家庭。
            """;

    public ChatController(ChatClient.Builder chatClientBuilder, ChatMemory chatMemory, LocationService locationService, FamilyService familyService) {
       this.familyService = familyService;
        this.chatMemory = chatMemory;
        this.locationService = locationService;

        // 初始化 ChatClient
        this.chatClient = chatClientBuilder
                .defaultSystem(SYSTEM_PROMPT)
                .defaultFunctions("airConditioningControl", "doorControl", "lightControl", "outdoorWeatherFunction")
                .build();
    }

    @PostMapping("/chat")
    public String chat(@RequestBody ChatRequest chatRequest, HttpServletRequest request) {
        // 1. 获取真实 IP 并解析城市
         String realIp = locationService.getRealIp(request);
         LocationDTO location = new LocationDTO();
         location = locationService.getCityByIp(realIp);
         String currentCity = location.getCity();
        String userInput = chatRequest.getMessage();
        Integer userId = chatRequest.getUserId();
        String userid = String.valueOf(userId);

        List<FamilyVo> families = familyService.getMyFamilies(userId);

        String familylocations = families.isEmpty() ? "暂无数据..." : families.stream()
                        .map(f->String.format("家庭id：%d,城市：%s,城市编码:%s",f.getId(),f.getCity(),f.getAdcode()))
                        .collect(Collectors.joining(";"));
        log.info("用户 {} 关联的家庭列表: {}", userId, families);

        log.info("用户ID: {}, IP: {}, 城市: {}, 提问: {}", userId, realIp, currentCity, userInput);

        // 2. 发起 AI 调用
        return chatClient.prompt()
                .system(s -> s.param("currentCity", currentCity)
                        .param("userId", String.valueOf(userId))
                        .param("familylocations",familylocations)) // 动态注入参数到 SYSTEM_PROMPT
                .user(userInput)
                .advisors(new MessageChatMemoryAdvisor(chatMemory,userid,10)) // 记忆顾问，关联用户ID，保留最近10条对话
                .call()
                .content();
    }


    @GetMapping("/clear")
    public String clear(@RequestParam String userId) {
        this.chatMemory.clear(userId);
        return "用户 " + userId + " 记忆清除";
    }
}