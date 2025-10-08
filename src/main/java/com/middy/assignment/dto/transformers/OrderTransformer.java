package com.middy.assignment.dto.transformers;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;

import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;

import com.middy.assignment.dto.OrderDto;
import com.middy.assignment.exception.OrderValidationException;
import com.middy.assignment.model.Order;

@Slf4j
@Component
public class OrderTransformer {

    private final Clock clock;

    public OrderTransformer(Clock clock) {
        this.clock = clock;
    }

    /**
     * Validates the provided OrderDto and transforms it into an Order object.
     *
     * @param orderDTO the OrderDto to validate and transform
     * @return a valid Order object
     * @throws OrderValidationException if the input is invalid or contains incorrect data
     * @throws OrderValidationException.OldOrderException if the order timestamp is older than allowed
     */
    public Order validateAndTransformToOrder(OrderDto orderDTO) {
        if (orderDTO == null || orderDTO.getAmount() == null || orderDTO.getTimestamp() == null) {
            throw new OrderValidationException("JSON is invalid", HttpStatus.BAD_REQUEST);
        }

        BigDecimal amount;
        long orderTime;
        try {
            amount = new BigDecimal(orderDTO.getAmount());
        } catch (NumberFormatException e) {
            throw new OrderValidationException(
                    String.format("Invalid amount as %s", orderDTO.getAmount()),
                    HttpStatus.UNPROCESSABLE_ENTITY
            );
        }

        // Expected timestamp format: ISO-8601 (e.g., "2023-06-01T12:34:56Z")
        try {
            orderTime = Instant.parse(orderDTO.getTimestamp()).toEpochMilli();
        } catch (java.time.format.DateTimeParseException e) {
            throw new OrderValidationException(
                    String.format("Invalid timestamp format: %s. Expected ISO-8601 format.", orderDTO.getTimestamp()),
                    HttpStatus.UNPROCESSABLE_ENTITY
            );
        }

        long now = clock.millis();
        if (orderTime > now) {
            throw new OrderValidationException.FutureOrderException(now, orderTime);
        }

        if (orderTime < (now - 60000)) {
            throw new OrderValidationException.OldOrderException(now, orderDTO.getTimestamp());
        }

        return new Order(amount, orderTime);
    }
}
