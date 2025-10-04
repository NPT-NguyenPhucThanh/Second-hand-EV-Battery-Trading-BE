package com.project.tradingev_batter.Service;

import com.project.tradingev_batter.Entity.User;

import java.util.List;

public interface UserService {
    List<User> getAllUsers();
    User getUserById(Long id);
    User createUser(User user);
    User updateUser(Long id, User updatedUser);
    void deleteUser(Long id);
}
