package com.example.Restaurante.controller;

import com.example.Restaurante.dto.ClienteResponse;
import com.example.Restaurante.dto.LoginRequest;
import com.example.Restaurante.dto.LoginResponse;
import com.example.Restaurante.service.AuthService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/auth")
public class AuthController {

    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    @PostMapping("/login")
    public ResponseEntity<LoginResponse> login(@RequestBody LoginRequest request) {
        LoginResponse response = authService.login(request);
        return ResponseEntity.status(HttpStatus.OK).body(response);
    }

    @GetMapping("/me")
    public ResponseEntity<ClienteResponse> me(Authentication authentication) {
        ClienteResponse response = authService.me(authentication);
        return ResponseEntity.ok(response);
    }
}
