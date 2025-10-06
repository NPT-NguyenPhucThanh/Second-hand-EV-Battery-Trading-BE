package com.project.tradingev_batter;

import com.project.tradingev_batter.Entity.*;
import com.project.tradingev_batter.Repository.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.security.crypto.password.PasswordEncoder;

import jakarta.annotation.PostConstruct;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashSet;
import java.util.Set;

@SpringBootApplication
public class TradingevBatterApplication {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private RoleRepository roleRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private OrderRepository orderRepository;

    @Autowired
    private ProductRepository productRepository;

    @Autowired
    private PostRepository postRepository;

    @Autowired
    private FeedbackRepository feedbackRepository;

    @Autowired
    private NotificationRepository notificationRepository;

    @Autowired
    private AddressRepository addressRepository;

    @Autowired
    private BrandBatteryRepository brandbatteryRepository;

    @Autowired
    private BrandcarsRepository brandcarsRepository;

    @Autowired
    private CartsRepository cartsRepository;

    @Autowired
    private CartItemRepository cartItemRepository;

    @Autowired
    private ChatroomRepository chatroomRepository;

    @Autowired
    private CommentRepository commentRepository;

    @Autowired
    private ContractsRepository contractsRepository;

    @Autowired
    private DisputeRepository disputeRepository;

    @Autowired
    private FavoritePostRepository favoritePostRepository;

    @Autowired
    private MessageRepository messageRepository;

    @Autowired
    private OrderDetailRepository orderDetailRepository;

    @Autowired
    private PackageServiceRepository packageServiceRepository;

    @Autowired
    private ProductImgRepository productImgRepository;

    @Autowired
    private RefundRepository refundRepository;

    @Autowired
    private TransactionRepository transactionRepository;

    @Autowired
    private UserPackageRepository userPackageRepository;

    public static void main(String[] args) {
        SpringApplication.run(TradingevBatterApplication.class, args);
    }

