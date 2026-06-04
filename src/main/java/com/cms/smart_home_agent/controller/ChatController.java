package com.cms.smart_home_agent.controller;

import com.cms.smart_home_agent.request.ChatRequest;
import com.cms.smart_home_agent.service.ChatService;
import com.cms.smart_home_agent.service.DeviceService;
import com.cms.smart_home_agent.service.FamilyService;
import com.cms.smart_home_agent.service.LocationService;
import com.cms.smart_home_agent.vo.FamilyVo;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@RestController
@RequestMapping("/ai")
public class ChatController {

    private final ChatClient chatClient;
    private final LocationService locationService;
    private final FamilyService familyService;
    private final ChatService chatService;
    private final DeviceService deviceService;

    // ✨ 1. 更新后的系统提示词：增加了 {deviceList} 占位符和更严格的约束
    private static final String SYSTEM_PROMPT = """
        你是一个专业且贴心的智能管家，名字叫田玺。
        1. 当前用户 ID 是：{userId}。
        2. 用户当前所在城市是：{currentCity}。
        3. 用户当前选中的家庭 ID 是：{activeFamilyId}，位置在：{activeFamilyLocation}。
        4. 用户的全部房子信息如下：{familylocations}。
        5. ✨ 当前选中家庭（ID:{activeFamilyId}）包含的真实设备列表如下：
           {deviceList}
            6. 【极其重要】设备匹配逻辑：
            - 优先匹配：调用控制工具时，必须优先从 {deviceList} 中选出名称最契合的设备。
            - 模糊推理：如果用户只说“开灯”且设备列表中没有包含位置的名称（如只有“LED”或“灯”），请结合当前家庭位置 {activeFamilyLocation} 和设备类型（如 LIGHT/LED）来推断用户指的是当前环境下的该设备。
            - 严禁伪造：如果通过以上逻辑仍无法在 {deviceList} 中找到对应设备，请直接询问用户具体要控制哪一个，绝不要凭空捏造列表中不存在的 deviceName。
        7. 当用户指令模糊（如“开灯”）时，请【务必优先】操作当前选中的家庭（ID:{activeFamilyId}）。
        8. 如果用户提到具体的家庭 ID（如“开家庭2的灯”），请操作对应家庭。
        9. 如果用户询问推荐温度，请调用 airConditioningControl 工具，不要传递 temperature 参数。
        10. 你的目标是让居家环境更舒适。
        """;

    public ChatController(ChatClient.Builder chatClientBuilder, LocationService locationService,
                          FamilyService familyService, ChatService chatService,
                          DeviceService deviceService) {
        this.deviceService = deviceService;
        this.familyService = familyService;
        this.chatService = chatService;
        this.locationService = locationService;

        this.chatClient = chatClientBuilder
                .defaultSystem(SYSTEM_PROMPT)
                .defaultFunctions("airConditioningControl", "doorControl", "lightControl", "outdoorWeatherFunction")
                .build();
    }

    @PostMapping("/chat")
    public String chat(@RequestBody ChatRequest chatRequest, HttpServletRequest request) {
        String realIp = locationService.getRealIp(request);
        String currentCity = locationService.getCityByIp(realIp).getCity();
        String userInput = chatRequest.getMessage();
        Integer userId = chatRequest.getUserId();
        Integer activeFamilyId = chatRequest.getFamilyId();

        String historyText = getHistoryContext(userId);
        List<FamilyVo> families = familyService.getMyFamilies(userId);
        String familylocations = formatFamilies(families);

        String activeFamilyLoc = families.stream()
                .filter(f -> f.getId().equals(activeFamilyId))
                .map(f -> f.getCity() + " " + f.getRemark())
                .findFirst()
                .orElse("未知地点");

        // ✨ 调用你方案二中在 DeviceService 定义的方法
        String deviceListText = deviceService.getDeviceListForAi(activeFamilyId);

        log.info("用户 {} 操作家庭: {}, 设备列表长度: {}", userId, activeFamilyId, deviceListText.length());

        String aiResponse = chatClient.prompt()
                .system(s -> s.param("currentCity", currentCity)
                        .param("userId", String.valueOf(userId))
                        .param("activeFamilyId", String.valueOf(activeFamilyId))
                        .param("activeFamilyLocation", activeFamilyLoc)
                        .param("familylocations", familylocations)
                        .param("deviceList", deviceListText)) // ✨ 注入设备列表
                .user(historyText + "\n--- 当前指令 ---\n" + userInput)
                .call()
                .content();

        saveChatHistory(userId, userInput, aiResponse);
        return aiResponse;
    }

