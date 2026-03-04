package com.cms.smart_home_agent.service;

import com.cms.smart_home_agent.request.LightRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.function.Function;

@Service
@Slf4j
public class LightService implements Function<LightRequest, String> {
    @Override
    public String apply(LightRequest lightRequest) {

            String location = lightRequest.getLocation();
            String action = lightRequest.getAction();
            Integer userId = lightRequest.getUserId();
        //后续通过用户id去查询对应的设备id，从而发送对这个设备的控制指令
            log.info("lightRequest:{}", lightRequest);
            log.info("location:{}", location);
            log.info("action:{}", action);
            log.info("userId:{}", userId);
            // 这里可以根据doorRequest中的location和action来执行相应的操作
            // 例如：
            // 根据location和action执行开灯或关灯的操作
            // 这里仅返回一个示例字符串，实际应用中应该调用相应的硬件接口来控制灯的状态
            return "已执行 " + action + " " + (location.isEmpty() ? "客厅" : location) + " 的灯";
    }
}
