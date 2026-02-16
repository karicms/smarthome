package com.cms.smart_home_agent.controller;

import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@Slf4j //日志工具
@RestController("/ai")
public class ChatController {
    private final ChatModel chatModel;
    public ChatController(ChatModel chatModel) {
        this.chatModel = chatModel;
    }

    @GetMapping("/chat")
    public String chat(@RequestParam String message)
    {
        log.info("收到用户提问：{}", message);
       String answer = chatModel.call(message);
       return answer;
    }

}
