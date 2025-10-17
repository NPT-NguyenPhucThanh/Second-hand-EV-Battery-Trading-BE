package com.project.tradingev_batter.Controller;

import com.project.tradingev_batter.Entity.Orders;
import com.project.tradingev_batter.Entity.Transaction;
import com.project.tradingev_batter.Entity.User;
import com.project.tradingev_batter.Repository.OrderRepository;
import com.project.tradingev_batter.Repository.TransactionRepository;
import com.project.tradingev_batter.Service.VNPayService;
import com.project.tradingev_batter.Service.SellerService;
import com.project.tradingev_batter.enums.OrderStatus;
import com.project.tradingev_batter.enums.TransactionStatus;
import com.project.tradingev_batter.enums.TransactionType;
import com.project.tradingev_batter.security.CustomUserDetails;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.text.SimpleDateFormat;
import java.util.*;

@RestController
@RequestMapping("/api/payment")
@Slf4j
public class PaymentController {

    private final VNPayService vnPayService;
    private final TransactionRepository transactionRepository;
    private final OrderRepository orderRepository;
    private final SellerService sellerService;

    public PaymentController(VNPayService vnPayService,
                           TransactionRepository transactionRepository,
                           OrderRepository orderRepository,
                           SellerService sellerService) {
        this.vnPayService = vnPayService;
        this.transactionRepository = transactionRepository;
        this.orderRepository = orderRepository;
        this.sellerService = sellerService;
    }

    //TẠO PAYMENT URL - User click "Thanh toán" → API này tạo VNPay URL
    @PostMapping("/create-payment-url")
    public ResponseEntity<Map<String, Object>> createPaymentUrl(
            @RequestParam Long orderId,
            @RequestParam String transactionType,
            HttpServletRequest request) {
        
        try {
            User user = getCurrentUser();
            Orders order = orderRepository.findById(orderId)
                    .orElseThrow(() -> new RuntimeException("Order not found"));
            
            // Kiểm tra quyền sở hữu
            if (order.getUsers().getUserid() != user.getUserid()) {
                return ResponseEntity.badRequest().body(Map.of(
                    "status", "error",
                    "message", "Bạn không có quyền thanh toán đơn hàng này"
                ));
            }
            
            // Tính số tiền cần thanh toán
            double amount = calculatePaymentAmount(order, TransactionType.valueOf(transactionType));
            
            // Tạo transaction code unique
            String transactionCode = vnPayService.generateTransactionCode(orderId, transactionType);
            
            // Tạo transaction record (status = PENDING)
            Transaction transaction = new Transaction();
            transaction.setTransactionCode(transactionCode);
            transaction.setAmount(amount);
            transaction.setTransactionType(TransactionType.valueOf(transactionType));
            transaction.setStatus(TransactionStatus.PENDING);
            transaction.setMethod("VNPAY");
            transaction.setCreatedat(new Date());
            transaction.setOrders(order);
            transaction.setCreatedBy(user);
            transaction.setDescription(getTransactionDescription(order, TransactionType.valueOf(transactionType)));
            transactionRepository.save(transaction);
            
            // Get IP address
            String ipAddress = getIpAddress(request);
            
            // Tạo payment URL
            String paymentUrl;
            
            if (vnPayService.isMockMode()) {
                // MOCK MODE - Return mock URL cho testing
                paymentUrl = "http://localhost:8080/api/payment/mock-payment?transactionCode=" + transactionCode;
                
                Map<String, Object> response = new HashMap<>();
                response.put("status", "success");
                response.put("message", "MOCK MODE - Use this URL to simulate payment");
                response.put("paymentUrl", paymentUrl);
                response.put("transactionCode", transactionCode);
                response.put("amount", amount);
                response.put("note", "Mock mode enabled. Click URL to auto-success payment.");
                
                return ResponseEntity.ok(response);
            } else {
                // REAL MODE - Tạo VNPay URL thật
                String orderInfo = "Thanh toan don hang #" + orderId;
                paymentUrl = vnPayService.createPaymentUrl(amount, orderInfo, transactionCode, ipAddress);
                
                Map<String, Object> response = new HashMap<>();
                response.put("status", "success");
                response.put("message", "Redirect buyer to VNPay");
                response.put("paymentUrl", paymentUrl);
                response.put("transactionCode", transactionCode);
                response.put("amount", amount);
                response.put("expiryMinutes", 60);
                
                return ResponseEntity.ok(response);
            }
            
        } catch (Exception e) {
            log.error("Error creating payment URL", e);
            return ResponseEntity.status(500).body(Map.of(
                "status", "error",
                "message", "Không thể tạo link thanh toán: " + e.getMessage()
            ));
        }
    }

