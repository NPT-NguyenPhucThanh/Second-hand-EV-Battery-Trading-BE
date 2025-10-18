package com.project.tradingev_batter.Controller;

import com.project.tradingev_batter.Entity.*;
import com.project.tradingev_batter.Service.*;
import com.project.tradingev_batter.dto.CheckoutRequest;
import com.project.tradingev_batter.dto.FeedbackRequest;
import com.project.tradingev_batter.dto.DisputeRequest;
import com.project.tradingev_batter.security.CustomUserDetails;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/buyer")
public class BuyerController {

    private final CartService cartService;
    private final OrderService orderService;
    private final FeedbackService feedbackService;
    private final DisputeService disputeService;
    private final ProductService productService;
    private final DocuSealService docuSealService;
    private final VNPayService vnPayService;
    private final UserService userService;
    private final ImageUploadService imageUploadService;

    public BuyerController(CartService cartService,
                          OrderService orderService,
                          FeedbackService feedbackService,
                          DisputeService disputeService,
                          ProductService productService,
                          DocuSealService docuSealService,
                          VNPayService vnPayService,
                          UserService userService,
                          ImageUploadService imageUploadService) {
        this.cartService = cartService;
        this.orderService = orderService;
        this.feedbackService = feedbackService;
        this.disputeService = disputeService;
        this.productService = productService;
        this.docuSealService = docuSealService;
        this.vnPayService = vnPayService;
        this.userService = userService;
        this.imageUploadService = imageUploadService;
    }

    //Thêm sản phẩm vào giỏ hàng
    @PostMapping("/cart/add")
    public ResponseEntity<Map<String, Object>> addToCart(
            @RequestParam Long productId,
            @RequestParam(defaultValue = "1") int quantity) {
        
        User buyer = getCurrentUser();
        cart_items cartItem = cartService.addToCart(buyer.getUserid(), productId, quantity);
        
        Map<String, Object> response = new HashMap<>();
        response.put("status", "success");
        response.put("message", "Thêm vào giỏ hàng thành công");
        response.put("cartItem", cartItem);
        
        return ResponseEntity.ok(response);
    }

    //Xem giỏ hàng
    @GetMapping("/cart")
    public ResponseEntity<Map<String, Object>> getCart() {
        User buyer = getCurrentUser();
        Carts cart = cartService.getCart(buyer.getUserid());
        
        double totalAmount = cartService.calculateCartTotal(buyer.getUserid());
        
        Map<String, Object> response = new HashMap<>();
        response.put("status", "success");
        response.put("cart", cart);
        response.put("totalAmount", totalAmount);
        response.put("itemCount", cart.getCart_items().size());
        
        return ResponseEntity.ok(response);
    }

    @DeleteMapping("/cart/remove/{itemId}")
    public ResponseEntity<Map<String, Object>> removeFromCart(@PathVariable Long itemId) {
        User buyer = getCurrentUser();
        cartService.removeFromCart(buyer.getUserid(), itemId);
        
        Map<String, Object> response = new HashMap<>();
        response.put("status", "success");
        response.put("message", "Đã xóa khỏi giỏ hàng");
        
        return ResponseEntity.ok(response);
    }

    @PutMapping("/cart/update/{itemId}")
    public ResponseEntity<Map<String, Object>> updateCartItem(
            @PathVariable Long itemId,
            @RequestParam int quantity) {
        
        User buyer = getCurrentUser();
        cart_items updatedItem = cartService.updateCartItemQuantity(buyer.getUserid(), itemId, quantity);
        
        Map<String, Object> response = new HashMap<>();
        response.put("status", "success");
        response.put("message", "Cập nhật giỏ hàng thành công");
        response.put("cartItem", updatedItem);
        
        return ResponseEntity.ok(response);
    }

    //Mua sản phẩm trực tiếp (Mua ngay)
    @PostMapping("/orders/buy-now")
    public ResponseEntity<Map<String, Object>> buyNow(@Valid @RequestBody CheckoutRequest request) {
        User buyer = getCurrentUser();
        
        // Tạo đơn hàng trực tiếp từ 1 sản phẩm
        Orders order = orderService.createOrderFromProduct(
                buyer.getUserid(), 
                request.getProductId(), 
                request.getQuantity(),
                request.getShippingAddress(),
                request.getPaymentMethod()
        );
        
        Map<String, Object> response = new HashMap<>();
        response.put("status", "success");
        response.put("message", "Đặt hàng thành công");
        response.put("order", order);
        response.put("orderId", order.getOrderid());
        
        // Nếu là xe, cần đặt cọc 10%
        if (orderService.isCarOrder(order)) {
            double depositAmount = order.getTotalamount() * 0.10;
            response.put("requireDeposit", true);
            response.put("depositAmount", depositAmount);
            response.put("message", "Đơn hàng đã được tạo. Vui lòng thanh toán đặt cọc 10%");
            response.put("nextStep", "Gọi API /api/payment/create-payment-url với transactionType=DEPOSIT");
        }
        
        return ResponseEntity.ok(response);
    }

