package com.payflux.dashboard;

import com.payflux.order.OrderDtos;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

public final class DashboardDtos {
    private DashboardDtos() {}
    public record Stats(long totalPayments, long successful, long failed, BigDecimal revenue, BigDecimal refunded) {}
    public record Daily(LocalDate date, long captured, long failed, BigDecimal revenue) {}
    public record Summary(Stats today, Stats last7Days, List<Daily> daily, List<OrderDtos.PaymentSummaryDto> recentAlerts) {}
}