    //VNPAY RETURN URL - VNPay redirect về đây sau khi buyer thanh toán
    @GetMapping("/vnpay-return")
    public ResponseEntity<Map<String, Object>> vnpayReturn(@RequestParam Map<String, String> params) {
        try {
            log.info("VNPay return callback received: {}", params);
            
            // Verify signature
            if (!vnPayService.verifyCallback(params)) {
                log.warn("Invalid signature from VNPay");
                return ResponseEntity.badRequest().body(Map.of(
                    "status", "error",
                    "message", "Chữ ký không hợp lệ - Có thể bị giả mạo!"
                ));
            }
            
            // Parse response
            Map<String, String> vnpayData = vnPayService.parseVNPayResponse(params);
            String transactionCode = vnpayData.get("transactionCode");
            String responseCode = vnpayData.get("responseCode");
            
            // Find transaction
            Transaction transaction = transactionRepository.findByTransactionCode(transactionCode)
                    .orElseThrow(() -> new RuntimeException("Transaction not found"));
            
            // Update transaction
            updateTransactionFromVNPay(transaction, vnpayData);
            
            // Update order status
            Orders order = transaction.getOrders();
            updateOrderStatusAfterPayment(order, transaction);
            
            // Build response
            Map<String, Object> response = new HashMap<>();
            response.put("status", vnPayService.isPaymentSuccess(responseCode) ? "success" : "failed");
            response.put("message", vnPayService.getResponseMessage(responseCode));
            response.put("transactionCode", transactionCode);
            response.put("amount", Double.parseDouble(vnpayData.get("amount")) / 100);
            response.put("bankCode", vnpayData.get("bankCode"));
            response.put("cardType", vnpayData.get("cardType"));
            response.put("paymentDate", vnpayData.get("paymentDate"));
            response.put("orderId", order.getOrderid());
            response.put("orderStatus", order.getStatus());
            
            // NOTE CHO FE: Hiển thị trang kết quả thanh toán
            // FE nên có route: /payment/result?transactionCode=xxx
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            log.error("Error processing VNPay return", e);
            return ResponseEntity.status(500).body(Map.of(
                "status", "error",
                "message", "Lỗi xử lý callback: " + e.getMessage()
            ));
        }
    }

    //VNPAY IPN (Instant Payment Notification) - Webhook từ VNPay
    //VNPay sẽ gọi API này để thông báo kết quả thanh toán
    //IMPORTANT: Cần trả về đúng định dạng để VNPay ghi nhận đã nhận IPN
    //Nếu không, VNPay sẽ tiếp tục gửi lại nhiều lần
    @PostMapping("/vnpay-ipn")
    public ResponseEntity<Map<String, Object>> vnpayIPN(@RequestParam Map<String, String> params) {
        try {
            log.info("VNPay IPN received: {}", params);
            
            // Verify signature
            if (!vnPayService.verifyCallback(params)) {
                log.error("Invalid signature from VNPay IPN");
                return ResponseEntity.ok(Map.of(
                    "RspCode", "97",
                    "Message", "Invalid signature"
                ));
            }
            
            // Parse response
            Map<String, String> vnpayData = vnPayService.parseVNPayResponse(params);
            String transactionCode = vnpayData.get("transactionCode");
            String responseCode = vnpayData.get("responseCode");
            
            // Find transaction
            Optional<Transaction> transactionOpt = transactionRepository.findByTransactionCode(transactionCode);
            
            if (transactionOpt.isEmpty()) {
                log.error("Transaction not found: {}", transactionCode);
                return ResponseEntity.ok(Map.of(
                    "RspCode", "01",
                    "Message", "Order not found"
                ));
            }
            
            Transaction transaction = transactionOpt.get();
            
            // Check if already processed (tránh duplicate IPN)
            if (transaction.getStatus() == TransactionStatus.SUCCESS || 
                transaction.getStatus() == TransactionStatus.FAILED) {
                log.warn("Transaction already processed: {}", transactionCode);
                return ResponseEntity.ok(Map.of(
                    "RspCode", "02",
                    "Message", "Order already confirmed"
                ));
            }
            
            // Update transaction
            updateTransactionFromVNPay(transaction, vnpayData);
            
            // Update order status
            Orders order = transaction.getOrders();
            updateOrderStatusAfterPayment(order, transaction);
            
            // Return success to VNPay
            log.info("IPN processed successfully for transaction: {}", transactionCode);
            return ResponseEntity.ok(Map.of(
                "RspCode", "00",
                "Message", "Confirm Success"
            ));
            
        } catch (Exception e) {
            log.error("Error processing VNPay IPN", e);
            return ResponseEntity.ok(Map.of(
                "RspCode", "99",
                "Message", "Unknown error"
            ));
        }
    }

