package com.payflux.api.web;

import com.payflux.api.dto.PageResponse;
import com.payflux.api.dto.RefundResponse;
import com.payflux.api.service.CurrentUserService;
import com.payflux.api.service.RefundService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/refunds")
public class RefundController {

    private final RefundService refundService;
    private final CurrentUserService currentUserService;

    public RefundController(RefundService refundService, CurrentUserService currentUserService) {
        this.refundService = refundService;
        this.currentUserService = currentUserService;
    }

    @GetMapping
    public PageResponse<RefundResponse> list(
            @RequestParam(defaultValue = "0") int page, @RequestParam(defaultValue = "20") int size) {
        var merchant = currentUserService.requireMerchant();
        return PageResponse.of(refundService.listForMerchant(merchant.getId(), page, size), r -> r);
    }
}
