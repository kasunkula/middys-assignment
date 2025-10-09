package com.middy.assignment.model;

import com.middy.assignment.model.Order; // Assuming Order class is available
import java.math.BigDecimal;
import java.util.concurrent.atomic.AtomicReference;
import lombok.Getter;
import lombok.ToString;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class InterimStatisticsLockFree {

    private static final Logger log = LoggerFactory.getLogger(InterimStatistics.class);
    private final AtomicReference<InterimStatisticsData> statistics =
            new AtomicReference<>(InterimStatisticsData.EMPTY);

    /**
     * Adds a new order to the interim statistics.
     * This method is thread-safe and non-blocking, using an atomic update loop.
     *
     * @param newOrder the order to add
     */
    public void add(Order newOrder) {
        InterimStatisticsData currentData;
        InterimStatisticsData newData;

        do {
            currentData = statistics.get();

            if (currentData.getTimestamp() == 0 || currentData.getTimestamp() != newOrder.getTimestamp()) {
                // Initializing or resetting the statistics
                if (currentData.getTimestamp() != 0) {
                    log.debug("Timestamp changed from {} to {}. Resetting statistics.",
                            currentData.getTimestamp(), newOrder.getTimestamp());
                }
                newData = new InterimStatisticsData(
                        newOrder.getTimestamp(),
                        1,
                        newOrder.getAmount(),
                        newOrder.getAmount(),
                        newOrder.getAmount()
                );
            } else {
                // Updating existing statistics
                newData = new InterimStatisticsData(
                        currentData.getTimestamp(),
                        currentData.getCount() + 1,
                        currentData.getSum().add(newOrder.getAmount()),
                        currentData.getMax().max(newOrder.getAmount()),
                        currentData.getMin().min(newOrder.getAmount())
                );
            }
        } while (!statistics.compareAndSet(currentData, newData));
    }

    public void reset() {
        statistics.set(InterimStatisticsData.EMPTY);
    }

    // A getter method for external access to the full statistics object
    public InterimStatisticsData getStatisticsData() {
        return statistics.get();
    }
}
