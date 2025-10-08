package com.middy.assignment.model;

import java.math.BigDecimal;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;

import lombok.Getter;
import lombok.ToString;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Getter
@ToString
public class InterimStatistics {

    private static final Logger log = LoggerFactory.getLogger(InterimStatistics.class);
    private final AtomicLong timestamp = new AtomicLong(0);
    private final AtomicLong count = new AtomicLong(0);
    private final AtomicReference<BigDecimal> sum = new AtomicReference<>(BigDecimal.ZERO);
    private final AtomicReference<BigDecimal> max = new AtomicReference<>(BigDecimal.valueOf(Double.MIN_VALUE));
    private final AtomicReference<BigDecimal> min = new AtomicReference<>(BigDecimal.valueOf(Double.MAX_VALUE));
    private final AtomicBoolean valid = new AtomicBoolean(true);

    public void reset(long newTimestamp) {
        if (timestamp.get() == 0) {
            return;
        }

        synchronized (this) {
            log.debug("Resetting InterimStatistics at timestamp: {} to {}", this.timestamp.get(), newTimestamp);
            this.timestamp.set(newTimestamp);
            this.sum.set(BigDecimal.ZERO);
            this.max.set(BigDecimal.ZERO);
            this.min.set(BigDecimal.valueOf(Double.MAX_VALUE));
            this.count.set(0);
            this.timestamp.set(0);
        }
    }

    /**
     * Adds a new order to the interim statistics.
     * <p>
     * Note: This method is not fully thread-safe. If multiple threads call this method concurrently,
     * there may be race conditions when updating statistics, especially when the timestamp changes.
     * When the new order's timestamp differs from the current, statistics are reset.
     * Consider synchronizing or revisiting concurrency handling for production use.
     *
     * @param newOrder the order to add
     */
    public void add(Order newOrder) {
        // TODO: could result in concurrency issue, need to revisit
        if (timestamp.get() == 0) {
            this.timestamp.set(newOrder.getTimestamp());
        } else if (this.timestamp.get() != newOrder.getTimestamp()) {
            reset(newOrder.getTimestamp());
        }

        this.count.incrementAndGet();
        this.sum.accumulateAndGet(newOrder.getAmount(), BigDecimal::add);
        this.min.accumulateAndGet(newOrder.getAmount(), BigDecimal::min);
        this.max.accumulateAndGet(newOrder.getAmount(), BigDecimal::max);
    }
}
