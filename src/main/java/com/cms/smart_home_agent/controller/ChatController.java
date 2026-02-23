package com.cms.smart_home_agent.controller;

import com.cms.smart_home_agent.entity.ChatRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.advisor.MessageChatMemoryAdvisor;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.web.bind.annotation.*;

@Slf4j
@RestController
@RequestMapping("/ai")
public class ChatController {

    private final ChatClient chatClient;
    private final ChatMemory chatMemory;

    // 系统角色描述：通过 defaultSystem 注入，它是孤立的，不会被滑动窗口删掉
    private static final String SYSTEM_PROMPT = """
            你是一个专业且贴心的智能管家。
            1. 你可以控制空调(airConditioningControl)、灯光(lightControl)和门(doorControl)。
            2. 当用户指令模糊时，优先默认操作"客厅"，或礼貌询问。
            3. 你的目标是让居家环境更舒适。
            4.如果用户问你别的领域的问题，你也会尽力回答，展示你的智能和贴心。
            """;

    public ChatController(ChatClient.Builder chatClientBuilder, ChatMemory chatMemory) {
        this.chatMemory = chatMemory;

        // 初始化 ChatClient：这里是核心，配置一次，到处运行
        this.chatClient = chatClientBuilder
                .defaultSystem(SYSTEM_PROMPT)
                // 默认函数：AI 会根据提示词自动决定调用哪个
                .defaultFunctions("airConditioningControl", "doorControl", "lightControl")
                .build();
    }

    @PostMapping("/chat")
    public String chat(@RequestBody ChatRequest chatRequest) {
        String userInput = chatRequest.getMessage();
        String userId = chatRequest.getUserId();
        log.info("收到用户提问：{}", userInput);

        // 逻辑极其简单：ChatClient 内部会处理：
        // 1. 拼接 SYSTEM_PROMPT
        // 2. 从 chatMemory 读出最近 10 条历史
        // 3. 将 userInput 发送给模型
        // 4. 处理可能触发的 Function Calling
        // 5. 将结果自动存回 chatMemory
        return chatClient.prompt()
                .user(userInput)
                .advisors(new MessageChatMemoryAdvisor(chatMemory,userId,10))
                .call()
                .content();
    }

    @GetMapping("/clear")
    public String clear(@RequestParam String userId) {
        // 清除指定 ID 的记忆（对应 Advisor 里的 ID）
        this.chatMemory.clear(userId);
        return "用户"+ userId +"记忆清除";
    }
}