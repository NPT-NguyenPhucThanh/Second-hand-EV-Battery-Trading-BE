package com.project.tradingev_batter.Controller;

import com.project.tradingev_batter.Entity.Role;
import com.project.tradingev_batter.Entity.User;
import com.project.tradingev_batter.Repository.UserRepository;
import com.project.tradingev_batter.dto.LoginRequest;
import com.project.tradingev_batter.dto.RegisterRequest;
import com.project.tradingev_batter.security.CustomUserDetails;
import com.project.tradingev_batter.security.JwtService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
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
public class AuthController {

    private final AuthenticationManager authenticationManager;
    private final JwtService jwtService;
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final UserDetailsService userDetailsService;

    public AuthController(AuthenticationManager authenticationManager, JwtService jwtService, UserRepository userRepository, PasswordEncoder passwordEncoder, UserDetailsService userDetailsService) {
        this.authenticationManager = authenticationManager;
        this.jwtService = jwtService;
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.userDetailsService = userDetailsService;
    }

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
        userRepository.save(user);
        Map<String, Object> response = new HashMap<>();
        response.put("status", "success");
        response.put("message", "Registered");
        response.put("username", user.getUsername());
        response.put("email", user.getEmail());
        response.put("created_at", user.getCreated_at());
        return ResponseEntity.ok(response);
    }

    @PostMapping("/login")
    public ResponseEntity<Map<String, Object>> login(@Valid @RequestBody LoginRequest request) {
        Authentication authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(request.getUsername(), request.getPassword())
        );
        // Fix LazyInitializationException: Sử dụng findByUsernameWithRoles để eager fetch roles
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
