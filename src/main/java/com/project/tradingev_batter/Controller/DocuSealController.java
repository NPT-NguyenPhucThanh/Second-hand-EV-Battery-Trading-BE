package com.project.tradingev_batter.Controller;

import com.project.tradingev_batter.Service.DocuSealService;
import com.project.tradingev_batter.dto.docuseal.DocuSealWebhookPayload;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

//Controller xử lý DocuSeal webhook và các API liên quan

@RestController
@RequestMapping("/api/docuseal")
@Slf4j
public class DocuSealController {

    private final DocuSealService docuSealService;

    public DocuSealController(DocuSealService docuSealService) {
        this.docuSealService = docuSealService;
    }

    /**
     * Nhận webhook callback từ DocuSeal
     * DocuSeal sẽ POST đến endpoint này khi có sự kiện:
     * - submission.created
     * - submission.completed
     * - submitter.completed
     * - submitter.declined
     *
     * @param payload Webhook payload từ DocuSeal
     * @return 200 OK để DocuSeal biết đã nhận được
     */
    @PostMapping("/webhook")
    public ResponseEntity<Map<String, String>> handleWebhook(@RequestBody DocuSealWebhookPayload payload) {
        log.info("Received DocuSeal webhook: event_type={}, submission_id={}",
                payload.getEventType(),
                payload.getData() != null ? payload.getData().getSlug() : "N/A");

        try {
            // Xử lý webhook
            docuSealService.handleWebhook(payload);

            // Trả về 200 OK
            Map<String, String> response = new HashMap<>();
            response.put("status", "success");
            response.put("message", "Webhook received and processed");

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            log.error("Error processing DocuSeal webhook", e);

            // Vẫn trả về 200 OK để DocuSeal không retry liên tục
            Map<String, String> response = new HashMap<>();
            response.put("status", "error");
            response.put("message", e.getMessage());

            return ResponseEntity.ok(response);
        }
    }

    /**
     * API kiểm tra trạng thái submission
     * Dùng để FE check xem submission đã hoàn tất chưa
     */
    @GetMapping("/submissions/{submissionId}/status")
    public ResponseEntity<Map<String, Object>> checkSubmissionStatus(@PathVariable String submissionId) {
        try {
            boolean isCompleted = docuSealService.isSubmissionCompleted(submissionId);

            Map<String, Object> response = new HashMap<>();
            response.put("submission_id", submissionId);
            response.put("is_completed", isCompleted);
            response.put("status", isCompleted ? "completed" : "pending");

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            log.error("Error checking submission status", e);

            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("status", "error");
            errorResponse.put("message", e.getMessage());

            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse);
        }
    }

    /**
     * API gửi lại email nhắc nhở ký
     * Dùng khi người dùng chưa ký và muốn nhận lại email
     */
    @PostMapping("/submissions/{submissionId}/resend-email")
    public ResponseEntity<Map<String, String>> resendSigningEmail(
            @PathVariable String submissionId,
            @RequestParam String email) {

        try {
            docuSealService.resendSigningEmail(submissionId, email);

            Map<String, String> response = new HashMap<>();
            response.put("status", "success");
            response.put("message", "Email đã được gửi lại thành công");

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            log.error("Error resending email", e);

            Map<String, String> errorResponse = new HashMap<>();
            errorResponse.put("status", "error");
            errorResponse.put("message", e.getMessage());

            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse);
        }
    }

    /**
     * API test webhook (for development)
     * Chỉ dùng để test, xóa đi khi production
     */
    @PostMapping("/webhook/test")
    public ResponseEntity<Map<String, String>> testWebhook(@RequestBody String payload) {
        log.info("Test webhook received: {}", payload);

        Map<String, String> response = new HashMap<>();
        response.put("status", "success");
        response.put("message", "Test webhook received");

        return ResponseEntity.ok(response);
    }

    /**
     * Endpoint test webhook
     * Dùng để test xem webhook có hoạt động không
     */
    @GetMapping("/webhook/test")
    public ResponseEntity<Map<String, String>> testWebhook() {
        Map<String, String> response = new HashMap<>();
        response.put("status", "success");
        response.put("message", "DocuSeal webhook endpoint is working");
        response.put("timestamp", String.valueOf(System.currentTimeMillis()));

        return ResponseEntity.ok(response);
    }
}