package com.project.tradingev_batter.enums;

public enum ProductStatus {
    CHO_DUYET("Chờ duyệt sơ bộ"),
    CHO_KIEM_DUYET("Chờ kiểm định"),
    DA_DUYET("Đã duyệt - Chờ ký hợp đồng"),
    DANG_BAN("Đang bán"),
    DA_BAN("Đã bán"),
    BI_TU_CHOI("Bị từ chối"),
    KHONG_DAT_KIEM_DINH("Không đạt kiểm định"),
    HET_HAN("Hết hạn gói"),
    REMOVED_FROM_WAREHOUSE("Đã gỡ khỏi kho");

    private final String description;

    ProductStatus(String description) {
        this.description = description;
    }

    public String getDescription() {
        return description;
    }
}
