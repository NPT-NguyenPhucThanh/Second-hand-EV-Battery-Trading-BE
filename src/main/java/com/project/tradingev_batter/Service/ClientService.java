package com.project.tradingev_batter.Service;

import com.project.tradingev_batter.Entity.User;
import org.springframework.web.multipart.MultipartFile;

public interface ClientService {
    // Quản lý Profile
    User getProfile(long userid);
    boolean updateProfile(long userid, User updatedUser) throws Exception;
    boolean changePassword(long userid, String oldPassword, String newPassword) throws Exception;
    boolean changeEmail(long userid, String newEmail) throws Exception;

    // Seller Upgrade Request
    User requestSellerUpgrade(long userid, MultipartFile cccdFront, MultipartFile cccdBack, MultipartFile vehicleRegistration) throws Exception;
    User resubmitSellerUpgrade(long userid, MultipartFile cccdFront, MultipartFile cccdBack, MultipartFile vehicleRegistration) throws Exception;
}
