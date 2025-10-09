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
    private volatile long timestamp = 0;
    private long count = 0;
    private BigDecimal sum = BigDecimal.ZERO;
    private BigDecimal max = BigDecimal.valueOf(Double.MIN_VALUE);
    private BigDecimal min = BigDecimal.valueOf(Double.MAX_VALUE);

    public synchronized void reset(long newTimestamp) {
        if (timestamp == 0) {
            return;
        }

        log.debug("Resetting InterimStatistics at timestamp: {} to {}", this.timestamp, newTimestamp);
        this.timestamp = newTimestamp;
        this.sum = BigDecimal.ZERO;
        this.max = BigDecimal.ZERO;
        this.min = BigDecimal.valueOf(Double.MAX_VALUE);
        this.count = 0;
    }

    /**
     * Adds a new order to the interim statistics.
     * This method is now fully thread-safe using synchronized blocks.
     *
     * @param newOrder the order to add
     */
    public synchronized void add(Order newOrder) {
        // Initialize timestamp if this is the first order
        if (this.timestamp == 0) {
            this.timestamp = newOrder.getTimestamp();
        } 
        // Reset statistics if order is for a different timestamp
        else if (this.timestamp != newOrder.getTimestamp()) {
            this.timestamp = newOrder.getTimestamp();
            this.sum = BigDecimal.ZERO;
            this.max = BigDecimal.valueOf(Double.MIN_VALUE);
            this.min = BigDecimal.valueOf(Double.MAX_VALUE);
            this.count = 0;
        }

        // Update statistics
        this.count++;
        this.sum = this.sum.add(newOrder.getAmount());
        this.min = this.min.min(newOrder.getAmount());
        this.max = this.max.max(newOrder.getAmount());
    }
}