    //Mua từ giỏ hàng (Checkout)
    @PostMapping("/orders/checkout")
    public ResponseEntity<Map<String, Object>> checkout(@Valid @RequestBody CheckoutRequest request) {
        User buyer = getCurrentUser();
        
        Orders order = orderService.createOrderFromCart(
                buyer.getUserid(),
                request.getShippingAddress(),
                request.getPaymentMethod()
        );
        
        Map<String, Object> response = new HashMap<>();
        response.put("status", "success");
        response.put("message", "Đặt hàng thành công");
        response.put("order", order);
        response.put("orderId", order.getOrderid());
        
        return ResponseEntity.ok(response);
    }

    /**
     * ĐẶT CỌC 10% CHO ĐƠN HÀNG XE - REDIRECT SANG VNPAY
     * 1. Buyer gọi API này
     * 2. BE tạo transaction record (PENDING)
     * 3. BE tạo payment URL từ VNPay
     * 4. Frontend redirect buyer sang VNPay
     * 5. Buyer thanh toán trên VNPay
     * 6. VNPay callback về /api/payment/vnpay-ipn
     * 7. BE cập nhật transaction (SUCCESS)
     * 8. BE tạo hợp đồng DocuSeal
     * NOTE: API này CHỈ TẠO PAYMENT URL, không xử lý payment trực tiếp
     */
    @PostMapping("/orders/{orderId}/deposit")
    public ResponseEntity<Map<String, Object>> makeDeposit(
            @PathVariable Long orderId,
            @RequestParam String transactionLocation,
            HttpServletRequest request) {
        
        User buyer = getCurrentUser();
        
        try {
            // Kiểm tra order tồn tại và thuộc về buyer
            Orders order = orderService.getOrderById(orderId);
            if (order.getUsers().getUserid() != buyer.getUserid()) {
                return ResponseEntity.badRequest().body(Map.of(
                    "status", "error",
                    "message", "Bạn không có quyền thanh toán đơn hàng này"
                ));
            }
            
            // Kiểm tra order có phải xe không
            if (!orderService.isCarOrder(order)) {
                return ResponseEntity.badRequest().body(Map.of(
                    "status", "error",
                    "message", "Chỉ đơn hàng xe mới cần đặt cọc"
                ));
            }
            
            // Lưu transaction location vào order (để dùng sau khi thanh toán xong)
            order.setShippingaddress(transactionLocation);
            
            // REDIRECT SANG PAYMENT CONTROLLER để tạo VNPay URL
            Map<String, Object> response = new HashMap<>();
            response.put("status", "success");
            response.put("message", "Vui lòng gọi API Payment để thanh toán đặt cọc");
            response.put("nextStep", Map.of(
                "endpoint", "/api/payment/create-payment-url",
                "method", "POST",
                "params", Map.of(
                    "orderId", orderId,
                    "transactionType", "DEPOSIT"
                ),
                "note", "Frontend cần gọi API này để lấy paymentUrl, sau đó redirect buyer sang VNPay"
            ));
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("status", "error");
            errorResponse.put("message", e.getMessage());
            return ResponseEntity.status(500).body(errorResponse);
        }
    }