    //MOCK PAYMENT - Endpoint giả lập thanh toán thành công (cho testing)
    @GetMapping("/mock-payment")
    public ResponseEntity<Map<String, Object>> mockPayment(@RequestParam String transactionCode) {
        try {
            Transaction transaction = transactionRepository.findByTransactionCode(transactionCode)
                    .orElseThrow(() -> new RuntimeException("Transaction not found"));
            
            // Mock VNPay response
            Map<String, String> mockResponse = vnPayService.mockPaymentSuccess(transactionCode, transaction.getAmount());
            
            // Update transaction
            updateTransactionFromVNPay(transaction, mockResponse);
            
            // Update order
            Orders order = transaction.getOrders();
            updateOrderStatusAfterPayment(order, transaction);
            
            Map<String, Object> response = new HashMap<>();
            response.put("status", "success");
            response.put("message", "MOCK - Thanh toán thành công");
            response.put("transactionCode", transactionCode);
            response.put("amount", transaction.getAmount());
            response.put("orderId", order.getOrderid());
            response.put("orderStatus", order.getStatus());
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            return ResponseEntity.status(500).body(Map.of(
                "status", "error",
                "message", e.getMessage()
            ));
        }
    }

    //CHECK TRANSACTION STATUS - Kiểm tra trạng thái giao dịch
    @GetMapping("/transaction-status/{transactionCode}")
    public ResponseEntity<Map<String, Object>> checkTransactionStatus(@PathVariable String transactionCode) {
        try {
            Transaction transaction = transactionRepository.findByTransactionCode(transactionCode)
                    .orElseThrow(() -> new RuntimeException("Transaction not found"));
            
            Map<String, Object> response = new HashMap<>();
            response.put("status", "success");
            response.put("transaction", Map.of(
                "transactionCode", transaction.getTransactionCode(),
                "amount", transaction.getAmount(),
                "status", transaction.getStatus(),
                "transactionType", transaction.getTransactionType(),
                "method", transaction.getMethod(),
                "vnpayTransactionNo", transaction.getVnpayTransactionNo() != null ? transaction.getVnpayTransactionNo() : "N/A",
                "bankCode", transaction.getBankCode() != null ? transaction.getBankCode() : "N/A",
                "paymentDate", transaction.getPaymentDate() != null ? transaction.getPaymentDate() : null,
                "responseMessage", transaction.getVnpayResponseCode() != null ? 
                    vnPayService.getResponseMessage(transaction.getVnpayResponseCode()) : "Chờ thanh toán"
            ));
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            return ResponseEntity.status(404).body(Map.of(
                "status", "error",
                "message", "Transaction not found"
            ));
        }
    }

    // ============ HELPER METHODS =====================================================================================

    private double calculatePaymentAmount(Orders order, TransactionType transactionType) {
        switch (transactionType) {
            case DEPOSIT:
                return order.getTotalfinal() * 0.10; // 10% đặt cọc
            case FINAL_PAYMENT:
                return order.getTotalfinal() * 0.90; // 90% còn lại
            case BATTERY_PAYMENT:
            case PACKAGE_PURCHASE:
                return order.getTotalfinal(); // 100%
            default:
                throw new RuntimeException("Invalid transaction type");
        }
    }

