package com.project.tradingev_batter.Service;

import com.project.tradingev_batter.Entity.Address;

import java.util.List;

public interface AddressService{
    List<Address> getAddresses(long userid);
    Address addAddress(long userid, Address address);
    Address updateAddress(long userid, long addressId, Address address) throws Exception;
    void deleteAddress(long userid, long addressId) throws Exception;
}
