package com.project.tradingev_batter.enums;

public enum OrderStatus {
    CHO_THANH_TOAN("Chờ thanh toán"),
    CHO_DAT_COC("Chờ đặt cọc 10%"),
    CHO_XAC_NHAN("Chờ xác nhận"),
    DA_DAT_COC("Đã đặt cọc 10%"),
    CHO_DUYET("Chờ manager duyệt"),
    DA_DUYET("Đã duyệt - Chờ thanh toán cuối"),
    DA_THANH_TOAN("Đã thanh toán đầy đủ"),
    DANG_VAN_CHUYEN("Đang vận chuyển (pin)"),
    DA_GIAO("Đã giao hàng - Chờ xác nhận"),
    DA_HOAN_TAT("Đã hoàn tất"),
    BI_TU_CHOI("Bị từ chối"),
    TRANH_CHAP("Tranh chấp"),
    DISPUTE_RESOLVED("Đã giải quyết tranh chấp"),
    RESOLVED_WITH_REFUND("Đã giải quyết - Có hoàn tiền"),
    DA_HUY("Đã hủy"),
    THAT_BAI("Thanh toán thất bại");

    private final String description;

    OrderStatus(String description) {
        this.description = description;
    }

    public String getDescription() {
        return description;
    }
}
