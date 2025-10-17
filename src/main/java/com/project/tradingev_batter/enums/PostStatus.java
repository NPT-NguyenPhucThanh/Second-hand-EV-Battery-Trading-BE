package com.project.tradingev_batter.enums;

public enum PostStatus {
    CHO_DUYET("Chờ duyệt"),
    DA_DUYET("Đã duyệt"),
    BI_TU_CHOI("Bị từ chối"),
    DA_AN("Đã ẩn"),
    DA_XOA("Đã xóa");

    private final String description;

    PostStatus(String description) {
        this.description = description;
    }

    public String getDescription() {
        return description;
    }
}

