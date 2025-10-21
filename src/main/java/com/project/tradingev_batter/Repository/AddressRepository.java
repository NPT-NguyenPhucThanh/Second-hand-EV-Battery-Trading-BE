package com.project.tradingev_batter.Repository;

import com.project.tradingev_batter.Entity.Address;
import com.project.tradingev_batter.Entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;


@Repository
public interface AddressRepository extends JpaRepository<Address,Long> {
    List<Address> findByUsersUserid(long userid);

    Address findByAddressid(long addressid);

    // Tìm address theo user và street (Seeds)
    List<Address> findByUsersAndStreet(User user, String street);
}
