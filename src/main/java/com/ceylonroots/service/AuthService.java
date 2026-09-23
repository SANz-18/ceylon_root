package com.ceylonroots.service;

import com.ceylonroots.dto.AuthResponse;
import com.ceylonroots.dto.LoginRequest;
import com.ceylonroots.dto.RegisterRequest;
import com.ceylonroots.exception.ApiException;
import com.ceylonroots.model.Role;
import com.ceylonroots.model.User;
import com.ceylonroots.repository.UserRepository;
import com.ceylonroots.security.JwtUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtUtil jwtUtil;

    public AuthResponse register(RegisterRequest req) {
        if (userRepository.existsByEmailIgnoreCase(req.getEmail())) {
            throw new ApiException("An account with that email already exists.", HttpStatus.CONFLICT);
        }
        User user = User.builder()
                .name(req.getName())
                .email(req.getEmail())
                .country(req.getCountry())
                .password(passwordEncoder.encode(req.getPassword()))
                .role(Role.BUYER) // self-registration is always as a buyer
                .active(true)
                .build();
        userRepository.save(user);
        String token = jwtUtil.generateToken(user.getId(), user.getEmail(), user.getRole().name());
        return new AuthResponse(token, user);
    }

    public AuthResponse login(LoginRequest req) {
        User user = userRepository.findByEmailIgnoreCase(req.getEmail())
                .orElseThrow(() -> new ApiException("Incorrect email or password.", HttpStatus.UNAUTHORIZED));

        if (!user.isActive()) {
            throw new ApiException("This account has been suspended.", HttpStatus.FORBIDDEN);
        }
        if (!passwordEncoder.matches(req.getPassword(), user.getPassword())) {
            throw new ApiException("Incorrect email or password.", HttpStatus.UNAUTHORIZED);
        }
        String token = jwtUtil.generateToken(user.getId(), user.getEmail(), user.getRole().name());
        return new AuthResponse(token, user);
    }
}
