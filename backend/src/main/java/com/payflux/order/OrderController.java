package com.payflux.order;

import com.payflux.common.PageResponse;

import jakarta.validation.Valid;

import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;

@RestController
@RequestMapping("/api/orders")
public class OrderController {
    private final OrderService orderService;

    public OrderController(OrderService orderService) {
        this.orderService = orderService;
    }

    @PostMapping
    public ResponseEntity<OrderDtos.OrderDto> create(
            @RequestHeader(value = "X-Idempotency-Key", required = false) String key,
            @Valid @RequestBody OrderDtos.CreateOrderRequest request) {
        OrderService.CreateResult result = orderService.create(request, key);
        return ResponseEntity.status(result.created() ? 201 : 200).body(result.dto());
    }

    @GetMapping
    public PageResponse<OrderDtos.OrderDto> list(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) OrderStatus status,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
                    LocalDate from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
                    LocalDate to) {
        return orderService.list(page, Math.min(size, 100), status, from, to);
    }

    @GetMapping("/{id}")
    public OrderDtos.OrderDetailDto detail(@PathVariable String id) {
        return orderService.detail(id);
    }
}
