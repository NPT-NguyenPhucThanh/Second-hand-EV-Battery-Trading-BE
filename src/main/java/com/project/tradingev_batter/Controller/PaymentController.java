package com.project.tradingev_batter.Controller;

import com.project.tradingev_batter.Entity.*;
import com.project.tradingev_batter.Repository.OrderRepository;
import com.project.tradingev_batter.Repository.TransactionRepository;
import com.project.tradingev_batter.Service.VNPayService;
import com.project.tradingev_batter.Service.SellerService;
import com.project.tradingev_batter.Service.DocuSealService;
import com.project.tradingev_batter.Service.NotificationService;
import com.project.tradingev_batter.enums.OrderStatus;
import com.project.tradingev_batter.enums.ProductStatus;
import com.project.tradingev_batter.enums.TransactionStatus;
import com.project.tradingev_batter.enums.TransactionType;
import com.project.tradingev_batter.security.CustomUserDetails;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
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
@Tag(name = "Payment APIs", description = "API thanh toán - Tích hợp VNPay cho đặt cọc, thanh toán cuối, mua gói dịch vụ")
@Slf4j
public class PaymentController {

    private final VNPayService vnPayService;
    private final TransactionRepository transactionRepository;
    private final OrderRepository orderRepository;
    private final SellerService sellerService;
    private final DocuSealService docuSealService;
    private final NotificationService notificationService;

    public PaymentController(VNPayService vnPayService,
                           TransactionRepository transactionRepository,
                           OrderRepository orderRepository,
                           SellerService sellerService,
                           DocuSealService docuSealService,
                           NotificationService notificationService) {
        this.vnPayService = vnPayService;
        this.transactionRepository = transactionRepository;
        this.orderRepository = orderRepository;
        this.sellerService = sellerService;
        this.docuSealService = docuSealService;
        this.notificationService = notificationService;
    }

