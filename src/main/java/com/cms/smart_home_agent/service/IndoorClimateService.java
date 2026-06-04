package com.cms.smart_home_agent.service;

import com.cms.smart_home_agent.entity.SensorData;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

/**
 * 室内环境读数统一入口：数据来自 MQTT {@code cms-pub} 上报，
 * 由 {@link MqttMessageProcessor} 异步写入最新 {@link SensorData}。
 * 空调习惯预测、日志入库等应从此服务取室内温度，避免各处写死常量。
 */
@Slf4j
@Service
public class IndoorClimateService {

    @Autowired
    private MqttMessageProcessor mqttMessageProcessor;

    /**
     * @return 摄氏度；尚无有效传感器数据时返回默认 26.0（与旧逻辑一致）
     */
    public double getIndoorTemperatureCelsius() {
        return getIndoorTemperatureCelsius(null);
    }

    public double getIndoorTemperatureCelsius(Integer familyId) {
        SensorData s = mqttMessageProcessor.getLastSensorData(familyId);
        double t = s.getTemperature();
        double h = s.getHumidity();
        if (!isPlausibleSample(t, h)) {
            log.debug("暂无有效温湿度 MQTT 数据，室内温度采用默认值 26°C");
            return 26.0;
        }
        return t;
    }

    public double getIndoorHumidityPercent() {
        return getIndoorHumidityPercent(null);
    }

    public double getIndoorHumidityPercent(Integer familyId) {
        SensorData s = mqttMessageProcessor.getLastSensorData(familyId);
        double t = s.getTemperature();
        double h = s.getHumidity();
        if (!isPlausibleSample(t, h)) {
            return 50.0;
        }
        return h;
    }

    /** DHT 合理范围；默认 Java Bean 未写入时为 0,0，视为未采集 */
    private static boolean isPlausibleSample(double temperatureC, double humidityPct) {
        if (Double.isNaN(temperatureC) || Double.isNaN(humidityPct)) {
            return false;
        }
        if (Math.abs(temperatureC) < 1e-6 && Math.abs(humidityPct) < 1e-6) {
            return false;
        }
        if (temperatureC < -20 || temperatureC > 55) {
            return false;
        }
        return humidityPct >= 0 && humidityPct <= 100;
    }
}
