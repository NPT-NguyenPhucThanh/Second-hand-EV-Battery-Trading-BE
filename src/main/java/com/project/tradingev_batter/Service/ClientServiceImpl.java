package com.project.tradingev_batter.Service;

import com.project.tradingev_batter.Entity.*;
import com.project.tradingev_batter.Repository.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Date;
import java.util.List;
import java.util.Optional;

@Service
public class ClientServiceImpl implements ClientService {

    private final UserRepository userRepository;
    private final AddressRepository addressRepository;
    private final ProductRepository productRepository;
    private final CartItemRepository cartItemsRepository;
    private final OrderRepository ordersRepository;
    private final TransactionRepository transactionRepository;
    private final ContractsRepository contractsRepository;
    private final PostRepository postRepository;
    private final NotificationRepository notificationRepository;

    @Autowired
    public ClientServiceImpl(UserRepository userRepository,
                             AddressRepository addressRepository,
                             ProductRepository productRepository,
                             CartItemRepository cartItemsRepository,
                             OrderRepository ordersRepository,
                             TransactionRepository transactionRepository,
                             ContractsRepository contractsRepository,
                             PostRepository postRepository,
                             NotificationRepository notificationRepository) {
        this.userRepository = userRepository;
        this.addressRepository = addressRepository;
        this.productRepository = productRepository;
        this.cartItemsRepository = cartItemsRepository;
        this.ordersRepository = ordersRepository;
        this.transactionRepository = transactionRepository;
        this.contractsRepository = contractsRepository;
        this.postRepository = postRepository;
        this.notificationRepository = notificationRepository;
    }

    @Override
    public User getProfile(long userid) {
        return userRepository.findByUserid(userid);
    }

    @Override
    @Transactional
    public boolean updateProfile(long userid, User updatedUser) {
        User existingUser = userRepository.findByUserid(userid);
        if (existingUser != null) {
            existingUser.setUsername(updatedUser.getUsername());
            existingUser.setPhone(updatedUser.getPhone());
            existingUser.setDisplayname(updatedUser.getDisplayname());
            existingUser.setDateofbirth(updatedUser.getDateofbirth());

            userRepository.save(existingUser);
            return true;
        }
        return false;
    }

    @Override
    @Transactional
    public boolean changePassword(long userid, String oldPassword, String newPassword) {
        User user = userRepository.findByUserid(userid);
        if (user != null) {
            if (user.getPassword().equals(oldPassword)) {
                user.setPassword(newPassword);
                userRepository.save(user);
                return true;
            }
        }
        return false;
    }


    @Override
    @Transactional
    public boolean changeEmail(long userid, String newEmail) {
        User user = userRepository.findByUserid(userid);
        if (user != null) {
            user.setEmail(newEmail);
            userRepository.save(user);
            return true;
        }
        return false;
    }
}