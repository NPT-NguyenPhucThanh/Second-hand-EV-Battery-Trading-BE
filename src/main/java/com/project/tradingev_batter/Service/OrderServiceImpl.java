package com.project.tradingev_batter.Service;

import java.util.Date;
import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.project.tradingev_batter.Entity.Carts;
import com.project.tradingev_batter.Entity.Notification;
import com.project.tradingev_batter.Entity.Order_detail;
import com.project.tradingev_batter.Entity.Orders;
import com.project.tradingev_batter.Entity.Product;
import com.project.tradingev_batter.Entity.Transaction;
import com.project.tradingev_batter.Entity.User;
import com.project.tradingev_batter.Entity.cart_items;
import com.project.tradingev_batter.Repository.NotificationRepository;
import com.project.tradingev_batter.Repository.OrderDetailRepository;
import com.project.tradingev_batter.Repository.OrderRepository;
import com.project.tradingev_batter.Repository.ProductRepository;
import com.project.tradingev_batter.Repository.TransactionRepository;
import com.project.tradingev_batter.Repository.UserRepository;
import com.project.tradingev_batter.enums.OrderStatus;
import com.project.tradingev_batter.enums.ProductStatus;
import com.project.tradingev_batter.enums.TransactionStatus;

@Service
public class OrderServiceImpl implements OrderService {

    private final OrderRepository orderRepository;
    private final UserRepository userRepository;
    private final OrderDetailRepository orderDetailRepository;
    private final ProductRepository productRepository;
    private final CartService cartService;
    private final TransactionRepository transactionRepository;
    private final NotificationRepository notificationRepository;

    public OrderServiceImpl(OrderRepository orderRepository,
            UserRepository userRepository,
            OrderDetailRepository orderDetailRepository,
            ProductRepository productRepository,
            CartService cartService,
            TransactionRepository transactionRepository,
            NotificationRepository notificationRepository) {
        this.orderRepository = orderRepository;
        this.userRepository = userRepository;
        this.orderDetailRepository = orderDetailRepository;
        this.productRepository = productRepository;
        this.cartService = cartService;
        this.transactionRepository = transactionRepository;
        this.notificationRepository = notificationRepository;
    }

    @Override
    @Transactional(readOnly = true)
    public List<Orders> getOrders(long userid) {
        return orderRepository.findByUsersUserid(userid);
    }

    @Override
    @Transactional(readOnly = true)
    public List<Order_detail> getOrderDetails(long orderId, long userid) throws Exception {
        Orders order = orderRepository.findById(orderId)
                .orElseThrow(() -> new Exception("Order not found with ID: " + orderId));

        if (order.getUsers().getUserid() != userid) {
            throw new Exception("This order does not belong to the user.");
        }
        return orderDetailRepository.findByOrders(order);
    }

    @Override
    @Transactional(readOnly = true)
    public Orders getOrderById(Long orderId) {
        // Fetch order với details, contracts sẽ được lazy load sau nếu cần
        // Không fetch cả 2 cùng lúc để tránh MultipleBagFetchException
        Orders order = orderRepository.findByIdWithDetails(orderId)
                .orElseThrow(() -> new RuntimeException("Order not found"));

        // Khởi tạo contracts collection để tránh LazyInitializationException
        if (order.getContracts() != null) {
            //noinspection ResultOfMethodCallIgnored
            order.getContracts().size(); // Trigger lazy loading
        }

        return order;
    }

    @Override
    @Transactional
    public Orders updateOrder(Orders order) {
        order.setUpdatedat(new Date());
        return orderRepository.save(order);
    }

    //Tạo đơn hàng từ 1 sản phẩm (Mua ngay)
    @Override
    @Transactional
    public Orders createOrderFromProduct(Long userId, Long productId, int quantity,
            String shippingAddress, String paymentMethod) {
        User buyer = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found"));

        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new RuntimeException("Product not found"));

        // Kiểm tra sản phẩm có sẵn không - sử dụng enum ProductStatus
        if (!ProductStatus.DANG_BAN.equals(product.getStatus()) && !ProductStatus.DA_DUYET.equals(product.getStatus())) {
            throw new RuntimeException("Sản phẩm không khả dụng để mua");
        }

        // Tạo order
        Orders order = new Orders();
        order.setUsers(buyer);
        order.setTotalamount(product.getCost() * quantity);
        order.setShippingfee(0.0); // Sẽ tính sau nếu là pin
        order.setTotalfinal(order.getTotalamount());
        order.setShippingaddress(shippingAddress);
        order.setPaymentmethod(paymentMethod);
        order.setCreatedat(new Date());