    //TẠO PAYMENT URL - User click "Thanh toán" → API này tạo VNPay URL
    @Operation(
            summary = "Tạo URL thanh toán VNPay",
            description = "Tạo payment URL để redirect buyer/seller sang VNPay. Hỗ trợ các loại: DEPOSIT (đặt cọc 10%), FINAL_PAYMENT (90% còn lại), PACKAGE_PURCHASE (mua gói)"
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Thành công - Trả về payment URL để redirect"),
            @ApiResponse(responseCode = "400", description = "Thiếu thông tin hoặc order không hợp lệ"),
            @ApiResponse(responseCode = "500", description = "Lỗi khi tạo payment URL")
    })
    @PostMapping("/create-payment-url")
    public ResponseEntity<Map<String, Object>> createPaymentUrl(
            @Parameter(description = "ID đơn hàng", required = true)
            @RequestParam Long orderId,
            @Parameter(description = "Loại thanh toán: DEPOSIT, FINAL_PAYMENT, PACKAGE_PURCHASE", required = true)
            @RequestParam String transactionType,
            HttpServletRequest request) {
        
        try {
            User user = getCurrentUser();
            Orders order = orderRepository.findById(orderId)
                    .orElseThrow(() -> new RuntimeException("Order not found"));
            
            // Kiểm tra quyền sở hữu
            if (!order.getUsers().getUserid().equals(user.getUserid())) {
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

            // LƯU TRANSACTION TRƯỚC KHI TẠO PAYMENT URL (quan trọng!)
            transaction = transactionRepository.save(transaction);
            log.info("Transaction saved with ID: {}, Code: {}", transaction.getTransid(), transactionCode);

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
    @Operation(
            summary = "VNPay Return URL",
            description = "URL mà VNPay redirect buyer/seller về sau khi thanh toán (dùng cho Frontend hiển thị kết quả)"
    )
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

            // Find transaction
            Transaction transaction = transactionRepository.findByTransactionCode(transactionCode)
                    .orElseThrow(() -> new RuntimeException("Transaction not found"));
            
            // XỬ LÝ STATUS NGAY TẠI ĐÂY (không cần chờ IPN)
            if (transaction.getStatus() == TransactionStatus.PENDING) {
                // Update transaction
                updateTransactionFromVNPay(transaction, vnpayData);

                // Update order status
                Orders order = transaction.getOrders();
                updateOrderStatusAfterPayment(order, transaction);

                log.info("Transaction processed via Return URL: {}", transactionCode);
            } else {
                log.info("Transaction already processed: {}", transactionCode);
            }

            // Build response
            Map<String, Object> response = new HashMap<>();
            response.put("status", vnPayService.isPaymentSuccess(vnpayData.get("responseCode")) ? "success" : "failed");
            response.put("message", vnPayService.getResponseMessage(vnpayData.get("responseCode")));
            response.put("transactionCode", transactionCode);
            response.put("amount", Double.parseDouble(vnpayData.get("amount")) / 100);
            response.put("bankCode", vnpayData.get("bankCode"));
            response.put("cardType", vnpayData.get("cardType"));
            response.put("paymentDate", vnpayData.get("paymentDate"));
            response.put("orderId", transaction.getOrders().getOrderid());
            response.put("orderStatus", transaction.getOrders().getStatus());

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
    @Operation(
            summary = "VNPay IPN Callback",
            description = "Webhook callback từ VNPay sau khi thanh toán. VNPay tự động gọi endpoint này để thông báo kết quả thanh toán."
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Xử lý callback thành công"),
            @ApiResponse(responseCode = "400", description = "Dữ liệu callback không hợp lệ")
    })
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

                    //AUTO-CREATE CONTRACT sau khi đặt cọc thành công
                    try {
                        createContractAfterDeposit(order);
                        log.info("Contract created successfully for order {}", order.getOrderid());
                    } catch (Exception e) {
                        log.error("Failed to create DocuSeal contract: {}", e.getMessage(), e);
                        // Gửi notification cho staff để tạo contract manual
                        try {
                            User buyer = order.getUsers();
                            notificationService.createNotification(buyer.getUserid(),
                                    "Lỗi tạo hợp đồng điện tử",
                                    "Hệ thống không thể tạo hợp đồng tự động cho đơn hàng #" + order.getOrderid() +
                                            ". Vui lòng liên hệ Staff để được hỗ trợ. Lỗi: " + e.getMessage());
                        } catch (Exception notifEx) {
                            log.error("Failed to send notification: {}", notifEx.getMessage());
                        }
                        // KHÔNG throw exception để không làm fail payment flow
                    }
                    break;
                case FINAL_PAYMENT:
                    order.setStatus(OrderStatus.DA_THANH_TOAN); // Đã thanh toán đầy đủ

                    // Cập nhật product status thành ĐÃ BÁN
                    markProductsAsSold(order);
                    break;
                case BATTERY_PAYMENT:
                    order.setStatus(OrderStatus.DA_THANH_TOAN); // Đã thanh toán - chờ vận chuyển

                    // Cập nhật product status thành ĐÃ BÁN
                    markProductsAsSold(order);
                    break;
                case PACKAGE_PURCHASE:
                    order.setStatus(OrderStatus.DA_THANH_TOAN); // Đã thanh toán gói
                    //Lấy packageId từ order và activate package
                    if (order.getPackageId() != null) {
                        sellerService.activatePackageAfterPayment(order.getUsers().getUserid(), order.getPackageId());
                        log.info("Package {} activated for user {}", order.getPackageId(), order.getUsers().getUserid());
                    } else {
                        log.error("PackageId is null for order {}", order.getOrderid());
                    }
                    break;
            }
        } else {
            order.setStatus(OrderStatus.THAT_BAI); // Thanh toán thất bại
        }
        
        order.setUpdatedat(new Date());
        orderRepository.save(order);
        log.info("Order {} status updated to: {}", order.getOrderid(), order.getStatus());
    }

    //ĐÁNH DẤU SẢN PHẨM ĐÃ BÁN SAU KHI THANH TOÁN ĐẦY ĐỦ
    //Nếu amount > 1: giảm số lượng theo quantity đã mua
    //Nếu amount == 1 hoặc sau khi giảm mà amount == 0: chuyển status thành DA_BAN
    private void markProductsAsSold(Orders order) {
        try {
            if (order.getDetails() != null && !order.getDetails().isEmpty()) {
                for (Order_detail detail : order.getDetails()) {
                    Product product = detail.getProducts();
                    if (product != null) {
                        int quantityOrdered = detail.getQuantity(); // Số lượng buyer đã mua
                        int currentAmount = product.getAmount();    // Số lượng hiện có trong kho

                        log.info("Processing product {}: current amount = {}, ordered quantity = {}",
                                 product.getProductid(), currentAmount, quantityOrdered);

                        if (currentAmount > quantityOrdered) {
                            // Còn hàng trong kho → giảm số lượng
                            product.setAmount(currentAmount - quantityOrdered);
                            product.setUpdatedat(new Date());
                            log.info("Product {} amount reduced to {}", product.getProductid(), product.getAmount());
                        } else {
                            // Hết hàng hoặc bán hết → chuyển status DA_BAN
                            product.setAmount(0);
                            product.setStatus(ProductStatus.DA_BAN);
                            product.setUpdatedat(new Date());
                            log.info("Product {} marked as SOLD (out of stock)", product.getProductid());
                        }
                        // Product sẽ được save tự động do cascade
                    }
                }
            }
        } catch (Exception e) {
            log.error("Error marking products as sold for order {}: {}", order.getOrderid(), e.getMessage());
        }
    }

    /**
     * TỰ ĐỘNG TẠO HỢP ĐỒNG SAU KHI ĐẶT CỌC THÀNH CÔNG
     * 1. Lấy thông tin buyer, seller, product từ order
     * 2. Gọi DocuSealService để tạo hợp đồng
     * 3. Gửi email/notification cho buyer và seller ký
     * 4. Notify manager để duyệt
     */
    private void createContractAfterDeposit(Orders order) {
        try {
            // Fetch order với details để tránh lazy initialization
            order = orderRepository.findByIdWithDetails(order.getOrderid())
                    .orElseThrow(() -> new RuntimeException("Order not found"));

            // Lấy thông tin từ order
            User buyer = order.getUsers();

            if (order.getDetails() == null || order.getDetails().isEmpty()) {
                log.error("Order {} has no details", order.getOrderid());
                throw new RuntimeException("Order has no details");
            }

            Order_detail detail = order.getDetails().get(0);
            Product product = detail.getProducts();
            User seller = product.getUsers();

            // Tạo hợp đồng mua bán xe
            String transactionLocation = order.getTransactionLocation() != null ?
                    order.getTransactionLocation() : "Chưa xác định";

            // Gọi DocuSealService để tạo hợp đồng
            docuSealService.createSaleTransactionContract(order, buyer, seller, transactionLocation);
            log.info("Sale transaction contract created successfully for order {}", order.getOrderid());

            // NOTE: Notifications đã được tạo tự động trong DocuSealServiceImpl.createSaleTransactionContract()
            // với URLs ký cụ thể cho từng bên (buyer và seller)

        } catch (Exception e) {
            log.error("Failed to create DocuSeal contract: " + e.getMessage(), e);
            // Không throw exception để không block flow thanh toán
            // Có thể retry sau hoặc tạo manual
        }
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
