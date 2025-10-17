package com.project.tradingev_batter.enums;

public enum RefundStatus {
    PENDING("Đang chờ xử lý"),
    PROCESSING("Đang xử lý"),
    COMPLETED("Đã hoàn thành"),
    REJECTED("Bị từ chối"),
    FAILED("Thất bại");

    private final String description;

    RefundStatus(String description) {
        this.description = description;
    }

    public String getDescription() {
        return description;
    }
}

