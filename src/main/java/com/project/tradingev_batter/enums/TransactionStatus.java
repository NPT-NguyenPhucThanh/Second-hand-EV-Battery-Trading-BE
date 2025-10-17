package com.project.tradingev_batter.enums;

public enum TransactionStatus {
    PENDING("Chờ xử lý"),
    PROCESSING("Đang xử lý"),
    SUCCESS("Thành công"),
    FAILED("Thất bại"),
    CANCELLED("Đã hủy"),
    REFUNDED("Đã hoàn tiền"),
    ESCROW("Đang giữ tiền 7 ngày");

    private final String description;

    TransactionStatus(String description) {
        this.description = description;
    }

    public String getDescription() {
        return description;
    }
}
