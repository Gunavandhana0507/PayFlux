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
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public OrderDtos.OrderDto create(@RequestHeader(value = "X-Idempotency-Key", required = false) String key, @Valid @RequestBody OrderDtos.CreateOrderRequest request) { return orderService.create(request, key); }
    @GetMapping
    public PageResponse<OrderDtos.OrderDto> list(@RequestParam(defaultValue = "0") int page, @RequestParam(defaultValue = "20") int size) { return orderService.list(page, Math.min(size, 100)); }
    @GetMapping("/{id}")
    public OrderDtos.OrderDetailDto detail(@PathVariable String id) { return orderService.detail(id); }
}
