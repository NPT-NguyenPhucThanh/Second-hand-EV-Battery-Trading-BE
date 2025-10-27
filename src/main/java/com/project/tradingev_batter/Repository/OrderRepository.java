package com.project.tradingev_batter.Repository;

import com.project.tradingev_batter.Entity.User;
import com.project.tradingev_batter.enums.OrderStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import com.project.tradingev_batter.Entity.Orders;

import java.util.List;

@Repository
public interface OrderRepository extends JpaRepository<Orders,Long> {
    @Query("SELECT SUM(o.totalfinal) FROM Orders o WHERE o.status = 'DA_HOAN_TAT'")
    double getTotalSales();
    @Query("SELECT FUNCTION('MONTH', o.createdat), COUNT(o) FROM Orders o GROUP BY FUNCTION('MONTH', o.createdat)")
    List<Object[]> getOrdersByMonth();

    // Query với LEFT JOIN FETCH để tránh LazyInitializationException
    @Query("SELECT DISTINCT o FROM Orders o " +
           "LEFT JOIN FETCH o.details " +
           "WHERE o.users.userid = :userid " +
           "ORDER BY o.createdat DESC")
    List<Orders> findByUsersUserid(long userid);

    @Query("SELECT DISTINCT o FROM Orders o " +
           "LEFT JOIN FETCH o.details " +
           "WHERE o.users.userid = :userid " +
           "AND o.status = :status")
    List<Orders> findByUsersAndStatus(long userid, OrderStatus status);

    // Query lấy tất cả orders với details để tránh LazyInitializationException
    @Query("SELECT DISTINCT o FROM Orders o LEFT JOIN FETCH o.details")
    List<Orders> findAllWithDetails();

    // Query lấy order by ID với details để tránh LazyInitializationException
    // Không fetch contracts cùng lúc để tránh MultipleBagFetchException
    @Query("SELECT DISTINCT o FROM Orders o " +
           "LEFT JOIN FETCH o.details d " +
           "LEFT JOIN FETCH d.products p " +
           "LEFT JOIN FETCH p.users " +
           "WHERE o.orderid = :orderId")
    java.util.Optional<Orders> findByIdWithDetails(Long orderId);
}
