package com.middy.assignment.controller;

import java.math.BigDecimal;
import java.time.Clock;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeEach;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import com.middy.assignment.model.Statistics;
import com.middy.assignment.service.StatisticsService;

import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(StatisticsController.class)
class StatisticsControllerTest {

    @Autowired
    private MockMvc mockMvc;

    private static StatisticsService mockStatisticsService;
    private static Clock mockClock;

    @TestConfiguration
    static class TestConfig {
        @Bean
        public StatisticsService statisticsService() {
            mockStatisticsService = Mockito.mock(StatisticsService.class);
            return mockStatisticsService;
        }

        @Bean
        public Clock clock() {
            mockClock = Mockito.mock(Clock.class);
            return mockClock;
        }
    }

    @BeforeEach
    void setUp() {
        Mockito.reset(mockStatisticsService, mockClock);
    }

    @Test
    void getStatistics_success_returns200WithValidJson() throws Exception {
        // Given
        long currentTime = 1696800000000L; // Fixed timestamp for predictable testing
        when(mockClock.millis()).thenReturn(currentTime);

        Statistics mockStats = new Statistics(
                new BigDecimal("1500.50").setScale(2),
                new BigDecimal("300.10").setScale(2),
                new BigDecimal("500.25").setScale(2),
                new BigDecimal("100.75").setScale(2),
                5L
        );

        when(mockStatisticsService.getStatistics(currentTime, 60000)).thenReturn(mockStats);

        // When & Then
        mockMvc.perform(get("/v1/statistics"))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.sum").value("1500.50"))
                .andExpect(jsonPath("$.avg").value("300.10"))
                .andExpect(jsonPath("$.max").value("500.25"))
                .andExpect(jsonPath("$.min").value("100.75"))
                .andExpect(jsonPath("$.count").value(5));

        verify(mockStatisticsService).getStatistics(currentTime, 60000);
    }

    @Test
    void getStatistics_emptyStatistics_returns200WithZeroValues() throws Exception {
        // Given
        long currentTime = 1696800000000L;
        when(mockClock.millis()).thenReturn(currentTime);

        Statistics emptyStats = new Statistics(
                BigDecimal.ZERO,
                BigDecimal.ZERO,
                BigDecimal.ZERO,
                BigDecimal.ZERO,
                0L
        );

        when(mockStatisticsService.getStatistics(currentTime, 60000)).thenReturn(emptyStats);

        // When & Then
        mockMvc.perform(get("/v1/statistics"))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.sum").value("0"))
                .andExpect(jsonPath("$.avg").value("0"))
                .andExpect(jsonPath("$.max").value("0"))
                .andExpect(jsonPath("$.min").value("0"))
                .andExpect(jsonPath("$.count").value(0));

        verify(mockStatisticsService).getStatistics(currentTime, 60000);
    }

    @Test
    void getStatistics_serviceThrowsException_returns500() throws Exception {
        // Given
        long currentTime = 1696800000000L;
        when(mockClock.millis()).thenReturn(currentTime);

        when(mockStatisticsService.getStatistics(anyLong(), anyInt()))
                .thenThrow(new RuntimeException("Unknoewn error"));

        // When & Then
        mockMvc.perform(get("/v1/statistics"))
                .andExpect(status().isInternalServerError())
                .andExpect(content().string(""));

        verify(mockStatisticsService).getStatistics(currentTime, 60000);
    }

    @Test
    void getStatistics_serviceThrowsNullPointerException_returns500() throws Exception {
        // Given
        long currentTime = 1696800000000L;
        when(mockClock.millis()).thenReturn(currentTime);

        when(mockStatisticsService.getStatistics(anyLong(), anyInt()))
                .thenThrow(new NullPointerException("Null reference"));

        // When & Then
        mockMvc.perform(get("/v1/statistics"))
                .andExpect(status().isInternalServerError())
                .andExpect(content().string(""));

        verify(mockStatisticsService).getStatistics(currentTime, 60000);
    }

    @Test
    void getStatistics_largeNumbers_returns200WithCorrectPrecision() throws Exception {
        // Given
        long currentTime = 1696800000000L;
        when(mockClock.millis()).thenReturn(currentTime);

        Statistics largeStats = new Statistics(
                new BigDecimal("999999999.99").setScale(2),
                new BigDecimal("123456789.12").setScale(2),
                new BigDecimal("888888888.88").setScale(2),
                new BigDecimal("0.01").setScale(2),
                1000000L
        );

        when(mockStatisticsService.getStatistics(currentTime, 60000)).thenReturn(largeStats);

        // When & Then
        mockMvc.perform(get("/v1/statistics"))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.sum").value("999999999.99"))
                .andExpect(jsonPath("$.avg").value("123456789.12"))
                .andExpect(jsonPath("$.max").value("888888888.88"))
                .andExpect(jsonPath("$.min").value("0.01"))
                .andExpect(jsonPath("$.count").value(1000000));

        verify(mockStatisticsService).getStatistics(currentTime, 60000);
    }

    @Test
    void getStatistics_verifyCorrectPeriodIsUsed() throws Exception {
        long currentTime = 1696800000000L;
        when(mockClock.millis()).thenReturn(currentTime);

        Statistics mockStats = new Statistics(
                BigDecimal.ONE, BigDecimal.ONE, BigDecimal.ONE, BigDecimal.ONE, 1L
        );

        when(mockStatisticsService.getStatistics(currentTime, 60000)).thenReturn(mockStats);

        mockMvc.perform(get("/v1/statistics"))
                .andExpect(status().isOk());

        verify(mockStatisticsService).getStatistics(currentTime, 60000);
    }
}
