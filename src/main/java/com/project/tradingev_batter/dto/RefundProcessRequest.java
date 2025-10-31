package com.project.tradingev_batter.dto;

import lombok.Data;

@Data
public class RefundProcessRequest {
    private boolean approve; // true = chấp nhận, false = từ chối
    private String refundMethod; // "VNPAY", "BANK_TRANSFER", etc. (nếu approve = true)
    private String note; // Ghi chú của manager (nếu từ chối)
}

