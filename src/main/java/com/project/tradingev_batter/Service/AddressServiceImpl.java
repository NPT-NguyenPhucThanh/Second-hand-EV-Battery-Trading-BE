package com.project.tradingev_batter.Service;

import com.project.tradingev_batter.Entity.Address;
import com.project.tradingev_batter.Entity.User;
import com.project.tradingev_batter.Repository.AddressRepository;
import com.project.tradingev_batter.Repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class AddressServiceImpl implements AddressService {

    private final AddressRepository addressRepository;
    private final UserRepository userRepository;

    @Autowired
    public AddressServiceImpl(AddressRepository addressRepository, UserRepository userRepository) {
        this.addressRepository = addressRepository;
        this.userRepository = userRepository;
    }

    @Override
    public List<Address> getAddresses(long userid) {
        return addressRepository.findByUsersUserid(userid);
    }

    @Override
    public Address addAddress(long userid, Address address) {
        User user = userRepository.findByUserid(userid);
        if (user == null) {
            throw new RuntimeException("User not found with ID: " + userid);
        }

        address.setUsers(user);
        return addressRepository.save(address);
    }

    @Override
    public Address updateAddress(long userid, long addressId, Address newAddress) throws Exception {
        User user = userRepository.findByUserid(userid);
        Address existingAddress = addressRepository.findByAddressId(addressId);

        if (user == null) {
            throw new RuntimeException("User not found with ID: " + userid);
        } else if (existingAddress == null) {
            throw new RuntimeException("Address not found with ID: " + addressId);
        } else if (existingAddress.getUsers().getUserid() != userid) {
            throw new RuntimeException("This address does not belong to the user.");
        }

        existingAddress.setStreet(newAddress.getStreet());
        existingAddress.setWard(newAddress.getWard());
        existingAddress.setDistrict(newAddress.getDistrict());
        existingAddress.setProvince(newAddress.getProvince());
        existingAddress.setCountry(newAddress.getCountry());

        return addressRepository.save(existingAddress);
    }

    @Override
    public void deleteAddress(long userid, long addressId) throws Exception {
        User user = userRepository.findByUserid(userid);
        Address existingAddress = addressRepository.findByAddressId(addressId);

        if (user == null) {
            throw new RuntimeException("User not found with ID: " + userid);
        } else if (existingAddress == null) {
            throw new RuntimeException("Address not found with ID: " + addressId);
        } else if (existingAddress.getUsers().getUserid() != userid) {
            throw new RuntimeException("This address does not belong to the user.");
        }

        addressRepository.delete(existingAddress);
    }
}