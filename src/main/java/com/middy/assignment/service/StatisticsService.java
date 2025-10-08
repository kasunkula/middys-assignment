package com.middy.assignment.service;

import com.middy.assignment.model.Statistics;

public interface StatisticsService {
    Statistics getStatistics(long currentTimeMillis, int periodInMillis);
}
