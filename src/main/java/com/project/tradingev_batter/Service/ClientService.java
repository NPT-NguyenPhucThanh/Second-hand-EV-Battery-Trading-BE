package com.project.tradingev_batter.Service;

import com.project.tradingev_batter.Entity.*;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;
import java.util.Optional;

public interface ClientService {
    // Quản lý Profile
    User getProfile(long userid);

    boolean updateProfile(long userid, User updatedUser) throws Exception;

    boolean changePassword(long userid, String oldPassword, String newPassword) throws Exception;

    boolean changeEmail(long userid, String newEmail) throws Exception;

}