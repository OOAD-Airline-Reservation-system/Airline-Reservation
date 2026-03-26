package com.airline.reservation.service;

import com.airline.reservation.dto.auth.AuthResponse;
import com.airline.reservation.dto.auth.LoginRequest;
import com.airline.reservation.dto.auth.RegisterRequest;
import com.airline.reservation.entity.Role;
import com.airline.reservation.entity.User;
import com.airline.reservation.exception.BadRequestException;
import com.airline.reservation.repository.UserRepository;
import com.airline.reservation.security.JwtService;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.Set;
import java.util.stream.Collectors;

@Service
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final AuthenticationManager authenticationManager;
    private final JwtService jwtService;

    public AuthService(
            UserRepository userRepository,
            PasswordEncoder passwordEncoder,
            AuthenticationManager authenticationManager,
            JwtService jwtService
    ) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.authenticationManager = authenticationManager;
        this.jwtService = jwtService;
    }

    public AuthResponse register(RegisterRequest request) {
        if (userRepository.existsByEmail(request.getEmail())) {
            throw new BadRequestException("Email is already registered");
        }

        User user = new User();
        user.setFullName(request.getFullName());
        user.setEmail(request.getEmail().toLowerCase());
        user.setPassword(passwordEncoder.encode(request.getPassword()));
        user.setRoles(Set.of(Role.ROLE_USER));
        userRepository.save(user);

        org.springframework.security.core.userdetails.User principal = new org.springframework.security.core.userdetails.User(
                user.getEmail(),
                user.getPassword(),
                user.getRoles().stream().map(role -> new org.springframework.security.core.authority.SimpleGrantedAuthority(role.name())).toList()
        );

        return new AuthResponse(
                jwtService.generateToken(principal),
                user.getEmail(),
                user.getFullName(),
                user.getRoles().stream().map(Enum::name).collect(Collectors.toSet())
        );
    }

    public AuthResponse login(LoginRequest request) {
        authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(request.getEmail().toLowerCase(), request.getPassword())
        );

        User user = userRepository.findByEmail(request.getEmail().toLowerCase())
                .orElseThrow(() -> new BadRequestException("User not found"));

        org.springframework.security.core.userdetails.User principal = new org.springframework.security.core.userdetails.User(
                user.getEmail(),
                user.getPassword(),
                user.getRoles().stream().map(role -> new org.springframework.security.core.authority.SimpleGrantedAuthority(role.name())).toList()
        );

        return new AuthResponse(
                jwtService.generateToken(principal),
                user.getEmail(),
                user.getFullName(),
                user.getRoles().stream().map(Enum::name).collect(Collectors.toSet())
        );
    }
}
