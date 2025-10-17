package com.project.tradingev_batter.enums;

public enum PaymentMethod {
    VNPAY("VNPay"),
    CASH("Tiền mặt"),
    BANK_TRANSFER("Chuyển khoản ngân hàng");

    private final String description;

    PaymentMethod(String description) {
        this.description = description;
    }

    public String getDescription() {
        return description;
    }
}
