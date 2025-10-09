package com.middy.assignment.service;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.util.concurrent.atomic.AtomicReferenceArray;

import com.middy.assignment.exception.OrderValidationException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import com.middy.assignment.model.InterimStatistics;
import com.middy.assignment.model.Order;
import com.middy.assignment.model.Statistics;

@Slf4j
@Component
public class StatisticsModule {

    private final int statisticsPeriodInMillis;

    private static final int STATS_SCALE = 2;

    private final Clock clock;

    private final AtomicReferenceArray<InterimStatistics> interimStatistics;

    public StatisticsModule(Clock clock, @Value("${stats-period-in-milliseconds}") int statisticsPeriodInMillis) {
        this.clock = clock;
        this.statisticsPeriodInMillis = statisticsPeriodInMillis;
        interimStatistics = new AtomicReferenceArray<>(statisticsPeriodInMillis + 1);
        for (int i = 0; i < statisticsPeriodInMillis + 1; i++) {
            interimStatistics.set(i, new InterimStatistics());
        }
    }

    public void addOrder(Order newOrder) {
        long now = clock.millis();

        if (newOrder.getTimestamp() <= (now - 60000)) {
            throw new OrderValidationException.OldOrderException(now, newOrder.getTimestamp());
        }

        if (newOrder.getTimestamp() > now) {
            throw new OrderValidationException.FutureOrderException(now, newOrder.getTimestamp());
        }

        int index = (int) (newOrder.getTimestamp() % statisticsPeriodInMillis);
        log.debug("Adding new order: {} to InterimStatistics@{} : {}", newOrder, index, interimStatistics.get(index));
        interimStatistics.get(index).add(newOrder);
    }

    public void deleteAllOrders() {
        for (int i = 0; i < statisticsPeriodInMillis + 1; i++) {
            interimStatistics.get(i).reset(0L);
        }
    }

    public Statistics getStatistics(long currentTimeMillis, int periodInMillis) {
        long start = currentTimeMillis - periodInMillis;

        log.info("Calculating statistics from {}({}) to {}({})",
                Instant.ofEpochMilli(start).toString(), start,
                Instant.ofEpochMilli(currentTimeMillis).toString(), currentTimeMillis);

        BigDecimal sum = BigDecimal.ZERO;
        long count = 0;
        BigDecimal currentMin = BigDecimal.valueOf(Double.MAX_VALUE);
        BigDecimal currentMax = BigDecimal.ZERO;

        for (int i = 0; i < statisticsPeriodInMillis + 1; i++) {
            InterimStatistics stat = interimStatistics.get(i);
            synchronized (stat) {
                long timestamp = stat.getTimestamp();
                if (start < timestamp && timestamp <= currentTimeMillis) {
                    sum = sum.add(stat.getSum());
                    count += stat.getCount();
                    currentMin = stat.getMin().min(currentMin);
                    currentMax = stat.getMax().max(currentMax);
                }
            }
        }

        if (count == 0) {
            currentMin = BigDecimal.ZERO.setScale(STATS_SCALE, java.math.RoundingMode.HALF_UP);
            currentMax = BigDecimal.ZERO.setScale(STATS_SCALE, java.math.RoundingMode.HALF_UP);
        } else {
            currentMin = currentMin.setScale(STATS_SCALE, java.math.RoundingMode.HALF_UP);
            currentMax = currentMax.setScale(STATS_SCALE, java.math.RoundingMode.HALF_UP);
        }

        sum = sum.setScale(STATS_SCALE, java.math.RoundingMode.HALF_UP);
        BigDecimal avg;
        if (count == 0) {
            avg = BigDecimal.ZERO.setScale(STATS_SCALE, java.math.RoundingMode.HALF_UP);
        } else {
            avg = sum.divide(BigDecimal.valueOf(count), STATS_SCALE, java.math.RoundingMode.HALF_UP);
        }
        return new Statistics(sum, avg, currentMax, currentMin, count);
    }
}
