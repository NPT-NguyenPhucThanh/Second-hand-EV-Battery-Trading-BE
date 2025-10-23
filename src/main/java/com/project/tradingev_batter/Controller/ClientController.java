package com.project.tradingev_batter.Controller;

import com.project.tradingev_batter.Entity.User;
import com.project.tradingev_batter.Service.ClientService;
import com.project.tradingev_batter.security.CustomUserDetails;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.HashMap;
import java.util.Map;


//ClientController - Xử lý các request từ Client
//Quản lý profile, Seller Upgrade Request
@RestController
@RequestMapping("/api/client")
@Tag(name = "Client APIs", description = "API công khai không cần đăng nhập - Xem gói dịch vụ")
public class ClientController {

    private final ClientService clientService;

    public ClientController(ClientService clientService) {
        this.clientService = clientService;
    }


     //Xem profile
    @Operation(summary = "Xem thông tin profile", description = "Lấy thông tin chi tiết của người dùng đang đăng nhập")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Thành công - Trả về thông tin profile"),
            @ApiResponse(responseCode = "401", description = "Chưa đăng nhập"),
            @ApiResponse(responseCode = "500", description = "Lỗi server")
    })
    @GetMapping("/profile")
    public ResponseEntity<Map<String, Object>> getProfile() {
        User user = getCurrentUser();
        User profile = clientService.getProfile(user.getUserid());
        
        Map<String, Object> response = new HashMap<>();
        response.put("status", "success");
        response.put("profile", profile);
        
        return ResponseEntity.ok(response);
    }

    //Cập nhật profile
    @Operation(summary = "Cập nhật thông tin profile", description = "Cập nhật thông tin người dùng")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Thành công - Cập nhật thông tin profile"),
            @ApiResponse(responseCode = "400", description = "Lỗi dữ liệu đầu vào"),
            @ApiResponse(responseCode = "401", description = "Chưa đăng nhập"),
            @ApiResponse(responseCode = "500", description = "Lỗi server")
    })
    @PutMapping("/profile")
    public ResponseEntity<Map<String, Object>> updateProfile(@RequestBody User updatedUser) {
        User user = getCurrentUser();

        Map<String, Object> response = new HashMap<>();
        try {
            boolean success = clientService.updateProfile(user.getUserid(), updatedUser);

            if (success) {
                response.put("status", "success");
                response.put("message", "Cập nhật profile thành công");
            } else {
                response.put("status", "error");
                response.put("message", "Cập nhật profile thất bại");
            }
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            response.put("status", "error");
            response.put("message", "Lỗi khi cập nhật profile: " + e.getMessage());
            return ResponseEntity.badRequest().body(response);
        }
    }

    //Đổi mật khẩu
    @Operation(summary = "Đổi mật khẩu", description = "Thay đổi mật khẩu cho tài khoản người dùng")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Thành công - Đổi mật khẩu"),
            @ApiResponse(responseCode = "400", description = "Lỗi dữ liệu đầu vào"),
            @ApiResponse(responseCode = "401", description = "Chưa đăng nhập"),
            @ApiResponse(responseCode = "500", description = "Lỗi server")
    })
    @PostMapping("/change-password")
    public ResponseEntity<Map<String, Object>> changePassword(
            @RequestParam String oldPassword,
            @RequestParam String newPassword) {
        
        User user = getCurrentUser();

        Map<String, Object> response = new HashMap<>();
        try {
            boolean success = clientService.changePassword(user.getUserid(), oldPassword, newPassword);

            if (success) {
                response.put("status", "success");
                response.put("message", "Đổi mật khẩu thành công");
            } else {
                response.put("status", "error");
                response.put("message", "Mật khẩu cũ không đúng");
            }
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            response.put("status", "error");
            response.put("message", "Lỗi khi đổi mật khẩu: " + e.getMessage());
            return ResponseEntity.badRequest().body(response);
        }
    }

    //Gửi yêu cầu nâng cấp từ Client lên Seller
    @Operation(summary = "Gửi yêu cầu nâng cấp lên Seller", description = "Gửi yêu cầu nâng cấp tài khoản từ Client lên Seller kèm theo tài liệu cần thiết")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Thành công - Yêu cầu nâng cấp đã được gửi"),
            @ApiResponse(responseCode = "400", description = "Lỗi dữ liệu đầu vào"),
            @ApiResponse(responseCode = "401", description = "Chưa đăng nhập"),
            @ApiResponse(responseCode = "500", description = "Lỗi server")
    })
    @PostMapping(value = "/seller-upgrade/request", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<Map<String, Object>> requestSellerUpgrade(
            @RequestParam("cccdFront") MultipartFile cccdFront,
            @RequestParam("cccdBack") MultipartFile cccdBack,
            @RequestParam(value = "vehicleRegistration", required = false) MultipartFile vehicleRegistration) {
        
        User user = getCurrentUser();
        
        try {
            User updatedUser = clientService.requestSellerUpgrade(
                    user.getUserid(), 
                    cccdFront, 
                    cccdBack, 
                    vehicleRegistration
            );
            
            Map<String, Object> response = new HashMap<>();
            response.put("status", "success");
            response.put("message", "Yêu cầu nâng cấp đã được gửi. Vui lòng chờ manager xét duyệt.");
            response.put("upgradeStatus", updatedUser.getSellerUpgradeStatus());
            response.put("requestDate", updatedUser.getSellerUpgradeRequestDate());
            
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("status", "error");
            errorResponse.put("message", e.getMessage());
            return ResponseEntity.badRequest().body(errorResponse);
        }
    }

    //Theo dõi tiến trình xét duyệt bởi Manager
    @Operation(summary = "Theo dõi tiến trình xét duyệt nâng cấp", description = "Lấy thông tin về tiến trình xét duyệt yêu cầu nâng cấp tài khoản")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Thành công - Trả về thông tin tiến trình xét duyệt"),
            @ApiResponse(responseCode = "401", description = "Chưa đăng nhập"),
            @ApiResponse(responseCode = "500", description = "Lỗi server")
    })
    @GetMapping("/seller-upgrade/status")
    public ResponseEntity<Map<String, Object>> getSellerUpgradeStatus() {
        User user = getCurrentUser();
        User currentUser = clientService.getProfile(user.getUserid());
        
        Map<String, Object> response = new HashMap<>();
        response.put("status", "success");
        
        if (currentUser.getSellerUpgradeStatus() == null) {
            response.put("upgradeStatus", "NOT_REQUESTED");
            response.put("message", "Bạn chưa gửi yêu cầu nâng cấp");
        } else {
            response.put("upgradeStatus", currentUser.getSellerUpgradeStatus());
            response.put("requestDate", currentUser.getSellerUpgradeRequestDate());
            
            switch (currentUser.getSellerUpgradeStatus()) {
                case "PENDING":
                    response.put("message", "Yêu cầu của bạn đang được xét duyệt");
                    break;
                case "APPROVED":
                    response.put("message", "Yêu cầu đã được chấp nhận. Bạn giờ là Seller!");
                    break;
                case "REJECTED":
                    response.put("message", "Yêu cầu bị từ chối");
                    response.put("rejectionReason", currentUser.getRejectionReason());
                    break;
            }
        }
        
        return ResponseEntity.ok(response);
    }

    //Gửi lại yêu cầu nếu bị từ chối
    @Operation(summary = "Gửi lại yêu cầu nâng cấp", description = "Gửi lại yêu cầu nâng cấp tài khoản nếu bị từ chối trước đó")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Thành công - Yêu cầu đã được gửi lại"),
            @ApiResponse(responseCode = "400", description = "Lỗi dữ liệu đầu vào"),
            @ApiResponse(responseCode = "401", description = "Chưa đăng nhập"),
            @ApiResponse(responseCode = "500", description = "Lỗi server")
    })
    @PostMapping(value = "/seller-upgrade/resubmit", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<Map<String, Object>> resubmitSellerUpgrade(
            @RequestParam("cccdFront") MultipartFile cccdFront,
            @RequestParam("cccdBack") MultipartFile cccdBack,
            @RequestParam(value = "vehicleRegistration", required = false) MultipartFile vehicleRegistration) {
        
        User user = getCurrentUser();
        
        try {
            User updatedUser = clientService.resubmitSellerUpgrade(
                    user.getUserid(), 
                    cccdFront, 
                    cccdBack, 
                    vehicleRegistration
            );
            
            Map<String, Object> response = new HashMap<>();
            response.put("status", "success");
            response.put("message", "Yêu cầu đã được gửi lại. Vui lòng chờ manager xét duyệt.");
            response.put("upgradeStatus", updatedUser.getSellerUpgradeStatus());
            
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("status", "error");
            errorResponse.put("message", e.getMessage());
            return ResponseEntity.badRequest().body(errorResponse);
        }
    }

    // =============== HELPER METHODS ==================================================================================
    
    private User getCurrentUser() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !(auth.getPrincipal() instanceof CustomUserDetails userDetails)) {
            throw new RuntimeException("User not authenticated");
        }
        return userDetails.getUser();
    }
}
