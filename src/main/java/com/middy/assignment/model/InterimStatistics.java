package com.middy.assignment.model;

import java.math.BigDecimal;

import lombok.Getter;
import lombok.ToString;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Getter
@ToString
public class InterimStatistics {

    private static final Logger log = LoggerFactory.getLogger(InterimStatistics.class);
    private long timestamp = 0;
    private long count = 0;
    private BigDecimal sum = BigDecimal.ZERO;
    private BigDecimal max = BigDecimal.valueOf(Double.MIN_VALUE);
    private BigDecimal min = BigDecimal.valueOf(Double.MAX_VALUE);
    private boolean valid = true;

    public synchronized void reset() {
        this.timestamp = 0;
        this.sum = BigDecimal.ZERO;
        this.max = BigDecimal.ZERO;
        this.min = BigDecimal.valueOf(Double.MAX_VALUE);
        this.count = 0;
    }

    private void initFrom(Order newOrder) {
        this.count = 1;
        this.sum = newOrder.getAmount();
        this.min = newOrder.getAmount();
        this.max = newOrder.getAmount();
        this.timestamp = newOrder.getTimestamp();
    }

    /**
     * Adds a new order to the interim statistics.
     * <p>
     * This method is thread-safe and ensures atomic updates of all statistics fields
     * ('count', 'sum', 'min', and 'max') using synchronization.
     * When the new order's timestamp differs from the current, statistics are reset.
     *
     * @param newOrder the order to add
     */
    public synchronized void add(Order newOrder) {
        if (timestamp == 0) {
            // reusing a new/reset InterimStatistics instance
            initFrom(newOrder);
            return;
        } else if (this.timestamp != newOrder.getTimestamp()) { 
            // reusing an existing InterimStatistics with a stale timestamp
            log.debug("Timestamp changed from {} to {}. Resetting statistics.",
                    this.timestamp, newOrder.getTimestamp());
            initFrom(newOrder);
            return;
        }

        // Update all statistics
        this.count++;
        this.sum = this.sum.add(newOrder.getAmount());
        this.min = this.min.min(newOrder.getAmount());
        this.max = this.max.max(newOrder.getAmount());
    }
}
