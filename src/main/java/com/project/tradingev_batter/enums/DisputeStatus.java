package com.project.tradingev_batter.enums;

public enum DisputeStatus {
    OPEN("Đang mở"),
    IN_PROGRESS("Đang xử lý"),
    RESOLVED("Đã giải quyết"),
    CLOSED("Đã đóng"),
    CANCELLED("Đã hủy");

    private final String description;

    DisputeStatus(String description) {
        this.description = description;
    }

    public String getDescription() {
        return description;
    }
}
