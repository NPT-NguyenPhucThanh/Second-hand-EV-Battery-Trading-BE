package com.project.tradingev_batter.dto.docuseal;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

//DTO để tạo submission (hồ sơ ký) trên DocuSeal
//Tương ứng với API POST /submissions
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DocuSealSubmissionRequest {
    
    /**
     * ID của template trên DocuSeal (bắt buộc)
     * Lấy từ DocuSeal dashboard sau khi tạo template
     */
    private String template_id;
    
    //Danh sách người ký (submitters)
    private List<Submitter> submitters;
    
    /**
     * URL webhook để nhận callback khi submission hoàn tất
     * VD: https://yourdomain.com/api/docuseal/webhook
     */
    private String webhook_url;
    
    /**
     * Gửi email ngay lập tức cho người ký
     * true = gửi email, false = không gửi
     */
    private Boolean send_email;
    
    //Gửi SMS (nếu có phone number)
    private Boolean send_sms;
    
    //Thông tin người ký
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Submitter {
        //Role name từ template (VD: "Seller", "Buyer", "Manager")
        private String role;
        
        //Email người ký
        private String email;
        
        //Tên người ký
        private String name;
        
        //Số điện thoại (optional)
        private String phone;
        
        /**
         * Các trường dữ liệu được điền sẵn (pre-filled fields)
         * Phải là Array of objects với format: [{"name": "field_name", "default_value": "value"}]
         */
        private List<Field> fields;
    }

    //Field data structure theo yêu cầu của DocuSeal API
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Field {
        private String name;           // Tên field trong template
        private String default_value;  // Giá trị mặc định
    }
}
