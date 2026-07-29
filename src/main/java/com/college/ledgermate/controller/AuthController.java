package com.college.ledgermate.controller;

import com.college.ledgermate.dto.AuthDtos;
import com.college.ledgermate.model.User;
import com.college.ledgermate.service.ExpenseService;
import com.college.ledgermate.service.UserService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController
public class AuthController {

    private final UserService userService;
    private final ExpenseService expenseService;

    public AuthController(UserService userService, ExpenseService expenseService) {
        this.userService = userService;
        this.expenseService = expenseService;
    }

    @PostMapping("/api/register")
    public ResponseEntity<AuthDtos.UserResponse> register(@Valid @RequestBody AuthDtos.RegisterRequest request) {
        User user = userService.registerUser(request.getName(), request.getEmail(), request.getPassword());
        AuthDtos.UserResponse resp = new AuthDtos.UserResponse();
        resp.setName(user.getName());
        resp.setEmail(user.getEmail());
        return ResponseEntity.ok(resp);
    }

    @PutMapping("/api/users")
    public ResponseEntity<AuthDtos.UserResponse> updateUser(
            @Valid @RequestBody AuthDtos.UpdateRequest request,Authentication authentication) {
        User user = userService.updateUser(request.getName(), authentication.getName(), request.getPassword());
        AuthDtos.UserResponse resp = new AuthDtos.UserResponse();
        resp.setName(user.getName());
        resp.setEmail(user.getEmail());
        return ResponseEntity.ok(resp);
    }

    @DeleteMapping("/api/users")
    public ResponseEntity<String> deleteUser(Authentication authentication) {

        User user = userService.getByEmail(authentication.getName());

        if (!expenseService.canDeleteUser(user)) {
            return ResponseEntity.badRequest().body("Cannot delete user until all debts are settled.");
        }

        userService.deleteUser(user);
        return ResponseEntity.ok("User deleted successfully.");
    }
}
