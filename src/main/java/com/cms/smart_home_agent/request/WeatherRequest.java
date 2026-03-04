package com.cms.smart_home_agent.request;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

@Data
public class WeatherRequest {
    @JsonProperty("要查询的天气的名称，例如：北京、上海等")
    private String city; // 城市名称，例如：北京、上海等

}
