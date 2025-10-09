package com.middy.assignment.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;

import com.middy.assignment.model.Statistics;

@Slf4j
@Service
public class StatisticsServiceImpl implements StatisticsService {

    private final StatisticsModule statisticsModule;

    public StatisticsServiceImpl(StatisticsModule statisticsModule) {
        this.statisticsModule = statisticsModule;
    }

    @Override
    public Statistics getStatistics(long currentTimeMillis, int periodInMillis) {
        return statisticsModule.getStatistics(currentTimeMillis, periodInMillis);
    }
}