    @PostConstruct
    public void seedData() {
        // Seed roles
        Role clientRole = new Role();
        clientRole.setRolename("CLIENT");
        clientRole.setJoindate(new Date());
        roleRepository.save(clientRole);

        Role sellerRole = new Role();
        sellerRole.setRolename("SELLER");
        clientRole.setJoindate(new Date());
        roleRepository.save(sellerRole);

        Role managerRole = new Role();
        managerRole.setRolename("MANAGER");
        clientRole.setJoindate(new Date());
        roleRepository.save(managerRole);

        // Seed user client
        User client = new User();
        client.setUsername("client");
        client.setPassword(passwordEncoder.encode("pass"));
        client.setEmail("client@email.com");
        client.setCreated_at(new Date());
        client.setIsactive(true);
        Set<Role> clientRoles = new HashSet<>();
        clientRoles.add(clientRole);
        client.setRoles(new ArrayList<>(clientRoles));
        client = userRepository.save(client);

        // Seed user seller
        User seller = new User();
        seller.setUsername("seller");
        seller.setPassword(passwordEncoder.encode("pass"));
        seller.setEmail("seller@email.com");
        seller.setCreated_at(new Date());
        seller.setIsactive(true);
        Set<Role> sellerRoles = new HashSet<>();
        sellerRoles.add(sellerRole);
        seller.setRoles(new ArrayList<>(sellerRoles));
        seller = userRepository.save(seller);

        // Seed user manager
        User manager = new User();
        manager.setUsername("manager");
        manager.setPassword(passwordEncoder.encode("pass"));
        manager.setEmail("manager@email.com");
        manager.setCreated_at(new Date());
        manager.setIsactive(true);
        Set<Role> managerRoles = new HashSet<>();
        managerRoles.add(managerRole);
        manager.setRoles(new ArrayList<>(managerRoles));
        manager = userRepository.save(manager);

        // Seed address
        Address address = new Address();
        address.setStreet("123 Street");
        address.setWard("Ward 1");
        address.setDistrict("District 1");
        address.setProvince("HCMC");
        address.setCountry("Vietnam");
        address.setUsers(client);
        addressRepository.save(address);

        // Seed brandbattery
        Brandbattery brandbattery = new Brandbattery();
        brandbattery.setYear(2020);
        brandbattery.setCapacity(100.0);
        brandbattery.setRemaining(80.0);
        brandbatteryRepository.save(brandbattery);

        // Seed brandcars
        Brandcars brandcars = new Brandcars();
        brandcars.setYear(2019);
        brandcars.setOdo(50000.0);
        brandcars.setCapacity(200.0);
        brandcars.setColor("Red");
        brandcarsRepository.save(brandcars);

        // Seed product (link brand, seller)
        Product product = new Product();
        product.setProductname("Car EV");
        product.setDescription("Car cũ tốt");
        product.setCost(5000000.0);
        product.setAmount(5);
        product.setStatus("DA_DUYET");
        product.setModel("Model1");
        product.setType("Car EV");
        product.setSpecs("Specs here");
        product.setCreatedat(new Date());
        product.setUpdatedat(new Date());
        product.setUsers(seller);
        product.setBrandbattery(brandbattery);
        product = productRepository.save(product);
        System.out.println("Seeded Product ID: " + product.getProductid());

        // Seed post (link product)
        Post post = new Post();
        post.setTitle("Bán pin EV");
        post.setDescription("Post desc");
        post.setStatus("DA_DUYET");
        post.setCreated_at(new Date());
        post.setUpdated_at(new Date());
        post.setProducts(product);
        post.setUsers(seller);
        postRepository.save(post);

        // Seed feedback (link product/order)
        Feedback feedback = new Feedback();
        feedback.setRating(5);
        feedback.setComment("Tốt");
        feedback.setCreated_at(new Date());
        feedback.setProducts(product);
        feedback.setUsers(client);
        feedbackRepository.save(feedback);

        // Seed notification (link user)
        Notification notification = new Notification();
        notification.setTitle("Thông báo mới");
        notification.setDescription("Desc");
        notification.setCreated_time(new Date());
        notification.setUsers(manager);
        notificationRepository.save(notification);

        // Seed orders (đã có, link buyer)
        Orders order = new Orders();
        order.setTotalamount(500000.0);
        order.setShippingfee(50000.0);
        order.setTotalfinal(550000.0);
        order.setShippingaddress("123 Street");
        order.setPaymentmethod("VnPay");
        order.setCreatedat(new Date());
        order.setUpdatedat(new Date());
        order.setStatus("CHO_DUYET");
        order.setUsers(client);
        order.setAddress(address);
        order = orderRepository.save(order);
        System.out.println("Seeded Orders ID: " + order.getOrderid());

        // Seed order_detail (link order/product)
        Order_detail orderDetail = new Order_detail();
        orderDetail.setQuantity(1);
        orderDetail.setUnit_price(500000.0);
        orderDetail.setOrders(order);
        orderDetail.setProducts(product);
        orderDetailRepository.save(orderDetail);

        // Seed carts (link user)
        Carts cart = new Carts();
        cart.setCreatedat(new Date());
        cart.setUpdatedat(new Date());
        cart.setUsers(client);
        cartsRepository.save(cart);

        // Seed cart_item (link cart/product)
        cart_items cartItem = new cart_items();
        cartItem.setQuantity(2);
        cartItem.setAddedat(new Date());
        cartItem.setCarts(cart);
        cartItem.setProducts(product);
        cartItem.setUsers(client);
        cartItemRepository.save(cartItem);

        // Seed chatroom (link buyer/seller/order)
        Chatroom chatroom = new Chatroom();
        chatroom.setCreatedat(new Date());
        chatroom.setBuyer(client);
        chatroom.setSeller(seller);
        chatroom.setOrders(order);
        chatroomRepository.save(chatroom);

        // Seed message (link chatroom)
        Message message = new Message();
        message.setText("Hello");
        message.setCreatedat(new Date());
        message.setChatroom(chatroom);
        message.setBuyerid(client);
        messageRepository.save(message);

        // Seed comment (link post/user)
        Comment comment = new Comment();
        comment.setText("Comment here");
        comment.setCreated_at(new Date());
        comment.setPosts(post);
        comment.setUsers(client);
        commentRepository.save(comment);

        // Seed contracts (link order/buyer/seller/admin)
        Contracts contract = new Contracts();
        contract.setSignedat(new Date());
        contract.setStatus(true);
        contract.setOrders(order);
        contract.setBuyers(client);
        contract.setSellers(seller);
        contract.setAdmins(manager);
        contractsRepository.save(contract);

        // Seed dispute (link order/manager)
        Dispute dispute = new Dispute();
        dispute.setDescription("Tranh chấp");
        dispute.setStatus("OPEN");
        dispute.setCreatedAt((new Date()));
        dispute.setOrder(order);
        //dispute.setResolved_by(manager);
        disputeRepository.save(dispute);

        // Seed favorite_post (link user/post)
        Favorite_post favorite = new Favorite_post();
        favorite.setCreated_at(new Date());
        favorite.setUsers(client);
        favorite.setPosts(post);
        favoritePostRepository.save(favorite);

        // Seed product_img (link product)
        product_img img = new product_img();
        img.setUrl("img	url.com");
        img.setProducts(product);
        productImgRepository.save(img);

        // Seed refund (link order)
        Refund refund = new Refund();
        refund.setAmount(100000.0);
        refund.setReason("Hoàn tiền");
        refund.setStatus("PENDING");
        refund.setCreatedat(new Date());
        refund.setOrders(order);
        refundRepository.save(refund);

        // Seed transaction (link order/user)
        Transaction transaction = new Transaction();
        transaction.setMethod("VnPay");
        transaction.setStatus("COMPLETED");
        transaction.setCreatedat(new Date());
        transaction.setOrders(order);
        transaction.setCreatedBy(client);
        transactionRepository.save(transaction);

        // Seed package_service
        PackageService pkg = new PackageService();
        pkg.setName("Cơ bản");
        pkg.setDurationMonths(1);
        pkg.setPrice(100000.0);
        pkg.setMaxCars(1);
        pkg.setMaxBatteries(2);
        pkg.setCreatedAt(new Date());
        packageServiceRepository.save(pkg);

        // Seed user_package (link user/package)
        UserPackage userPkg = new UserPackage();
        userPkg.setUser(seller);
        userPkg.setPackageService(pkg);
        userPkg.setPurchaseDate(new Date());
        userPkg.setExpiryDate(new Date(System.currentTimeMillis() + 2592000000L)); // 1 tháng
        userPkg.setRemainingCars(1);
        userPkg.setRemainingBatteries(2);
        userPackageRepository.save(userPkg);

        System.out.println("All data seeded successfully!");
    }
}