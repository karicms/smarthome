package com.cms.smart_home_agent.service;

import com.cms.smart_home_agent.entity.DoorRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.function.Function;

@Service
@Slf4j
public class DoorService implements Function<DoorRequest, String> {
    @Override
    public String apply(DoorRequest doorRequest) {
        // 这里可以根据doorRequest中的location和action来执行相应的操作
        // 例如：
        String location = doorRequest.getLocation();
        String action = doorRequest.getAction();
        log.info(">>>>>> [硬件指令执行中] <<<<<<");
        log.info("目标房间: {}", location.isEmpty() ? "客厅" : location);
        log.info("门的动作: {}", action);
        log.info(">>>>>> [指令发送成功] <<<<<<");
        // 根据location和action执行开门或关门的操作
        // 这里仅返回一个示例字符串，实际应用中应该调用相应的硬件接口来控制门的状态
        return "已执行 " + action + " " + (location.isEmpty() ? "客厅" : location) + " 的门";
    }
}