        // Set status dựa trên loại sản phẩm
        if ("Car EV".equals(product.getType())) {
            order.setStatus(OrderStatus.CHO_DAT_COC); // Xe: Chờ đặt cọc 10%
            order.setTransferOwnership(true);
            order.setChangePlate(true);
        } else {
            // Pin: Chờ thanh toán 100% (không cần chờ xác nhận)
            order.setStatus(OrderStatus.CHO_THANH_TOAN);
            // Tính phí ship cho pin
            double shippingFee = calculateShippingFee(shippingAddress);
            order.setShippingfee(shippingFee);
            order.setTotalfinal(order.getTotalamount() + shippingFee);
        }

        order = orderRepository.save(order);

        // Tạo order detail
        Order_detail detail = new Order_detail();
        detail.setOrders(order);
        detail.setProducts(product);
        detail.setQuantity(quantity);
        detail.setUnit_price(product.getCost());
        orderDetailRepository.save(detail);

        // Tạo notification
        String message = "Car EV".equals(product.getType())
                ? "Đơn hàng #" + order.getOrderid() + " đã được tạo. Vui lòng đặt cọc 10%."
                : "Đơn hàng #" + order.getOrderid() + " đã được tạo. Vui lòng thanh toán.";
        createNotification(buyer, "Đặt hàng thành công", message);

