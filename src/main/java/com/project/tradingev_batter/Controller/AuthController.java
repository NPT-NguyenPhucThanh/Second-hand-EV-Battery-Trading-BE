package com.project.tradingev_batter.Controller;

import com.project.tradingev_batter.Entity.User;
import com.project.tradingev_batter.Repository.UserRepository;
import com.project.tradingev_batter.dto.LoginRequest;
import com.project.tradingev_batter.dto.RegisterRequest;
import com.project.tradingev_batter.security.JwtService;
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
    public ResponseEntity<String> register(@RequestBody RegisterRequest request) {
        if (userRepository.findByUsername(request.getUsername()) != null) {
            return ResponseEntity.badRequest().body("Username exists");
        }
        User user = new User();
        user.setUsername(request.getUsername());
        user.setPassword(passwordEncoder.encode(request.getPassword()));
        user.setEmail(request.getEmail());
        user.setCreated_at(new Date());
        user.setIsactive(true);
        // Add default role "CLIENT" (giả sử có method addRole)
        userRepository.save(user);
        return ResponseEntity.ok("Registered");
    }

    @PostMapping("/login")
    public ResponseEntity<String> login(@RequestBody LoginRequest request) {
        Authentication authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(request.getUsername(), request.getPassword())
        );
        UserDetails userDetails = userDetailsService.loadUserByUsername(request.getUsername());
        String jwt = jwtService.generateToken(userDetails);
        return ResponseEntity.ok(jwt);
    }

    @PostMapping("/logout")
    public ResponseEntity<String> logout() {
        // Stateless, client xóa token
        return ResponseEntity.ok("Logged out");
    }

}
