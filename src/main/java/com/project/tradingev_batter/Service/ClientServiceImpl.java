package com.project.tradingev_batter.Service;

import com.project.tradingev_batter.Entity.*;
import com.project.tradingev_batter.Repository.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.Date;
import java.util.List;

@Service
public class ClientServiceImpl implements ClientService {

    private final UserRepository userRepository;
    private final ImageUploadService imageUploadService;
    private final NotificationRepository notificationRepository;
    private final PasswordEncoder passwordEncoder;

    @Autowired
    public ClientServiceImpl(UserRepository userRepository,
                             ImageUploadService imageUploadService,
                             NotificationRepository notificationRepository,
                             PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.imageUploadService = imageUploadService;
        this.notificationRepository = notificationRepository;
        this.passwordEncoder = passwordEncoder;
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
            existingUser.setUpdated_at(new Date());

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
            // Kiểm tra mật khẩu cũ với passwordEncoder
            if (passwordEncoder.matches(oldPassword, user.getPassword())) {
                user.setPassword(passwordEncoder.encode(newPassword));
                user.setUpdated_at(new Date());
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
            user.setUpdated_at(new Date());
            userRepository.save(user);
            return true;
        }
        return false;
    }


     //Client gửi yêu cầu nâng cấp lên Seller
    @Override
    @Transactional
    public User requestSellerUpgrade(long userid, MultipartFile cccdFront, MultipartFile cccdBack, 
                                    MultipartFile vehicleRegistration) throws Exception {
        User user = userRepository.findByUserid(userid);
        if (user == null) {
            throw new Exception("User not found");
        }

        // Kiểm tra đã gửi yêu cầu chưa
        if ("PENDING".equals(user.getSellerUpgradeStatus())) {
            throw new Exception("Bạn đã gửi yêu cầu nâng cấp rồi. Vui lòng chờ xét duyệt.");
        }

        if ("APPROVED".equals(user.getSellerUpgradeStatus())) {
            throw new Exception("Bạn đã là Seller rồi");
        }

        // Upload CCCD
        try {
            String cccdFrontUrl = imageUploadService.uploadImage(cccdFront, "user_documents/" + userid + "/cccd");
            String cccdBackUrl = imageUploadService.uploadImage(cccdBack, "user_documents/" + userid + "/cccd");
            
            user.setCccdFrontUrl(cccdFrontUrl);
            user.setCccdBackUrl(cccdBackUrl);

            // Upload giấy tờ xe nếu có
            if (vehicleRegistration != null && !vehicleRegistration.isEmpty()) {
                String vehicleUrl = imageUploadService.uploadImage(vehicleRegistration, "user_documents/" + userid + "/vehicle");
                user.setVehicleRegistrationUrl(vehicleUrl);
            }

            user.setSellerUpgradeStatus("PENDING");
            user.setSellerUpgradeRequestDate(new Date());
            user.setRejectionReason(null); // Clear rejection reason nếu có
            user.setUpdated_at(new Date());

            userRepository.save(user);

            // Tạo notification cho user
            createNotification(user, "Yêu cầu nâng cấp đã được gửi", 
                    "Yêu cầu nâng cấp lên Seller của bạn đã được gửi. Vui lòng chờ manager xét duyệt.");

            // Tạo notification cho tất cả manager
            createNotificationForAllManagers(
                    "Yêu cầu nâng cấp Seller mới",
                    "User " + user.getUsername() + " (ID: " + user.getUserid() + ") đã gửi yêu cầu nâng cấp lên Seller. Vui lòng xem xét.");

            return user;
        } catch (IOException e) {
            throw new Exception("Upload file thất bại: " + e.getMessage());
        }
    }


    //Gửi lại yêu cầu nếu bị từ chối
    @Override
    @Transactional
    public User resubmitSellerUpgrade(long userid, MultipartFile cccdFront, MultipartFile cccdBack, 
                                      MultipartFile vehicleRegistration) throws Exception {
        User user = userRepository.findByUserid(userid);
        if (user == null) {
            throw new Exception("User not found");
        }

        // Chỉ cho phép resubmit nếu bị từ chối
        if (!"REJECTED".equals(user.getSellerUpgradeStatus())) {
            throw new Exception("Chỉ có thể gửi lại khi yêu cầu bị từ chối");
        }

        // Upload lại files
        try {
            String cccdFrontUrl = imageUploadService.uploadImage(cccdFront, "user_documents/" + userid + "/cccd");
            String cccdBackUrl = imageUploadService.uploadImage(cccdBack, "user_documents/" + userid + "/cccd");
            
            user.setCccdFrontUrl(cccdFrontUrl);
            user.setCccdBackUrl(cccdBackUrl);

            if (vehicleRegistration != null && !vehicleRegistration.isEmpty()) {
                String vehicleUrl = imageUploadService.uploadImage(vehicleRegistration, "user_documents/" + userid + "/vehicle");
                user.setVehicleRegistrationUrl(vehicleUrl);
            }

            user.setSellerUpgradeStatus("PENDING");
            user.setSellerUpgradeRequestDate(new Date());
            user.setRejectionReason(null);
            user.setUpdated_at(new Date());

            userRepository.save(user);

            createNotification(user, "Yêu cầu nâng cấp đã được gửi lại", 
                    "Yêu cầu nâng cấp lên Seller của bạn đã được gửi lại. Vui lòng chờ manager xét duyệt.");

            return user;
        } catch (IOException e) {
            throw new Exception("Upload file thất bại: " + e.getMessage());
        }
    }

    private void createNotification(User user, String title, String description) {
        Notification notification = new Notification();
        notification.setTitle(title);
        notification.setDescription(description);
        notification.setCreated_time(new Date());
        notification.setUsers(user);
        notificationRepository.save(notification);
    }

    private void createNotificationForAllManagers(String title, String description) {
        // lấy danh sách manager, lấy từ database hoặc service
        List<User> managers = userRepository.findByRole("ROLE_MANAGER");

        for (User manager : managers) {
            Notification notification = new Notification();
            notification.setTitle(title);
            notification.setDescription(description);
            notification.setCreated_time(new Date());
            notification.setUsers(manager);
            notificationRepository.save(notification);
        }
    }
}
