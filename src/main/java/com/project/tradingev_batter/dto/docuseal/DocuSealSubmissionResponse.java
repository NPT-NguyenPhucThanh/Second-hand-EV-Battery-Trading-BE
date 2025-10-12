package com.project.tradingev_batter.dto.docuseal;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

import java.util.Date;
import java.util.List;

//DTO nhận response từ DocuSeal API Khi tạo submission thành công
@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class DocuSealSubmissionResponse {
    
    /**
     * ID của submission (slug)
     * VD: "tN7jL9"
     */
    private String slug;
    
    //ID submission dạng số
    private Long id;
    
    /**
     * Trạng thái submission
     * Giá trị: "pending", "completed", "declined", "expired"
     */
    private String status;
    
    //URL xem submission
    @JsonProperty("submission_url")
    private String submissionUrl;
    
    //Ngày tạo
    @JsonProperty("created_at")
    private Date createdAt;
    
    //Ngày cập nhật
    @JsonProperty("updated_at")
    private Date updatedAt;
    
    //Template ID
    @JsonProperty("template_id")
    private String templateId;
    
    //Danh sách submitters với thông tin chi tiết
    private List<SubmitterDetail> submitters;
    
    //Thông tin chi tiết của người ký
    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class SubmitterDetail {
        private Long id;
        
        @JsonProperty("submission_id")
        private Long submissionId;
        
        private String uuid;
        private String email;
        private String slug;
        
        //URL để người ký có thể sign
        private String url;
        
        //Trạng thái: "pending", "completed", "declined"
        private String status;
        
        //Thời gian gửi email
        @JsonProperty("sent_at")
        private Date sentAt;
        
        //Thời gian đã open email/document
        @JsonProperty("opened_at")
        private Date openedAt;
        
        //Thời gian hoàn thành ký
        @JsonProperty("completed_at")
        private Date completedAt;
        
        //Tên người ký
        private String name;
        
        //Role từ template
        private String role;
    }
    
    //Thông tin documents (PDF files) đã ký
    private List<DocumentInfo> documents;
    
    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class DocumentInfo {
        private String name;
        private String url;
    }
}
