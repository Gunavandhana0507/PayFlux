package com.payflux.refund;

import com.payflux.common.PageResponse;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/refunds")
public class RefundController {
    private final RefundService refundService;
    public RefundController(RefundService refundService) { this.refundService = refundService; }
    @GetMapping public PageResponse<RefundDtos.RefundDto> list(@RequestParam(defaultValue = "0") int page, @RequestParam(defaultValue = "20") int size) { return refundService.list(page, Math.min(size, 100)); }
    @GetMapping("/{id}") public RefundDtos.RefundDto detail(@PathVariable String id) { return refundService.detail(id); }
}
