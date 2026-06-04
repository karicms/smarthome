package com.cms.smart_home_agent.service;

import com.cms.smart_home_agent.config.PresenceProperties;
import com.cms.smart_home_agent.presence.IrPassageResult;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.HashOperations;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ZSetOperations;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Mock：演示毫米波与 PIR 并列取消「离家待定」、以及在启用毫米波时对静止人体的补偿路径。
 * <p>
 * 不启动 Spring 容器，仅验证与 MQTT 解耦后的核心交互（Redis + MQTT 关断由 mock 承接）。
 */
@ExtendWith(MockitoExtension.class)
class MmWavePresenceFlowMockTest {

    @Mock
    private StringRedisTemplate redis;

    @Mock
    private ZSetOperations<String, String> zset;

    @Mock
    private HashOperations<String, Object, Object> hash;

    private PresenceProperties props;
    private PresenceDetectionService presence;

    @Mock
    private DeviceService devices;

    @Mock
    private MqttService mqtt;

    @BeforeEach
    void setUp() {
        props = new PresenceProperties();
        props.setIrMaxGapMs(3000);
        props.setIrPendingTtlSeconds(15);
        props.setMmWaveEnabled(true);
        when(redis.opsForZSet()).thenReturn(zset);
        when(redis.opsForHash()).thenReturn(hash);
        presence = new PresenceDetectionService(redis, props, devices, mqtt);
    }

    @Test
    void mmWavePresenceCancelsAwayPendingLikePir() {
        when(zset.remove(anyString(), anyString())).thenReturn(1L);

        long ts = 1_700_000_000_000L;
        presence.onMmWavePresence(10, true, ts);

        ArgumentCaptor<String> keyCap = ArgumentCaptor.forClass(String.class);
        verify(zset).remove(keyCap.capture(), eq("10"));
        assertThat(keyCap.getValue()).contains("away");

        verify(hash, atLeastOnce()).put(anyString(), eq("lastMotionMs"), anyString());
        verify(hash, atLeastOnce()).put(anyString(), eq("lastMotionSource"), eq("mmwave"));
    }

    @Test
    void mmWaveIgnoredWhenDisabledEvenIfAdapterWouldReport() {
        props.setMmWaveEnabled(false);
        presence = new PresenceDetectionService(redis, props, devices, mqtt);

        presence.reportOccupancy(5, true, 1000L, 0.95);

        verify(zset, never()).remove(anyString(), anyString());
    }

    @Test
    void irSequenceWithinGapProducesExit() {
        org.springframework.data.redis.core.ValueOperations<String, String> valOps =
                org.mockito.Mockito.mock(org.springframework.data.redis.core.ValueOperations.class);
        when(redis.opsForValue()).thenReturn(valOps);
        when(valOps.get("presence:ir:pending:7")).thenReturn(null);
        IrPassageResult r1 = presence.handleIrBeam("IN", 7);
        assertThat(r1).isEqualTo(IrPassageResult.PENDING);

        when(valOps.get("presence:ir:pending:7")).thenReturn("IN:" + (System.currentTimeMillis() - 500));
        IrPassageResult r2 = presence.handleIrBeam("OUT", 7);
        assertThat(r2).isEqualTo(IrPassageResult.EXIT);
        verify(zset).add(anyString(), eq("7"), anyLong());
    }
}
