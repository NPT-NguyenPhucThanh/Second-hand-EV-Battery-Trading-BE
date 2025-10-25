package com.project.tradingev_batter.Controller;

import com.project.tradingev_batter.Entity.*;
import com.project.tradingev_batter.Service.*;
import com.project.tradingev_batter.dto.CheckoutRequest;
import com.project.tradingev_batter.dto.FeedbackRequest;
import com.project.tradingev_batter.dto.DisputeRequest;
import com.project.tradingev_batter.dto.PriceSuggestionRequest;
import com.project.tradingev_batter.dto.PriceSuggestionResponse;
import com.project.tradingev_batter.security.CustomUserDetails;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
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
@Tag(name = "Buyer APIs", description = "API dành cho người mua - Quản lý giỏ hàng, đặt hàng, đánh giá, khiếu nại, nâng cấp seller")
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
    private final TransactionService transactionService;
    private final GeminiAIService geminiAIService;

    public BuyerController(CartService cartService,
                          OrderService orderService,
                          FeedbackService feedbackService,
                          DisputeService disputeService,
                          ProductService productService,
                          DocuSealService docuSealService,
                          VNPayService vnPayService,
                          UserService userService,
                          ImageUploadService imageUploadService,
                          TransactionService transactionService,
                          GeminiAIService geminiAIService) {
        this.cartService = cartService;
        this.orderService = orderService;
        this.feedbackService = feedbackService;
        this.disputeService = disputeService;
        this.productService = productService;
        this.docuSealService = docuSealService;
        this.vnPayService = vnPayService;
        this.userService = userService;
        this.imageUploadService = imageUploadService;
        this.transactionService = transactionService;
        this.geminiAIService = geminiAIService;
    }

    //Thêm sản phẩm vào giỏ hàng
    @Operation(
            summary = "Thêm sản phẩm vào giỏ hàng",
            description = "Buyer thêm sản phẩm (xe/pin) vào giỏ hàng với số lượng chỉ định"
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Thêm vào giỏ hàng thành công"),
            @ApiResponse(responseCode = "400", description = "Sản phẩm không tồn tại hoặc hết hàng"),
            @ApiResponse(responseCode = "401", description = "Chưa đăng nhập")
    })
    @PostMapping("/cart/add")
    public ResponseEntity<Map<String, Object>> addToCart(
            @Parameter(description = "ID của sản phẩm cần thêm vào giỏ", required = true)
            @RequestParam Long productId,
            @Parameter(description = "Số lượng sản phẩm", example = "1")
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
    @Operation(
            summary = "Xem giỏ hàng",
            description = "Lấy danh sách sản phẩm trong giỏ hàng của buyer hiện tại"
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Trả về thông tin giỏ hàng"),
            @ApiResponse(responseCode = "401", description = "Chưa đăng nhập")
    })
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

    @Operation(
            summary = "Xóa sản phẩm khỏi giỏ hàng",
            description = "Xóa một item trong giỏ hàng"
    )
    @DeleteMapping("/cart/remove/{itemId}")
    public ResponseEntity<Map<String, Object>> removeFromCart(
            @Parameter(description = "ID của cart item cần xóa", required = true)
            @PathVariable Long itemId) {
        User buyer = getCurrentUser();
        cartService.removeFromCart(buyer.getUserid(), itemId);
        
        Map<String, Object> response = new HashMap<>();
        response.put("status", "success");
        response.put("message", "Đã xóa khỏi giỏ hàng");
        
        return ResponseEntity.ok(response);
    }

    @Operation(
            summary = "Cập nhật số lượng sản phẩm trong giỏ",
            description = "Thay đổi số lượng của một item trong giỏ hàng"
    )
    @PutMapping("/cart/update/{itemId}")
    public ResponseEntity<Map<String, Object>> updateCartItem(
            @Parameter(description = "ID của cart item", required = true)
            @PathVariable Long itemId,
            @Parameter(description = "Số lượng mới", required = true)
            @RequestParam int quantity) {
        
        User buyer = getCurrentUser();
        cart_items updatedItem = cartService.updateCartItemQuantity(buyer.getUserid(), itemId, quantity);
        
        Map<String, Object> response = new HashMap<>();
        response.put("status", "success");
        response.put("message", "Cập nhật giỏ hàng thành công");
        response.put("cartItem", updatedItem);
        
        return ResponseEntity.ok(response);
    }

    //Mua sản phẩm - Checkout (từ 1 product hoặc từ giỏ hàng)
    @Operation(
            summary = "Checkout - Tạo đơn hàng",
            description = "Tạo đơn hàng từ 1 sản phẩm (Mua ngay) hoặc từ giỏ hàng (nếu productId = null hoặc 0)"
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Đặt hàng thành công - Trả về thông tin order"),
            @ApiResponse(responseCode = "400", description = "Sản phẩm không khả dụng hoặc giỏ hàng trống")
    })
    @PostMapping("/checkout")
    public ResponseEntity<Map<String, Object>> checkout(@Valid @RequestBody CheckoutRequest request) {
        User buyer = getCurrentUser();
        
        Orders order;

        // Nếu có productId → Mua trực tiếp 1 sản phẩm
        if (request.getProductId() != null && request.getProductId() > 0) {
            order = orderService.createOrderFromProduct(
                    buyer.getUserid(),
                    request.getProductId(),
                    request.getQuantity() != null ? request.getQuantity() : 1,
                    request.getShippingAddress(),
                    request.getPaymentMethod()
            );
        } else {
            // Mua từ giỏ hàng
            order = orderService.createOrderFromCart(
                    buyer.getUserid(),
                    request.getShippingAddress(),
                    request.getPaymentMethod()
            );
        }

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
            response.put("nextStep", "Gọi API /api/buyer/orders/{orderId}/deposit để lưu thông tin giao dịch, sau đó thanh toán qua VNPay");
        }

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
    @Operation(
            summary = "Đặt cọc 10% cho đơn hàng xe",
            description = "Lưu thông tin giao dịch (điểm giao dịch, thời gian hẹn) và redirect sang VNPay để thanh toán đặt cọc"
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Thông tin đã lưu - Trả về hướng dẫn gọi API payment"),
            @ApiResponse(responseCode = "400", description = "Thiếu thông tin hoặc đơn hàng không hợp lệ")
    })
    @PostMapping("/orders/{orderId}/deposit")
    public ResponseEntity<Map<String, Object>> makeDeposit(
            @Parameter(description = "ID đơn hàng", required = true)
            @PathVariable Long orderId,
            @Parameter(description = "Địa điểm giao dịch", required = true)
            @RequestParam String transactionLocation,
            @Parameter(description = "Thời gian hẹn giao dịch (yyyy-MM-dd HH:mm:ss hoặc timestamp)", required = true)
            @RequestParam String appointmentDate,
            @Parameter(description = "Có sang tên xe không?")
            @RequestParam(required = false, defaultValue = "false") Boolean transferOwnership,
            @Parameter(description = "Có đổi biển số không?")
            @RequestParam(required = false, defaultValue = "false") Boolean changePlate,
            HttpServletRequest request) {
        
        User buyer = getCurrentUser();
        
        try {
            // ============= VALIDATION =============

            // Validate transactionLocation
            if (transactionLocation == null || transactionLocation.trim().isEmpty()) {
                return ResponseEntity.badRequest().body(Map.of(
                    "status", "error",
                    "message", "Vui lòng cung cấp điểm giao dịch (transactionLocation)"
                ));
            }

            // Validate appointmentDate
            if (appointmentDate == null || appointmentDate.trim().isEmpty()) {
                return ResponseEntity.badRequest().body(Map.of(
                    "status", "error",
                    "message", "Vui lòng cung cấp thời gian hẹn giao dịch (appointmentDate)"
                ));
            }

            // Parse appointmentDate
            Date parsedAppointmentDate;
            try {
                // Try parsing as timestamp first
                long timestamp = Long.parseLong(appointmentDate);
                parsedAppointmentDate = new Date(timestamp);
            } catch (NumberFormatException e) {
                // Try parsing as date string
                try {
                    java.text.SimpleDateFormat sdf = new java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
                    parsedAppointmentDate = sdf.parse(appointmentDate);
                } catch (Exception ex) {
                    return ResponseEntity.badRequest().body(Map.of(
                        "status", "error",
                        "message", "Định dạng appointmentDate không hợp lệ. Vui lòng dùng 'yyyy-MM-dd HH:mm:ss' hoặc timestamp"
                    ));
                }
            }

            // Validate appointmentDate phải là thời điểm trong tương lai
            if (parsedAppointmentDate.before(new Date())) {
                return ResponseEntity.badRequest().body(Map.of(
                    "status", "error",
                    "message", "Thời gian hẹn giao dịch phải là thời điểm trong tương lai"
                ));
            }

            // ============= BUSINESS LOGIC =============

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
            
            // Lưu thông tin giao dịch vào order
            order.setTransactionLocation(transactionLocation);
            order.setAppointmentDate(parsedAppointmentDate);
            order.setTransferOwnership(transferOwnership);
            order.setChangePlate(changePlate);
            order.setUpdatedat(new Date());
            orderService.updateOrder(order); // Cần thêm method này vào OrderService

            // REDIRECT SANG PAYMENT CONTROLLER để tạo VNPay URL
            Map<String, Object> response = new HashMap<>();
            response.put("status", "success");
            response.put("message", "Thông tin giao dịch đã được lưu. Vui lòng thanh toán đặt cọc");
            response.put("transactionLocation", transactionLocation);
            response.put("appointmentDate", parsedAppointmentDate);
            response.put("transferOwnership", transferOwnership);
            response.put("changePlate", changePlate);
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
    @Operation(
            summary = "Thanh toán phần còn lại (90%)",
            description = "Sau khi Manager duyệt, buyer đến điểm giao dịch và thanh toán 90% còn lại qua VNPay"
    )
    @PostMapping("/orders/{orderId}/final-payment")
    public ResponseEntity<Map<String, Object>> makeFinalPayment(
            @Parameter(description = "ID đơn hàng", required = true)
            @PathVariable Long orderId,
            @Parameter(description = "Có sang tên xe không?")
            @RequestParam(required = false) Boolean transferOwnership,
            @Parameter(description = "Có đổi biển số không?")
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
    @Operation(
            summary = "Xem lịch sử đơn hàng",
            description = "Lấy tất cả đơn hàng của buyer hiện tại"
    )
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

    @Operation(
            summary = "Xem chi tiết một đơn hàng",
            description = "Lấy thông tin chi tiết của đơn hàng bao gồm order details"
    )
    @GetMapping("/orders/{orderId}")
    public ResponseEntity<Map<String, Object>> getOrderDetail(
            @Parameter(description = "ID đơn hàng", required = true)
            @PathVariable Long orderId) {
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

    //XEM CHI TIẾT TRANSACTION HISTORY CỦA MỘT ORDER
    //Hiển thị tất cả transactions: DEPOSIT, FINAL_PAYMENT, REFUND, COMMISSION
    //API này giúp buyer theo dõi chi tiết luồng tiền của đơn hàng
    @Operation(
            summary = "Xem lịch sử giao dịch của đơn hàng",
            description = "Hiển thị tất cả transactions: DEPOSIT, FINAL_PAYMENT, REFUND, COMMISSION"
    )
    @GetMapping("/orders/{orderId}/transactions")
    public ResponseEntity<Map<String, Object>> getOrderTransactions(
            @Parameter(description = "ID đơn hàng", required = true)
            @PathVariable Long orderId) {
        User buyer = getCurrentUser();

        try {
            // Kiểm tra order có thuộc về buyer không
            Orders order = orderService.getOrderById(orderId);
            if (order.getUsers().getUserid() != buyer.getUserid()) {
                return ResponseEntity.badRequest().body(Map.of(
                    "status", "error",
                    "message", "Bạn không có quyền xem transactions của đơn hàng này"
                ));
            }

            // Lấy transaction history chi tiết
            Map<String, Object> transactionHistory = transactionService.getTransactionHistoryDetail(orderId);

            Map<String, Object> response = new HashMap<>();
            response.put("status", "success");
            response.putAll(transactionHistory); // Merge tất cả thông tin từ transactionHistory

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("status", "error");
            errorResponse.put("message", e.getMessage());
            return ResponseEntity.status(500).body(errorResponse);
        }
    }

    //Đánh giá sản phẩm và người bán sau khi hoàn tất giao dịch
    @Operation(
            summary = "Đánh giá sản phẩm và người bán",
            description = "Buyer tạo feedback sau khi hoàn tất giao dịch (chỉ khi order status = DA_HOAN_TAT)"
    )
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
    @Operation(
            summary = "Gửi khiếu nại",
            description = "Tạo dispute cho đơn hàng có vấn đề. Order status sẽ chuyển sang TRANH_CHAP."
    )
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
    @Operation(
            summary = "Xem danh sách khiếu nại của mình",
            description = "Lấy tất cả disputes do buyer tạo"
    )
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
    @Operation(
            summary = "Xác nhận đã nhận hàng (cho đơn pin)",
            description = "Buyer xác nhận đã nhận pin. Sau 3 ngày hệ thống sẽ tự động xác nhận nếu buyer không thao tác."
    )
    @PostMapping("/orders/{orderId}/confirm-receipt")
    public ResponseEntity<Map<String, Object>> confirmReceipt(
            @Parameter(description = "ID đơn hàng", required = true)
            @PathVariable Long orderId) {
        User buyer = getCurrentUser();
        
        Orders order = orderService.confirmReceipt(buyer.getUserid(), orderId);
        
        Map<String, Object> response = new HashMap<>();
        response.put("status", "success");
        response.put("message", "Xác nhận nhận hàng thành công");
        response.put("order", order);
        
        return ResponseEntity.ok(response);
    }

    //YÊU CẦU NÂNG CẤP LÊN SELLER
    //Buyer gửi yêu cầu với CCCD (mặt trước + mặt sau)
    //VALIDATION: Max 5MB, chỉ jpg/jpeg/png/pdf, bắt buộc upload cả 2 files CCCD
    @Operation(
            summary = "Gửi yêu cầu nâng cấp lên Seller",
            description = "Upload CCCD (mặt trước + mặt sau) để yêu cầu trở thành Seller. Max 5MB/file, chỉ chấp nhận jpg/jpeg/png/pdf."
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Yêu cầu đã gửi - Chờ Manager xét duyệt"),
            @ApiResponse(responseCode = "400", description = "File không hợp lệ hoặc đã là Seller")
    })
    @PostMapping("/request-seller-upgrade")
    public ResponseEntity<Map<String, Object>> requestSellerUpgrade(
            @Parameter(description = "CCCD mặt trước (max 5MB, jpg/jpeg/png/pdf)", required = true)
            @RequestParam("cccdFront") MultipartFile cccdFront,
            @Parameter(description = "CCCD mặt sau (max 5MB, jpg/jpeg/png/pdf)", required = true)
            @RequestParam("cccdBack") MultipartFile cccdBack) {

        User buyer = getCurrentUser();

        try {
            // VALIDATION: Kiểm tra file size và type
            List<MultipartFile> files = List.of(cccdFront, cccdBack);
            for (MultipartFile file : files) {
                // Check file không null và không empty
                if (file == null || file.isEmpty()) {
                    return ResponseEntity.badRequest().body(Map.of(
                        "status", "error",
                        "message", "Vui lòng upload đầy đủ CCCD (mặt trước + mặt sau)"
                    ));
                }

                // Check file size: max 5MB
                if (file.getSize() > 5 * 1024 * 1024) {
                    return ResponseEntity.badRequest().body(Map.of(
                        "status", "error",
                        "message", "File " + file.getOriginalFilename() + " vượt quá 5MB"
                    ));
                }

                // Check file type: jpg, jpeg, png, pdf
                String contentType = file.getContentType();
                if (contentType == null ||
                    !(contentType.equals("image/jpeg") ||
                      contentType.equals("image/jpg") ||
                      contentType.equals("image/png") ||
                      contentType.equals("application/pdf"))) {
                    return ResponseEntity.badRequest().body(Map.of(
                        "status", "error",
                        "message", "File " + file.getOriginalFilename() + " phải là jpg, jpeg, png hoặc pdf"
                    ));
                }
            }

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

            // Xóa vehicleRegistrationUrl nếu có (không cần nữa)
            buyer.setVehicleRegistrationUrl(null);

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
                "cccdBack", cccdBackUrl
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
    @Operation(
            summary = "Xem trạng thái yêu cầu nâng cấp",
            description = "Kiểm tra trạng thái yêu cầu nâng cấp lên Seller (NOT_REQUESTED, PENDING, APPROVED, REJECTED)"
    )
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

    //GEMINI AI - GỢI Ý GIÁ BÁN CHO BUYER/
    // Buyer tham khảo giá thị trường trước khi mua xe/pin
    // Rate limit: 10 requests/minute/user
    // Cache: 24 hours
    @Operation(
            summary = "Gợi ý giá bán từ Gemini AI",
            description = "Buyer tham khảo giá thị trường trước khi mua xe/pin. Rate limit: 10 requests/minute/user, cache 24h."
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Trả về gợi ý giá từ AI"),
            @ApiResponse(responseCode = "429", description = "Vượt quá giới hạn rate limit"),
            @ApiResponse(responseCode = "500", description = "Lỗi khi gọi AI API")
    })
    @PostMapping("/ai/suggest-price")
    public ResponseEntity<Map<String, Object>> suggestPrice(@Valid @RequestBody PriceSuggestionRequest request) {
        User buyer = getCurrentUser();

        try {
            // Gọi Gemini AI với rate limiting và caching
            PriceSuggestionResponse aiResponse = geminiAIService.suggestPrice(request, buyer.getUserid());

            Map<String, Object> response = new HashMap<>();
            response.put("status", "success");
            response.put("productType", request.getProductType());
            response.put("brand", request.getBrand());
            response.put("model", request.getModel());
            response.put("year", request.getYear());
            response.put("condition", request.getCondition());
            response.put("priceSuggestion", Map.of(
                "minPrice", aiResponse.getMinPrice(),
                "maxPrice", aiResponse.getMaxPrice(),
                "suggestedPrice", aiResponse.getSuggestedPrice(),
                "marketInsight", aiResponse.getMarketInsight()
            ));
            response.put("note", "Giá tham khảo từ Gemini AI. Giá thực tế có thể thay đổi tùy vào tình trạng cụ thể của sản phẩm.");

            return ResponseEntity.ok(response);

        } catch (RuntimeException e) {
            // Rate limit exceeded
            if (e.getMessage().contains("Rate limit exceeded")) {
                return ResponseEntity.status(429).body(Map.of(
                    "status", "error",
                    "message", e.getMessage()
                ));
            }

            // Other errors
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("status", "error");
            errorResponse.put("message", "Không thể lấy gợi ý giá: " + e.getMessage());
            return ResponseEntity.status(500).body(errorResponse);
        }
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
