package com.ceylonroots.controller;

import com.ceylonroots.dto.CreateUserRequest;
import com.ceylonroots.dto.RoleUpdateRequest;
import com.ceylonroots.model.User;
import com.ceylonroots.service.CurrentUserProvider;
import com.ceylonroots.service.UserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;
    private final CurrentUserProvider currentUserProvider;

    @GetMapping
    public List<User> all() {
        return userService.findAll();
    }

    @PostMapping
    public User create(@Valid @RequestBody CreateUserRequest req) {
        return userService.createUser(req);
    }

    @PutMapping("/{id}/role")
    public User updateRole(@PathVariable Long id, @RequestBody RoleUpdateRequest req) {
        return userService.updateRole(id, req.getRole(), currentUserProvider.get());
    }

    @PutMapping("/{id}/toggle-active")
    public User toggleActive(@PathVariable Long id) {
        return userService.toggleActive(id, currentUserProvider.get());
    }
}