    @GetMapping(value = "/chat/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter chatStream(@RequestParam Integer userId,
                                 @RequestParam String message,
                                 @RequestParam Integer familyId) {
        SseEmitter emitter = new SseEmitter(60000L);

        new Thread(() -> {
            try {
                // 1. 准备上下文数据
                String realIp = "127.0.0.1";
                String currentCity = locationService.getCityByIp(realIp).getCity();
                List<FamilyVo> families = familyService.getMyFamilies(userId);

                String familylocations = formatFamilies(families);
                String activeFamilyLoc = families.stream()
                        .filter(f -> f.getId().equals(familyId))
                        .map(f -> f.getCity() + " " + f.getRemark())
                        .findFirst()
                        .orElse("未知地点");

                String historyText = getHistoryContext(userId);

                // ✨ 获取当前选中的家庭设备列表
                String deviceListText = deviceService.getDeviceListForAi(familyId);

                // 2. AI 调用 (修正了之前变量引用的错误)
                String aiResponse = chatClient.prompt()
                        .system(s -> s.param("currentCity", currentCity)
                                .param("userId", String.valueOf(userId))
                                .param("activeFamilyId", String.valueOf(familyId))
                                .param("activeFamilyLocation", activeFamilyLoc)
                                .param("familylocations", familylocations)
                                .param("deviceList", deviceListText)) // ✨ 注入
                        .user(historyText + "\n--- 当前指令 ---\n" + message)
                        .call()
                        .content();

                // 3. 模拟流式输出
                if (aiResponse != null) {
                    for (char c : aiResponse.toCharArray()) {
                        emitter.send(SseEmitter.event().data(String.valueOf(c)));
                        Thread.sleep(30);
                    }
                }

                emitter.send(SseEmitter.event().data("[DONE]"));
                emitter.complete();

                saveChatHistory(userId, message, aiResponse);

            } catch (Exception e) {
                log.error("流式聊天失败", e);
                completeWithError(emitter, e);
            }
        }).start();

        return emitter;
    }

    // --- 工具方法保持不变 ---

    private String getHistoryContext(Integer userId) {
        List<String> history = chatService.getHistory(userId);
        if (history.isEmpty()) return "";
        java.util.Collections.reverse(history);
        return "\n--- 最近对话历史 ---\n" + String.join("\n", history);
    }

    private String formatFamilies(List<FamilyVo> families) {
        if (families.isEmpty()) return "暂无数据...";
        return families.stream()
                .map(f -> String.format("家庭ID:%d, 城市:%s, 备注:%s", f.getId(), f.getCity(), f.getRemark()))
                .collect(Collectors.joining("; "));
    }

    private void saveChatHistory(Integer userId, String userMsg, String aiMsg) {
        chatService.addToRedisWindow(userId, "user", userMsg);
        chatService.asyncSaveToDb(userId, "user", userMsg);
        chatService.addToRedisWindow(userId, "ai", aiMsg);
        chatService.asyncSaveToDb(userId, "ai", aiMsg);
    }

    private void completeWithError(SseEmitter emitter, Exception e) {
        try {
            emitter.send(SseEmitter.event().data("抱歉，出错了: " + e.getMessage()));
            emitter.send(SseEmitter.event().data("[DONE]"));
            emitter.complete();
        } catch (Exception ignored) {}
    }

    @GetMapping("/clear")
    public String clear(@RequestParam Integer userId) {
        chatService.clearHistory(userId);
        return "用户 " + userId + " 记忆清除";
    }
}