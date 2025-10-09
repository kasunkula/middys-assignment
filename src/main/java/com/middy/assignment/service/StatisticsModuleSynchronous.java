package com.middy.assignment.service;

import com.middy.assignment.exception.OrderValidationException;
import com.middy.assignment.model.*;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;

@Slf4j
@Component
@ConditionalOnProperty(name = "concurrency-model", havingValue = "synchronous")
public class StatisticsModuleSynchronous implements StatisticsModule {

    private final int statisticsPeriodInMillis;

    private static final int STATS_SCALE = 2;

    private final Clock clock;

    private final InterimStatistics[] interimStatistics;

    public StatisticsModuleSynchronous(Clock clock, @Value("${stats-period-in-milliseconds}") int statisticsPeriodInMillis) {
        log.info("Using Synchronous Statistics Module");
        this.clock = clock;
        this.statisticsPeriodInMillis = statisticsPeriodInMillis;
        interimStatistics = new InterimStatistics[statisticsPeriodInMillis];
        for (int i = 0; i < statisticsPeriodInMillis; i++) {
            interimStatistics[i] = new InterimStatistics();
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
        log.debug("Adding new order: {} to InterimStatistics@{} : {}", newOrder, index, interimStatistics[index]);
        interimStatistics[index].add(newOrder);
    }

    public void deleteAllOrders() {
        for (int i = 0; i < statisticsPeriodInMillis; i++) {
            interimStatistics[i].reset();
        }
    }

    public Statistics getStatistics(long currentTimeMillis, int periodInMillis) {
        long start = currentTimeMillis - periodInMillis;

        log.debug("Calculating statistics from {}({}) to {}({})",
                Instant.ofEpochMilli(start).toString(), start,
                Instant.ofEpochMilli(currentTimeMillis).toString(), currentTimeMillis);

        BigDecimal sum = BigDecimal.ZERO;
        long count = 0;
        BigDecimal currentMin = BigDecimal.valueOf(Double.MAX_VALUE);
        BigDecimal currentMax = BigDecimal.ZERO;

        for (int i = 0; i < statisticsPeriodInMillis; i++) {
            InterimStatistics stat = interimStatistics[i];
            synchronized (stat) {
                if (start < stat.getTimestamp() && stat.getTimestamp() <= currentTimeMillis) {
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
