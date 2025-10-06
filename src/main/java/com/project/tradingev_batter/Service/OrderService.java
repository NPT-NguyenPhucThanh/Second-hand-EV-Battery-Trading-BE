package com.project.tradingev_batter.Service;

import com.project.tradingev_batter.Entity.Order_detail;
import com.project.tradingev_batter.Entity.Orders;

import java.util.List;

public interface OrderService {
    List<Orders> getOrders(long userid);
    List<Order_detail> getOrderDetails(long orderId, long userid) throws Exception;
}