package com.payflux.order;

import com.payflux.common.PageResponse;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/orders")
public class OrderController {
    private final OrderService orderService;
    public OrderController(OrderService orderService) { this.orderService = orderService; }
    @PostMapping @ResponseStatus(HttpStatus.CREATED) public OrderDtos.OrderDto create(@RequestHeader(value = "X-Idempotency-Key", required = false) String key, @Valid @RequestBody OrderDtos.CreateOrderRequ