    /**
     * THANH TOÁN PHẦN CÒN LẠI (90%) - REDIRECT SANG VNPAY
     * 1. Buyer đã đặt cọc 10%
     * 2. Manager đã duyệt order
     * 3. Buyer đến điểm giao dịch
     * 4. Buyer gọi API này để thanh toán 90% còn lại
     * 5. Redirect sang VNPay
     */
    @PostMapping("/orders/{orderId}/final-payment")
    public ResponseEntity<Map<String, Object>> makeFinalPayment(
            @PathVariable Long orderId,
            @RequestParam(required = false) Boolean transferOwnership,
            @RequestParam(required = false) Boolean changePlate,
            HttpServletRequest request) {
        
        User buyer = getCurrentUser();
        
        try {
            // Kiểm tra order
            Orders order = orderService.getOrderById(orderId);
            if (order.getUsers().getUserid() != buyer.getUserid()) {
                return ResponseEntity.badRequest().body(Map.of(
                    "status", "error",
                    "message", "Bạn không có quyền thanh toán đơn hàng này"
                ));
            }
            
            //Lưu thông tin transferOwnership, changePlate vào Orders
            if (transferOwnership != null) {
                order.setTransferOwnership(transferOwnership);
            }
            if (changePlate != null) {
                order.setChangePlate(changePlate);
            }
            orderService.updateOrder(order);

            // REDIRECT SANG PAYMENT CONTROLLER
            Map<String, Object> response = new HashMap<>();
            response.put("status", "success");
            response.put("message", "Vui lòng gọi API Payment để thanh toán phần còn lại");
            response.put("nextStep", Map.of(
                "endpoint", "/api/payment/create-payment-url",
                "method", "POST",
                "params", Map.of(
                    "orderId", orderId,
                    "transactionType", "FINAL_PAYMENT"
                ),
                "additionalInfo", Map.of(
                    "transferOwnership", transferOwnership != null && transferOwnership,
                    "changePlate", changePlate != null && changePlate
                )
            ));
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("status", "error");
            errorResponse.put("message", e.getMessage());
            return ResponseEntity.status(500).body(errorResponse);
        }
    }

    //Xem lịch sử giao dịch
    @GetMapping("/orders")
    public ResponseEntity<Map<String, Object>> getOrders() {
        User buyer = getCurrentUser();
        List<Orders> orders = orderService.getOrders(buyer.getUserid());
        
        Map<String, Object> response = new HashMap<>();
        response.put("status", "success");
        response.put("orders", orders);
        response.put("totalOrders", orders.size());
        
        return ResponseEntity.ok(response);
    }

    @GetMapping("/orders/{orderId}")
    public ResponseEntity<Map<String, Object>> getOrderDetail(@PathVariable Long orderId) {
        User buyer = getCurrentUser();
        
        try {
            List<Order_detail> details = orderService.getOrderDetails(orderId, buyer.getUserid());
            Orders order = orderService.getOrderById(orderId);
            
            Map<String, Object> response = new HashMap<>();
            response.put("status", "success");
            response.put("order", order);
            response.put("details", details);
            
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("status", "error");
            errorResponse.put("message", e.getMessage());
            return ResponseEntity.badRequest().body(errorResponse);
        }
    }

    //Đánh giá sản phẩm và người bán sau khi hoàn tất giao dịch
    @PostMapping("/feedback")
    public ResponseEntity<Map<String, Object>> createFeedback(@Valid @RequestBody FeedbackRequest request) {
        User buyer = getCurrentUser();
        
        Feedback feedback = feedbackService.createFeedbackFromBuyer(
                buyer.getUserid(),
                request.getOrderId(),
                request.getProductId(),
                request.getRating(),
                request.getComment()
        );
        
        Map<String, Object> response = new HashMap<>();
        response.put("status", "success");
        response.put("message", "Đánh giá thành công");
        response.put("feedback", feedback);
        
        return ResponseEntity.ok(response);
    }

    //Gửi khiếu nại nếu có vấn đề xảy ra
    @PostMapping("/dispute")
    public ResponseEntity<Map<String, Object>> createDispute(@Valid @RequestBody DisputeRequest request) {
        User buyer = getCurrentUser();
        
        Dispute dispute = disputeService.createDispute(
                buyer.getUserid(),
                request.getOrderId(),
                request.getDescription(),
                request.getReasonType()
        );
        
        Map<String, Object> response = new HashMap<>();
        response.put("status", "success");
        response.put("message", "Khiếu nại đã được gửi. Manager sẽ xử lý trong thời gian sớm nhất.");
        response.put("dispute", dispute);
        
        return ResponseEntity.ok(response);
    }

    //Xem các khiếu nại của mình
    @GetMapping("/disputes")
    public ResponseEntity<Map<String, Object>> getDisputes() {
        User buyer = getCurrentUser();
        List<Dispute> disputes = disputeService.getDisputesByBuyer(buyer.getUserid());
        
        Map<String, Object> response = new HashMap<>();
        response.put("status", "success");
        response.put("disputes", disputes);
        
        return ResponseEntity.ok(response);
    }

    //Xác nhận đã nhận hàng (cho đơn hàng pin)
    @PostMapping("/orders/{orderId}/confirm-receipt")
    public ResponseEntity<Map<String, Object>> confirmReceipt(@PathVariable Long orderId) {
        User buyer = getCurrentUser();
        
        Orders order = orderService.confirmReceipt(buyer.getUserid(), orderId);
        
        Map<String, Object> response = new HashMap<>();
        response.put("status", "success");
        response.put("message", "Xác nhận nhận hàng thành công");
        response.put("order", order);
        
        return ResponseEntity.ok(response);
    }

