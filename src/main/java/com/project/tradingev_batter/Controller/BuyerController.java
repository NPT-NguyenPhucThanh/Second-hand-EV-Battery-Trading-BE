package com.project.tradingev_batter.Controller;

import com.project.tradingev_batter.Entity.*;
import com.project.tradingev_batter.Service.*;
import com.project.tradingev_batter.dto.CheckoutRequest;
import com.project.tradingev_batter.dto.FeedbackRequest;
import com.project.tradingev_batter.dto.DisputeRequest;
import com.project.tradingev_batter.security.CustomUserDetails;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

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

    public BuyerController(CartService cartService,
                          OrderService orderService,
                          FeedbackService feedbackService,
                          DisputeService disputeService,
                          ProductService productService,
                          DocuSealService docuSealService) {
        this.cartService = cartService;
        this.orderService = orderService;
        this.feedbackService = feedbackService;
        this.disputeService = disputeService;
        this.productService = productService;
        this.docuSealService = docuSealService;
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
    public ResponseEntity<Map<String, Object>> buyNow(@RequestBody CheckoutRequest request) {
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
            response.put("message", "Vui lòng đặt cọc 10% để tiếp tục");
        }
        
        return ResponseEntity.ok(response);
    }

    //Mua từ giỏ hàng (Checkout)
    @PostMapping("/orders/checkout")
    public ResponseEntity<Map<String, Object>> checkout(@RequestBody CheckoutRequest request) {
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

    //Đặt cọc 10% cho đơn hàng xe
    @PostMapping("/orders/{orderId}/deposit")
    public ResponseEntity<Map<String, Object>> makeDeposit(
            @PathVariable Long orderId,
            @RequestParam String paymentMethod,
            @RequestParam String transactionLocation) {
        
        User buyer = getCurrentUser();
        
        // Xử lý đặt cọc
        Transaction transaction = orderService.processDeposit(buyer.getUserid(), orderId, paymentMethod);
        Orders order = transaction.getOrders();
        
        //Tạo hợp đồng mua bán qua DocuSeal
        try {
            // Lấy seller từ order
            User seller = order.getDetails().get(0).getProducts().getUsers();
            
            // Tạo hợp đồng mua bán
            Contracts contract = docuSealService.createSaleTransactionContract(
                    order, buyer, seller, transactionLocation);
            
            Map<String, Object> response = new HashMap<>();
            response.put("status", "success");
            response.put("message", "Đặt cọc thành công. Vui lòng ký hợp đồng điện tử để hoàn tất.");
            response.put("transaction", transaction);
            response.put("depositAmount", order.getTotalamount() * 0.10);
            response.put("contract", Map.of(
                    "contractId", contract.getContractid(),
                    "submissionId", contract.getDocusealSubmissionId(),
                    "status", "pending_signature"
            ));
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("status", "error");
            errorResponse.put("message", "Đặt cọc thành công nhưng không thể tạo hợp đồng: " + e.getMessage());
            errorResponse.put("transaction", transaction);
            return ResponseEntity.status(500).body(errorResponse);
        }
    }

    //Thanh toán phần còn lại khi đến điểm giao dịch
    @PostMapping("/orders/{orderId}/final-payment")
    public ResponseEntity<Map<String, Object>> makeFinalPayment(
            @PathVariable Long orderId,
            @RequestParam String paymentMethod,
            @RequestParam(required = false) Boolean transferOwnership,
            @RequestParam(required = false) Boolean changePlate) {
        
        User buyer = getCurrentUser();
        
        Transaction transaction = orderService.processFinalPayment(
                buyer.getUserid(), 
                orderId, 
                paymentMethod,
                transferOwnership != null && transferOwnership,
                changePlate != null && changePlate
        );
        
        Map<String, Object> response = new HashMap<>();
        response.put("status", "success");
        response.put("message", "Thanh toán thành công. Giao dịch hoàn tất.");
        response.put("transaction", transaction);
        response.put("transferOwnership", transferOwnership);
        response.put("changePlate", changePlate);
        
        return ResponseEntity.ok(response);
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
    @PostMapping("/feedbacks")
    public ResponseEntity<Map<String, Object>> createFeedback(@RequestBody FeedbackRequest request) {
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
    @PostMapping("/disputes")
    public ResponseEntity<Map<String, Object>> createDispute(@RequestBody DisputeRequest request) {
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

    // =============== HELPER METHODS ==================================================================================
    
    private User getCurrentUser() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !(auth.getPrincipal() instanceof CustomUserDetails)) {
            throw new RuntimeException("User not authenticated");
        }
        CustomUserDetails userDetails = (CustomUserDetails) auth.getPrincipal();
        return userDetails.getUser();
    }
}