        return order;
    }

    //Tạo đơn hàng từ giỏ hàng
    @Override
    @Transactional
    public Orders createOrderFromCart(Long userId, String shippingAddress, String paymentMethod) {
        User buyer = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found"));

        Carts cart = cartService.getCart(userId);

        if (cart.getCart_items().isEmpty()) {
            throw new RuntimeException("Giỏ hàng trống");
        }

        // Tính tổng tiền
        double totalAmount = cartService.calculateCartTotal(userId);

        // Kiểm tra loại sản phẩm trong giỏ hàng
        boolean hasCar = cart.getCart_items().stream()
                .anyMatch(item -> "Car EV".equals(item.getProducts().getType()));
        boolean hasBattery = cart.getCart_items().stream()
                .anyMatch(item -> "Battery".equals(item.getProducts().getType()));

        System.out.println("===== DEBUG CART CHECKOUT =====");
        System.out.println("Cart items count: " + cart.getCart_items().size());
        cart.getCart_items().forEach(item -> {
            System.out.println("Product: " + item.getProducts().getProductname()
                    + " | Type: " + item.getProducts().getType()
                    + " | Cost: " + item.getProducts().getCost());
        });
        System.out.println("Has Car: " + hasCar);
        System.out.println("Has Battery: " + hasBattery);
        System.out.println("==============================");

        // Tính phí ship (chỉ tính cho pin)
        double shippingFee = 0.0;
        if (hasBattery) {
            shippingFee = calculateShippingFee(shippingAddress);
            System.out.println("Calculated shipping fee: " + shippingFee);
        }

        // Tạo order
        Orders order = new Orders();
        order.setUsers(buyer);
        order.setTotalamount(totalAmount);
        order.setShippingfee(shippingFee);
        order.setTotalfinal(totalAmount + shippingFee);
        order.setShippingaddress(shippingAddress);
        order.setPaymentmethod(paymentMethod);
        order.setCreatedat(new Date());

        // Set status dựa trên loại sản phẩm
        if (hasCar) {
            order.setStatus(OrderStatus.CHO_DAT_COC); // Xe: Chờ đặt cọc 10%
            // Tự động set transferOwnership và changePlate = true cho đơn hàng xe
            order.setTransferOwnership(true);
            order.setChangePlate(true);
        } else {
            // Chỉ pin: Chờ thanh toán 100%
            order.setStatus(OrderStatus.CHO_THANH_TOAN);
        }

        order = orderRepository.save(order);

        // Tạo order details từ cart items
        for (cart_items cartItem : cart.getCart_items()) {
            Order_detail detail = new Order_detail();
            detail.setOrders(order);
            detail.setProducts(cartItem.getProducts());
            detail.setQuantity(cartItem.getQuantity());
            detail.setUnit_price(cartItem.getProducts().getCost());
            orderDetailRepository.save(detail);
        }

        // Xóa giỏ hàng
        cartService.clearCart(userId);

        // Tạo notification
        createNotification(buyer, "Đặt hàng thành công",
                "Đơn hàng #" + order.getOrderid() + " đã được tạo từ giỏ hàng.");

        return order;
    }

    @Override
    public boolean isCarOrder(Orders order) {
        return order.getDetails().stream()
                .anyMatch(detail -> "Car EV".equals(detail.getProducts().getType()));
    }

    @Override
    public boolean isBatteryOrder(Orders order) {
        return order.getDetails().stream()
                .anyMatch(detail -> "Battery".equals(detail.getProducts().getType()));
    }

    //Đặt cọc 10% cho đơn hàng xe
    @Override
    @Transactional
    public Transaction processDeposit(Long userId, Long orderId, String paymentMethod) {
        User buyer = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found"));

        Orders order = orderRepository.findByIdWithDetails(orderId)
                .orElseThrow(() -> new RuntimeException("Order not found"));

        // Kiểm tra quyền sở hữu
        if (!order.getUsers().getUserid().equals(userId)) {
            throw new RuntimeException("Bạn không có quyền thực hiện hành động này");
        }

        // Kiểm tra trạng thái
        if (!OrderStatus.CHO_DAT_COC.equals(order.getStatus())) {
            throw new RuntimeException("Đơn hàng không ở trạng thái chờ đặt cọc");
        }

        // Tính tiền cọc 10%
        double depositAmount = order.getTotalamount() * 0.10;

        // Tạo transaction đặt cọc
        Transaction transaction = new Transaction();
        transaction.setOrders(order);
        transaction.setCreatedBy(buyer);
        transaction.setMethod(paymentMethod);
        transaction.setStatus(TransactionStatus.SUCCESS); // Giả sử thanh toán thành công (tích hợp VnPay sau)
        transaction.setCreatedat(new Date());
        transaction = transactionRepository.save(transaction);

        // Cập nhật trạng thái đơn hàng
        order.setStatus(OrderStatus.CHO_DUYET); // Chờ manager duyệt
        order.setUpdatedat(new Date());
        orderRepository.save(order);

        // Tạo notification
        createNotification(buyer, "Đặt cọc thành công",
                "Bạn đã đặt cọc " + depositAmount + " VNĐ cho đơn hàng #" + orderId + ". Đang chờ manager duyệt.");

        return transaction;
    }

    //Thanh toán phần còn lại khi đến điểm giao dịch
    @Override
    @Transactional
    public Transaction processFinalPayment(Long userId, Long orderId, String paymentMethod,
            boolean transferOwnership, boolean changePlate) {
        User buyer = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found"));

        Orders order = orderRepository.findByIdWithDetails(orderId)
                .orElseThrow(() -> new RuntimeException("Order not found"));

        // Kiểm tra quyền sở hữu
        if (!order.getUsers().getUserid().equals(userId)) {
            throw new RuntimeException("Bạn không có quyền thực hiện hành động này");
        }

        // Kiểm tra trạng thái (đã duyệt bởi manager)
        if (!OrderStatus.DA_DUYET.equals(order.getStatus())) {
            throw new RuntimeException("Đơn hàng chưa được duyệt");
        }

        // Tính phần còn lại 90%
        double remainingAmount = order.getTotalamount() * 0.90;

        // Tạo transaction thanh toán cuối
        Transaction transaction = new Transaction();
        transaction.setOrders(order);
        transaction.setCreatedBy(buyer);
        transaction.setMethod(paymentMethod);
        transaction.setStatus(TransactionStatus.SUCCESS);
        transaction.setCreatedat(new Date());
        transaction = transactionRepository.save(transaction);

        // Cập nhật trạng thái đơn hàng thành hoàn tất
        order.setStatus(OrderStatus.DA_HOAN_TAT);
        order.setUpdatedat(new Date());
        orderRepository.save(order);

        // Cập nhật số lượng sản phẩm (xe đã bán)
        updateProductAmountAfterOrder(order);

        // Tính hoa hồng 5% và chuyển tiền cho seller
        double commission = order.getTotalamount() * 0.05;
        double sellerRevenue = order.getTotalamount() - commission;

        // Lấy seller từ product
        Order_detail detail = order.getDetails().get(0);
        User seller = detail.getProducts().getUsers();

        // Tạo notification cho buyer
        String message = "Thanh toán thành công " + remainingAmount + " VNĐ cho đơn hàng #" + orderId + ".";
        if (transferOwnership) {
            message += " Xe đã được sang tên.";
        }
        if (changePlate) {
            message += " Biển số đã được đổi.";
        }
        createNotification(buyer, "Thanh toán thành công", message);

        // Tạo notification cho seller
        createNotification(seller, "Đơn hàng hoàn tất",
                "Đơn hàng #" + orderId + " đã hoàn tất. Bạn nhận được " + sellerRevenue
                + " VNĐ (đã trừ 5% hoa hồng = " + commission + " VNĐ).");

        return transaction;
    }

    //Xác nhận nhận hàng cho đơn hàng pin
    @Override
    @Transactional
    public Orders confirmReceipt(Long userId, Long orderId) {
        Orders order = orderRepository.findByIdWithDetails(orderId)
                .orElseThrow(() -> new RuntimeException("Order not found"));

        // Kiểm tra quyền sở hữu
        if (!order.getUsers().getUserid().equals(userId)) {
            throw new RuntimeException("Bạn không có quyền thực hiện hành động này");
        }

        // Kiểm tra là đơn hàng pin
        if (!isBatteryOrder(order)) {
            throw new RuntimeException("Chỉ đơn hàng pin mới có thể xác nhận nhận hàng");
        }

        // Kiểm tra trạng thái - chấp nhận cả DA_THANH_TOAN và DA_GIAO
        if (!OrderStatus.DA_THANH_TOAN.equals(order.getStatus())
                && !OrderStatus.DA_GIAO.equals(order.getStatus())) {
            throw new RuntimeException("Đơn hàng không ở trạng thái có thể xác nhận nhận hàng");
        }

        // Cập nhật trạng thái
        order.setStatus(OrderStatus.DA_HOAN_TAT);
        order.setUpdatedat(new Date());
        orderRepository.save(order);

        // Cập nhật số lượng sản phẩm
        updateProductAmountAfterOrder(order);

        // Tính hoa hồng và chuyển tiền cho seller
        double commission = order.getTotalamount() * 0.05;
        double sellerRevenue = order.getTotalamount() - commission;

        Order_detail detail = order.getDetails().get(0);
        User seller = detail.getProducts().getUsers();

        // Tạo notification
        createNotification(order.getUsers(), "Xác nhận nhận hàng thành công",
                "Đơn hàng #" + orderId + " đã hoàn tất.");

        createNotification(seller, "Đơn hàng hoàn tất",
                "Đơn hàng #" + orderId + " đã hoàn tất. Bạn nhận được " + sellerRevenue
                + " VNĐ (đã trừ 5% hoa hồng = " + commission + " VNĐ).");

        return order;
    }

    //Xác nhận giao hàng (cho cả pin và xe)
    @Override
    @Transactional
    public Orders confirmDelivery(Long userId, Long orderId) {
        Orders order = orderRepository.findByIdWithDetails(orderId)
                .orElseThrow(() -> new RuntimeException("Order not found"));

        // Kiểm tra quyền sở hữu (seller xác nhận giao hàng)
        Order_detail detail = order.getDetails().get(0);
        if (!detail.getProducts().getUsers().getUserid().equals(userId)) {
            throw new RuntimeException("Bạn không có quyền thực hiện hành động này");
        }

        // Kiểm tra trạng thái
        if (!OrderStatus.DA_THANH_TOAN.equals(order.getStatus())) {
            throw new RuntimeException("Đơn hàng chưa được thanh toán");
        }

        // Cập nhật trạng thái
        order.setStatus(OrderStatus.DA_GIAO);
        order.setUpdatedat(new Date());
        orderRepository.save(order);

        // Nếu là xe, cập nhật ngay (không cần buyer xác nhận)
        if (isCarOrder(order)) {
            order.setStatus(OrderStatus.DA_HOAN_TAT);
            orderRepository.save(order);

            // Cập nhật số lượng sản phẩm
            updateProductAmountAfterOrder(order);

            // Tính hoa hồng
            double commission = order.getTotalamount() * 0.05;
            double sellerRevenue = order.getTotalamount() - commission;

            createNotification(order.getUsers(), "Giao hàng thành công",
                    "Đơn hàng #" + orderId + " đã được giao thành công.");

            createNotification(detail.getProducts().getUsers(), "Đơn hàng hoàn tất",
                    "Đơn hàng #" + orderId + " đã hoàn tất. Bạn nhận được " + sellerRevenue
                    + " VNĐ (đã trừ 5% hoa hồng = " + commission + " VNĐ).");
        } else {
            // Pin: chờ buyer xác nhận nhận hàng
            createNotification(order.getUsers(), "Đơn hàng đang giao",
                    "Đơn hàng #" + orderId + " đang được giao. Vui lòng xác nhận khi nhận được hàng.");
        }

        return order;
    }

    // =============== HELPER METHODS ==================================================================================
    /**
     * Cập nhật số lượng sản phẩm sau khi đơn hàng hoàn tất
     */
    private void updateProductAmountAfterOrder(Orders order) {
        for (Order_detail detail : order.getDetails()) {
            Product product = detail.getProducts();
            int orderedQuantity = detail.getQuantity();

            // Trừ số lượng
            int newAmount = product.getAmount() - orderedQuantity;
            if (newAmount < 0) {
                newAmount = 0;
            }
            product.setAmount(newAmount);

            // Nếu hết hàng, chuyển status sang DA_BAN
            if (newAmount == 0) {
                product.setStatus(ProductStatus.DA_BAN);
            }

            product.setUpdatedat(new Date());
            productRepository.save(product);
        }
    }

    @SuppressWarnings("unused")
    private double calculateShippingFee(String shippingAddress) {
        // Logic tính phí ship 30k (tích hợp API giao hàng sau if rảnh)
        return 30000.0; // 30k VNĐ
    }

    private void createNotification(User user, String title, String description) {
        Notification notification = new Notification();
        notification.setTitle(title);
        notification.setDescription(description);
        notification.setCreated_time(new Date());
        notification.setUsers(user);
        notificationRepository.save(notification);
    }
}
