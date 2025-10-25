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

    // KHÔNG DÙNG @EntityGraph vì Hibernate không thể fetch multiple bags cùng lúc
    // Thay vào đó dùng @Query với LEFT JOIN FETCH nếu cần eager load
    List<Orders> findByUsersUserid(long userid);

    List<Orders> findByUsersAndStatus(User user, OrderStatus status);
}
