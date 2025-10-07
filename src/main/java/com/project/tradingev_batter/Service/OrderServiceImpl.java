package com.project.tradingev_batter.Service;

import com.project.tradingev_batter.Entity.Order_detail;
import com.project.tradingev_batter.Entity.Orders;
import com.project.tradingev_batter.Entity.User;
import com.project.tradingev_batter.Repository.OrderDetailRepository;
import com.project.tradingev_batter.Repository.OrderRepository;
import com.project.tradingev_batter.Repository.UserRepository;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class OrderServiceImpl implements OrderService {
    private final OrderRepository orderRepository;
    private final UserRepository userRepository;
    private final OrderDetailRepository order_detailRepository;

    public  OrderServiceImpl(OrderRepository orderRepository,
                             UserRepository userRepository,
                             OrderDetailRepository order_detailRepository) {
        this.orderRepository = orderRepository;
        this.userRepository = userRepository;
        this.order_detailRepository = order_detailRepository;
    }

    public List<Orders> getOrders(long userid){
        return orderRepository.findByUsersUserid(userid);
    }

    @Override
    public List<Order_detail> getOrderDetails(long orderId, long userid) throws Exception {
        Orders order = orderRepository.findById(orderId)
                .orElseThrow(() -> new Exception("Order not found with ID: " + orderId));

        if (order.getUsers().getUserid() != userid) {
            throw new Exception("This order does not belong to the user.");
        }
        return order_detailRepository.findByOrders(order);
    }
}