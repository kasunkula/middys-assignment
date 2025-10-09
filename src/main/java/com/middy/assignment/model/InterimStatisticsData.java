package com.middy.assignment.model;

import java.math.BigDecimal;
import lombok.Getter;
import lombok.ToString;

/**
 * Immutable class to hold the statistics.
 */
@Getter
@ToString
public final class InterimStatisticsData {
    private final long timestamp;
    private final long count;
    private final BigDecimal sum;
    private final BigDecimal max;
    private final BigDecimal min;

    // A static constant representing an empty/reset state.
    public static final InterimStatisticsData EMPTY = new InterimStatisticsData(
            0, 0, BigDecimal.ZERO, BigDecimal.valueOf(Double.MIN_VALUE),
            BigDecimal.valueOf(Double.MAX_VALUE)
    );

    public InterimStatisticsData(long timestamp, long count, BigDecimal sum, BigDecimal max, BigDecimal min) {
        this.timestamp = timestamp;
        this.count = count;
        this.sum = sum;
        this.max = max;
        this.min = min;
    }
}