    //YÊU CẦU NÂNG CẤP LÊN SELLER
    //Buyer gửi yêu cầu với CCCD và giấy tờ xe
    @PostMapping("/request-seller-upgrade")
    public ResponseEntity<Map<String, Object>> requestSellerUpgrade(
            @RequestParam("cccdFront") MultipartFile cccdFront,
            @RequestParam("cccdBack") MultipartFile cccdBack,
            @RequestParam(value = "vehicleRegistration", required = false) MultipartFile vehicleRegistration) {

        User buyer = getCurrentUser();

        try {
            // Kiểm tra đã có role Seller chưa
            boolean isSeller = buyer.getRoles().stream()
                    .anyMatch(role -> "SELLER".equals(role.getRolename()));

            if (isSeller) {
                return ResponseEntity.badRequest().body(Map.of(
                    "status", "error",
                    "message", "Bạn đã là Seller rồi"
                ));
            }

            // Kiểm tra đã có yêu cầu pending chưa
            if ("PENDING".equals(buyer.getSellerUpgradeStatus())) {
                return ResponseEntity.badRequest().body(Map.of(
                    "status", "error",
                    "message", "Bạn đã có yêu cầu đang chờ xét duyệt"
                ));
            }

            // Upload CCCD front
            String cccdFrontUrl = imageUploadService.uploadImage(cccdFront, "seller_upgrade/cccd_front_" + buyer.getUserid());
            buyer.setCccdFrontUrl(cccdFrontUrl);

            // Upload CCCD back
            String cccdBackUrl = imageUploadService.uploadImage(cccdBack, "seller_upgrade/cccd_back_" + buyer.getUserid());
            buyer.setCccdBackUrl(cccdBackUrl);

            // Upload vehicle registration nếu có
            if (vehicleRegistration != null && !vehicleRegistration.isEmpty()) {
                String vehicleRegUrl = imageUploadService.uploadImage(vehicleRegistration, "seller_upgrade/vehicle_" + buyer.getUserid());
                buyer.setVehicleRegistrationUrl(vehicleRegUrl);
            }

            // Cập nhật status
            buyer.setSellerUpgradeStatus("PENDING");
            buyer.setSellerUpgradeRequestDate(new Date());
            userService.updateUser(buyer.getUserid(), buyer);

            Map<String, Object> response = new HashMap<>();
            response.put("status", "success");
            response.put("message", "Yêu cầu nâng cấp lên Seller đã được gửi. Vui lòng chờ Manager xét duyệt.");
            response.put("requestDate", buyer.getSellerUpgradeRequestDate());
            response.put("uploadedDocuments", Map.of(
                "cccdFront", cccdFrontUrl,
                "cccdBack", cccdBackUrl,
                "vehicleRegistration", buyer.getVehicleRegistrationUrl() != null ? buyer.getVehicleRegistrationUrl() : "N/A"
            ));

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("status", "error");
            errorResponse.put("message", "Không thể upload tài liệu: " + e.getMessage());
            return ResponseEntity.status(500).body(errorResponse);
        }
    }

    //XEM TRẠNG THÁI YÊU CẦU NÂNG CẤP
    @GetMapping("/seller-upgrade-status")
    public ResponseEntity<Map<String, Object>> getSellerUpgradeStatus() {
        User buyer = getCurrentUser();

        Map<String, Object> response = new HashMap<>();
        response.put("status", "success");

        if (buyer.getSellerUpgradeStatus() == null) {
            response.put("upgradeStatus", "NOT_REQUESTED");
            response.put("message", "Bạn chưa gửi yêu cầu nâng cấp");
        } else {
            response.put("upgradeStatus", buyer.getSellerUpgradeStatus());
            response.put("requestDate", buyer.getSellerUpgradeRequestDate());

            switch (buyer.getSellerUpgradeStatus()) {
                case "PENDING":
                    response.put("message", "Yêu cầu đang chờ Manager xét duyệt");
                    break;
                case "APPROVED":
                    response.put("message", "Yêu cầu đã được chấp nhận. Bạn đã là Seller!");
                    break;
                case "REJECTED":
                    response.put("message", "Yêu cầu bị từ chối");
                    response.put("rejectionReason", buyer.getRejectionReason());
                    break;
            }
        }

        return ResponseEntity.ok(response);
    }

    // =============== HELPER METHODS ==================================================================================
    
    private String uploadImageToCloudinary(MultipartFile file, String folderPath) throws Exception {
        // Sử dụng ImageUploadService có sẵn
        return imageUploadService.uploadImage(file, folderPath);
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
