package com.project.tradingev_batter.Service;

import com.project.tradingev_batter.Entity.Contracts;
import com.project.tradingev_batter.Entity.Product;
import com.project.tradingev_batter.Entity.User;
import com.project.tradingev_batter.Entity.Orders;
import com.project.tradingev_batter.dto.docuseal.DocuSealSubmissionResponse;
import com.project.tradingev_batter.dto.docuseal.DocuSealWebhookPayload;

//Service interface cho DocuSeal integration
public interface DocuSealService {
    
    /**
     * Tạo hợp đồng đăng bán sản phẩm (Product Listing Contract)
     * Giữa Seller và Hệ thống/Manager
     * Được gọi sau khi xe đạt kiểm định
     * product Sản phẩm cần tạo hợp đồng
     * seller Người bán (owner của product)
     * manager Manager tạo hợp đồng
     * @return Contract entity đã được tạo và lưu vào DB
     */
    Contracts createProductListingContract(Product product, User seller, User manager);
    
    /**
     * Tạo hợp đồng mua bán (Sale Transaction Contract)
     * Giữa Buyer và Seller
     * Được gọi khi buyer đặt cọc 10%
     * order Đơn hàng
     * buyer Người mua
     * seller Người bán
     * transactionLocation Địa điểm giao dịch
     * @return Contract entity đã được tạo và lưu vào DB
     */
    Contracts createSaleTransactionContract(Orders order, User buyer, User seller, String transactionLocation);
    
    /**
     * Lấy thông tin submission từ DocuSeal
     * submissionId ID của submission (slug hoặc id)
     * @return Thông tin chi tiết submission
     */
    DocuSealSubmissionResponse getSubmission(String submissionId);
    
    /**
     * Xử lý webhook callback từ DocuSeal
     * Được gọi khi có sự kiện: submission.completed, submitter.completed, etc.
     * @param payload Webhook payload từ DocuSeal
     */
    void handleWebhook(DocuSealWebhookPayload payload);
    
    /**
     * Tải PDF đã ký từ DocuSeal
     * documentUrl URL của document từ DocuSeal
     * @return Byte array của PDF file
     */
    byte[] downloadSignedDocument(String documentUrl);
    
    /**
     * Kiểm tra xem submission đã hoàn tất chưa
     * submissionId ID của submission
     * @return true nếu tất cả người ký đã hoàn tất, false nếu còn pending
     */
    boolean isSubmissionCompleted(String submissionId);
    
    /**
     * Gửi lại email nhắc nhở người ký
     * submissionId ID của submission
     * submitterEmail Email người ký cần nhắc nhở
     */
    void resendSigningEmail(String submissionId, String submitterEmail);
}
