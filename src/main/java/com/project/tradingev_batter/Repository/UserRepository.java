package com.project.tradingev_batter.Repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import com.project.tradingev_batter.Entity.User;

import java.util.List;

@Repository
public interface UserRepository extends JpaRepository<User, Long> {
    User findByUsername(String username);
    
    @Query("SELECT u FROM User u LEFT JOIN FETCH u.roles WHERE u.username = :username")
    User findByUsernameWithRoles(String username);
    
    User findByEmail(String email);
    User findByUserid(long userid);
    
    // Seller Upgrade Request
    List<User> findBySellerUpgradeStatus(String status);

    // Tìm user theo role
    @Query("SELECT u FROM User u JOIN u.roles r WHERE r.rolename = :roleName")
    List<User> findByRole(String roleName);

    // Get all users with roles eagerly loaded
    @Query("SELECT DISTINCT u FROM User u LEFT JOIN FETCH u.roles")
    List<User> findAllWithRoles();

    // Get user by ID with roles eagerly loaded
    @Query("SELECT u FROM User u LEFT JOIN FETCH u.roles WHERE u.userid = :id")
    User findByIdWithRoles(Long id);
}
