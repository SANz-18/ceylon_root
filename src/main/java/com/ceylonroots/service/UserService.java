package com.ceylonroots.service;

import com.ceylonroots.dto.CreateUserRequest;
import com.ceylonroots.exception.ApiException;
import com.ceylonroots.model.Role;
import com.ceylonroots.model.User;
import com.ceylonroots.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    public List<User> findAll() {
        return userRepository.findAll();
    }

    /** Admin-only path for provisioning Staff/Admin (and, if convenient, Buyer) accounts. */
    public User createUser(CreateUserRequest req) {
        if (userRepository.existsByEmailIgnoreCase(req.getEmail())) {
            throw new ApiException("An account with that email already exists.", HttpStatus.CONFLICT);
        }
        User user = User.builder()
                .name(req.getName())
                .email(req.getEmail())
                .password(passwordEncoder.encode(req.getPassword()))
                .role(req.getRole())
                .country(req.getCountry())
                .active(true)
                .build();
        return userRepository.save(user);
    }

    public User updateRole(Long id, Role role, User actor) {
        User u = get(id);
        if (u.getId().equals(actor.getId())) {
            throw new ApiException("You cannot change your own role.", HttpStatus.BAD_REQUEST);
        }
        u.setRole(role);
        return userRepository.save(u);
    }

    public User toggleActive(Long id, User actor) {
        User u = get(id);
        if (u.getId().equals(actor.getId())) {
            throw new ApiException("You cannot suspend your own account.", HttpStatus.BAD_REQUEST);
        }
        u.setActive(!u.isActive());
        return userRepository.save(u);
    }

    private User get(Long id) {
        return userRepository.findById(id)
                .orElseThrow(() -> new ApiException("User not found.", HttpStatus.NOT_FOUND));
    }
}
