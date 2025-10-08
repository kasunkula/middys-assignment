package com.middy.assignment.controller;

import java.time.Clock;

import com.middy.assignment.model.Statistics;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.middy.assignment.dto.StatisticsDto;
import com.middy.assignment.service.StatisticsService;


@Slf4j
@RestController
@RequestMapping("/v1/statistics")
public class StatisticsController {

    private final int statisticsPeriodInMillis;
    private final StatisticsService statisticsService;
    private final Clock clock;

    public StatisticsController(StatisticsService statisticsService, Clock clock, @Value("${statistics.period.millis:60000}") int statisticsPeriodInMillis) {
        this.statisticsService = statisticsService;
        this.clock = clock;
        this.statisticsPeriodInMillis = statisticsPeriodInMillis;
    }

    @GetMapping
    public ResponseEntity<StatisticsDto> getStatistics() {
        try {
            Statistics stat = statisticsService.getStatistics(clock.millis(), statisticsPeriodInMillis);
            return ResponseEntity.ok(new StatisticsDto(stat));
        } catch (Exception e) {
            log.error("Error querying statistics", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }
}
