package com.project.tradingev_batter.dto;

import lombok.Data;

public class ApprovalRequest {
    private boolean approved;
    private String note;

    public boolean isApproved() {
        return approved;
    }

    public void setApproved(boolean approved) {
        this.approved = approved;
    }

    public String getNote() {
        return note;
    }

    public void setNote(String note) {
        this.note = note;
    }
}
