package com.project.tradingev_batter.Repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import com.project.tradingev_batter.Entity.Role;

@Repository
public interface RoleRepository extends JpaRepository<Role, Long> {
    Role findByRolename(String rolename);
}
