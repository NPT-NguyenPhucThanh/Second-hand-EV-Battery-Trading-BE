package com.project.tradingev_batter.enums;

public enum TransactionType {
    DEPOSIT("Đặt cọc 10%"),
    FINAL_PAYMENT("Thanh toán phần còn lại"),
    BATTERY_PAYMENT("Thanh toán mua pin"),
    PACKAGE_PURCHASE("Mua gói dịch vụ"),
    REFUND("Hoàn tiền");

    private final String description;

    TransactionType(String description) {
        this.description = description;
    }

    public String getDescription() {
        return description;
    }
}
