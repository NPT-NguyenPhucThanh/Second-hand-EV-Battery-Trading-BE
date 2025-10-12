package com.project.tradingev_batter.dto.docuseal;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

import java.util.Date;
import java.util.List;
import java.util.Map;


//DTO nhận webhook callback từ DocuSeal
//Khi người dùng hoàn tất ký hoặc có sự kiện khác
//DocuSeal sẽ POST đến webhook_url với payload này
@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class DocuSealWebhookPayload {
    
    /**
     * Loại sự kiện
     * Giá trị có thể:
     * submission.created
     * submission.completed
     * submitter.completed
     * submitter.declined
     */
    @JsonProperty("event_type")
    private String eventType;
    
    //Timestamp của event
    private Long timestamp;
    
    //Dữ liệu submission
    private SubmissionData data;
    
    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class SubmissionData {
        //ID submission
        private Long id;
        
        //Slug submission
        private String slug;
        
        //Trạng thái: "pending", "completed", "declined"

        private String status;
        
        //Template ID
        @JsonProperty("template_id")
        private String templateId;
        
        //Thời gian tạo
        @JsonProperty("created_at")
        private Date createdAt;
        
        //Thời gian cập nhật
        @JsonProperty("updated_at")
        private Date updatedAt;
        
        //Thời gian hoàn tất
        @JsonProperty("completed_at")
        private Date completedAt;
        
        //Danh sách submitters
        private List<SubmitterInfo> submitters;
        
        //Danh sách documents (PDF đã ký)
        private List<DocumentInfo> documents;
        
        //Metadata tùy chỉnh (nếu có truyền lúc tạo submission)
        private Map<String, Object> metadata;
    }
    
    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class SubmitterInfo {
        private Long id;
        private String uuid;
        private String slug;
        private String email;
        private String name;
        private String role;
        private String status;
        
        @JsonProperty("completed_at")
        private Date completedAt;
        
        @JsonProperty("declined_at")
        private Date declinedAt;

        private List<FieldValue> values;
    }
    
    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class FieldValue {
        private String field;
        private Object value;
    }
    
    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class DocumentInfo {
        private String name;
        
        //URL để tải PDF đã ký
        private String url;
    }
}
