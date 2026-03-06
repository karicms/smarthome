package com.cms.smart_home_agent.service;

import com.cms.smart_home_agent.entity.ChatLog;
import com.cms.smart_home_agent.mapper.ChatLogMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@Slf4j // 添加日志支持
public class ChatService {
    @Autowired
    private ChatLogMapper chatLogMapper;

    @Autowired
    private StringRedisTemplate redisTemplate;

    private static final String REDIS_KEY_PREFIX = "chat:history:";

    // 将消息添加到 Redis 中的聊天窗口列表
    public void addToRedisWindow(Integer userId,String role,String content){
        String key = REDIS_KEY_PREFIX + userId;
        redisTemplate.opsForList().leftPush(key,role+":"+content);
        // 限制列表长度，保持最近 10 条消息
        redisTemplate.opsForList().trim(key,0,9);
    }

    @Async
    public void asyncSaveToDb(Integer userId,String role,String content)
    {
        ChatLog logEntiy = new ChatLog();
        logEntiy.setUserId(userId);
        logEntiy.setRole(role);
        logEntiy.setContent(content);
        chatLogMapper.insert(logEntiy);
    }

    public List<String> getHistory(Integer userId)
    {
        String key = REDIS_KEY_PREFIX + userId;

        List<String> history = redisTemplate.opsForList().range(key, 0, -1);

        if(history==null){
            return new java.util.ArrayList<>(); // 避免返回 null，改为返回空列表
        }

        return history;
    }

    public void clearHistory(Integer userId)
    {
        String key = REDIS_KEY_PREFIX + userId;
        redisTemplate.delete(key);
        log.info("已清除用户 {} 的聊天历史", userId);
    }

}
