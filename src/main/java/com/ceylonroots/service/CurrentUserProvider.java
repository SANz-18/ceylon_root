package com.ceylonroots.service;

import com.ceylonroots.exception.ApiException;
import com.ceylonroots.model.User;
import com.ceylonroots.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class CurrentUserProvider {

    private final UserRepository userRepository;

    public User get() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !(auth.getPrincipal() instanceof UserDetails details)) {
            throw new ApiException("Not authenticated.", HttpStatus.UNAUTHORIZED);
        }
        return userRepository.findByEmailIgnoreCase(details.getUsername())
                .orElseThrow(() -> new ApiException("User not found.", HttpStatus.UNAUTHORIZED));
    }
}
