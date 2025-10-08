package com.middy.assignment;

import java.time.Instant;
import java.time.ZoneId;

import org.springframework.http.MediaType;
import org.springframework.test.web.reactive.server.WebTestClient;

import com.middy.assignment.dto.OrderDto;

public class Utils {

    public static OrderDto getOrderWithDeltaWithMIllis(double amount, long milliSecondsFromNow) {
        String timestamp = Instant.ofEpochMilli(System.currentTimeMillis() - milliSecondsFromNow).atZone(ZoneId.of("UTC")).toInstant().toString();
        return new OrderDto(String.valueOf(amount), timestamp);
    }

    public static OrderDto getOrder(double amount, long timestampInMillis) {
        String timestamp = Instant.ofEpochMilli(timestampInMillis).atZone(ZoneId.of("UTC")).toInstant().toString();
        return new OrderDto(String.valueOf(amount), timestamp);
    }

    public static void submitOrder(double amount, long milliSecondsFromNow, WebTestClient webTestClient) {
        submitOrder(getOrderWithDeltaWithMIllis(amount, milliSecondsFromNow), webTestClient);
    }

    public static void submitOrder(OrderDto order, WebTestClient webTestClient) {
        webTestClient.post()
                .uri("/v1/orders")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(order)
                .exchange()
                .expectStatus().isCreated();
    }
}