    private String getTransactionDescription(Orders order, TransactionType type) {
        switch (type) {
            case DEPOSIT:
                return "Đặt cọc 10% cho đơn hàng #" + order.getOrderid();
            case FINAL_PAYMENT:
                return "Thanh toán phần còn lại cho đơn hàng #" + order.getOrderid();
            case BATTERY_PAYMENT:
                return "Thanh toán mua pin - Đơn hàng #" + order.getOrderid();
            case PACKAGE_PURCHASE:
                return "Mua gói dịch vụ - Đơn hàng #" + order.getOrderid();
            default:
                return "Thanh toán đơn hàng #" + order.getOrderid();
        }
    }

    private void updateTransactionFromVNPay(Transaction transaction, Map<String, String> vnpayData) {
        String responseCode = vnpayData.get("responseCode");
        
        transaction.setVnpayTransactionNo(vnpayData.get("vnpayTransactionNo"));
        transaction.setBankCode(vnpayData.get("bankCode"));
        transaction.setCardType(vnpayData.get("cardType"));
        transaction.setVnpayResponseCode(responseCode);
        
        if (vnPayService.isPaymentSuccess(responseCode)) {
            transaction.setStatus(TransactionStatus.SUCCESS);
            
            // Parse payment date
            try {
                SimpleDateFormat formatter = new SimpleDateFormat("yyyyMMddHHmmss");
                transaction.setPaymentDate(formatter.parse(vnpayData.get("paymentDate")));
                
                // Tính escrow release date (7 ngày sau)
                Calendar calendar = Calendar.getInstance();
                calendar.setTime(transaction.getPaymentDate());
                calendar.add(Calendar.DAY_OF_MONTH, 7);
                transaction.setEscrowReleaseDate(calendar.getTime());
                transaction.setIsEscrowed(true); // Đánh dấu tiền đang giữ
                
            } catch (Exception e) {
                log.error("Error parsing payment date", e);
            }
        } else {
            transaction.setStatus(TransactionStatus.FAILED);
        }
        
        transactionRepository.save(transaction);
        log.info("Transaction updated: {} - Status: {}", transaction.getTransactionCode(), transaction.getStatus());
    }

    private void updateOrderStatusAfterPayment(Orders order, Transaction transaction) {
        if (transaction.getStatus() == TransactionStatus.SUCCESS) {
            switch (transaction.getTransactionType()) {
                case DEPOSIT:
                    order.setStatus(OrderStatus.DA_DAT_COC); // Đã đặt cọc 10%
                    break;
                case FINAL_PAYMENT:
                    order.setStatus(OrderStatus.DA_THANH_TOAN); // Đã thanh toán đầy đủ
                    break;
                case BATTERY_PAYMENT:
                    order.setStatus(OrderStatus.DA_THANH_TOAN); // Đã thanh toán - chờ vận chuyển
                    break;
                case PACKAGE_PURCHASE:
                    order.setStatus(OrderStatus.DA_THANH_TOAN); // Đã thanh toán gói
                    // TODO: Lấy packageId từ đâu đó?
                    // Tạm thời comment, cần fix sau
                    // sellerService.activatePackageAfterPayment(order.getUsers().getUserid(), packageId);
                    break;
            }
        } else {
            order.setStatus(OrderStatus.THAT_BAI); // Thanh toán thất bại
        }
        
        order.setUpdatedat(new Date());
        orderRepository.save(order);
        log.info("Order {} status updated to: {}", order.getOrderid(), order.getStatus());
    }

    private String getIpAddress(HttpServletRequest request) {
        String ipAddress = request.getHeader("X-Forwarded-For");
        if (ipAddress == null || ipAddress.isEmpty()) {
            ipAddress = request.getRemoteAddr();
        }
        return ipAddress;
    }

    private User getCurrentUser() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !(auth.getPrincipal() instanceof CustomUserDetails)) {
            throw new RuntimeException("User not authenticated");
        }
        CustomUserDetails userDetails = (CustomUserDetails) auth.getPrincipal();
        return userDetails.getUser();
    }
}
