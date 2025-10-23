package com.project.tradingev_batter.Controller;

import com.project.tradingev_batter.Entity.Role;
import com.project.tradingev_batter.Entity.User;
import com.project.tradingev_batter.Repository.RoleRepository;
import com.project.tradingev_batter.Repository.UserRepository;
import com.project.tradingev_batter.dto.LoginRequest;
import com.project.tradingev_batter.dto.RegisterRequest;
import com.project.tradingev_batter.security.CustomUserDetails;
import com.project.tradingev_batter.security.JwtService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/auth")
@Tag(name = "Authentication APIs", description = "API xác thực - Đăng ký, đăng nhập, quản lý tài khoản")
public class AuthController {

    private final AuthenticationManager authenticationManager;
    private final JwtService jwtService;
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final RoleRepository roleRepository;

    public AuthController(AuthenticationManager authenticationManager,
                         JwtService jwtService,
                         UserRepository userRepository,
                         PasswordEncoder passwordEncoder,
                         RoleRepository roleRepository) {
        this.authenticationManager = authenticationManager;
        this.jwtService = jwtService;
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.roleRepository = roleRepository;
    }

    @Operation(
            summary = "Đăng ký tài khoản mới",
            description = "Tạo tài khoản người dùng mới với role mặc định là BUYER. Username và email phải là duy nhất."
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Đăng ký thành công - Trả về thông tin user"),
            @ApiResponse(responseCode = "400", description = "Username hoặc email đã tồn tại"),
            @ApiResponse(responseCode = "500", description = "Lỗi server")
    })
    @PostMapping("/register")
    public ResponseEntity<Map<String, Object>> register(@Valid @RequestBody RegisterRequest request) {
        if (userRepository.findByUsername(request.getUsername()) != null) {
            Map<String, Object> response = new HashMap<>();
            response.put("status", "error");
            response.put("message", "Username exists");
            return ResponseEntity.badRequest().body(response);
        }

        User user = new User();
        user.setUsername(request.getUsername());
        user.setPassword(passwordEncoder.encode(request.getPassword()));
        user.setEmail(request.getEmail());
        user.setCreated_at(new Date());
        user.setIsactive(true);

        // GÁN ROLE MẶC ĐỊNH: BUYER
        Role buyerRole = roleRepository.findByRolename("BUYER")
                .orElseGet(() -> {
                    Role role = new Role();
                    role.setRolename("BUYER");
                    role.setJoindate(new Date());
                    return roleRepository.save(role);
                });
        user.setRoles(List.of(buyerRole));

        userRepository.save(user);

        Map<String, Object> response = new HashMap<>();
        response.put("status", "success");
        response.put("message", "Registered");
        response.put("username", user.getUsername());
        response.put("email", user.getEmail());
        response.put("roles", List.of("BUYER"));
        response.put("created_at", user.getCreated_at());
        return ResponseEntity.ok(response);
    }

    @Operation(
            summary = "Đăng nhập",
            description = "Xác thực người dùng và trả về JWT token cho phiên làm việc tiếp theo."
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Đăng nhập thành công - Trả về JWT token"),
            @ApiResponse(responseCode = "401", description = "Sai tên đăng nhập hoặc mật khẩu"),
            @ApiResponse(responseCode = "500", description = "Lỗi server")
    })
    @PostMapping("/login")
    public ResponseEntity<Map<String, Object>> login(@Valid @RequestBody LoginRequest request) {
        authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(request.getUsername(), request.getPassword())
        );
        //LazyInitializationException: Sử dụng findByUsernameWithRoles để eager fetch roles
        User user = userRepository.findByUsernameWithRoles(request.getUsername());
        CustomUserDetails customUserDetails = new CustomUserDetails(user);
        String jwt = jwtService.generateToken(customUserDetails);
        Map<String, Object> response = new HashMap<>();
        response.put("status", "success");
        response.put("token", jwt);
        response.put("username", user.getUsername());
        response.put("email", user.getEmail());
        response.put("roles", user.getRoles().stream().map(Role::getRolename).collect(Collectors.toList()));
        return ResponseEntity.ok(response);
    }

    @PostMapping("/logout")
    public ResponseEntity<Map<String, Object>> logout() {
        Map<String, Object> response = new HashMap<>();
        response.put("status", "success");
        response.put("message", "Logged out");
        return ResponseEntity.ok(response);
    }
}
