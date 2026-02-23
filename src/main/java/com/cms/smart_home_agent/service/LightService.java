package com.cms.smart_home_agent.service;

import com.cms.smart_home_agent.entity.DoorRequest;
import com.cms.smart_home_agent.entity.LightRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.openai.api.OpenAiApi;
import org.springframework.stereotype.Service;

import java.util.function.Function;

@Service
@Slf4j
public class LightService implements Function<LightRequest, String> {
    @Override
    public String apply(LightRequest lightRequest) {

            String location = lightRequest.getLocation();
            String action = lightRequest.getAction();

            log.info("lightRequest:{}", lightRequest);
            log.info("location:{}", location);
            log.info("action:{}", action);
            // 这里可以根据doorRequest中的location和action来执行相应的操作
            // 例如：
            // 根据location和action执行开灯或关灯的操作
            // 这里仅返回一个示例字符串，实际应用中应该调用相应的硬件接口来控制灯的状态
            return "已执行 " + action + " " + (location.isEmpty() ? "客厅" : location) + " 的灯";
    }
}
