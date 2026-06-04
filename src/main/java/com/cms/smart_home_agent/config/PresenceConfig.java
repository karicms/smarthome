package com.cms.smart_home_agent.config;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@EnableConfigurationProperties(PresenceProperties.class)
public class PresenceConfig {
}
