package com.project.tradingev_batter;

import com.project.tradingev_batter.Entity.Role;
import com.project.tradingev_batter.Entity.User;
import com.project.tradingev_batter.Repository.RoleRepository;
import com.project.tradingev_batter.Repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.security.crypto.password.PasswordEncoder;
import com.project.tradingev_batter.Entity.Orders;
import com.project.tradingev_batter.Repository.OrderRepository;

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
        sellerRole.setJoindate(new Date());
        roleRepository.save(sellerRole);

        Role managerRole = new Role();
        managerRole.setRolename("MANAGER");
        managerRole.setJoindate(new Date());
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
        userRepository.save(manager);

        // Seed sample order (ID=1)
        Orders order = new Orders();
        // Không set orderid (IDENTITY tự assign =1)
        order.setTotalamount(500000.0);
        order.setShippingfee(50000.0);
        order.setTotalfinal(550000.0);   // Tổng cuối = totalamount + shippingfee
        order.setShippingaddress("12A9 Master, Quận 2, TP.HCM");  // Địa chỉ ship
        order.setPaymentmethod("CASH");   // Phương thức thanh toán
        Date now = new Date();
        order.setCreatedat(now);
        order.setUpdatedat(now);
        order.setStatus("CHO_DUYET");
        order.setUsers(client);           // Link buyer với client user (FK buyerid)
        order.setAddress(null);           // Optional, set null trước
        // Các List one-to-many đã init empty trong @Data
        orderRepository.save(order);
        System.out.println("Data seeded successfully! Order ID: " + order.getOrderid());

        System.out.println("Data seeded successfully!");
    }
}