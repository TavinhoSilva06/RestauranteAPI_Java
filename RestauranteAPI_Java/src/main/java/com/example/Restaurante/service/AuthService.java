package com.example.Restaurante.service;

import com.example.Restaurante.dto.ClienteResponse;
import com.example.Restaurante.dto.LoginRequest;
import com.example.Restaurante.dto.LoginResponse;
import com.example.Restaurante.exception.CredenciaisInvalidasException;
import com.example.Restaurante.security.ClienteUserDetails;
import com.example.Restaurante.security.JwtService;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;

@Service
public class AuthService {

    private final AuthenticationManager authenticationManager;
    private final JwtService jwtService;

    public AuthService(AuthenticationManager authenticationManager, JwtService jwtService) {
        this.authenticationManager = authenticationManager;
        this.jwtService = jwtService;
    }

    public LoginResponse login(LoginRequest request) {
        try {
            Authentication authentication = authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(request.email(), request.senha())
            );

            ClienteUserDetails userDetails = (ClienteUserDetails) authentication.getPrincipal();
            String token = jwtService.gerarToken(userDetails);
            ClienteResponse clienteResponse = buildClienteResponse(userDetails);

            return new LoginResponse(token, clienteResponse);

        } catch (BadCredentialsException e) {
            throw new CredenciaisInvalidasException("E-mail ou senha inválidos");
        }
    }

    public ClienteResponse me(Authentication authentication) {
        ClienteUserDetails userDetails = (ClienteUserDetails) authentication.getPrincipal();
        return buildClienteResponse(userDetails);
    }

    private ClienteResponse buildClienteResponse(ClienteUserDetails userDetails) {
        return new ClienteResponse(
                userDetails.getCliente().getId(),
                userDetails.getCliente().getNome(),
                userDetails.getCliente().getEmail(),
                userDetails.getCliente().getPapel()
        );
    }
}
