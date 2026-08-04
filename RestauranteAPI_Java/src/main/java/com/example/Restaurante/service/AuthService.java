package com.example.Restaurante.service;

import com.example.Restaurante.dto.ClienteResponse;
import com.example.Restaurante.dto.LoginRequest;
import com.example.Restaurante.exception.CredenciaisInvalidasException;
import com.example.Restaurante.security.ClienteUserDetails;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.web.context.SecurityContextRepository;
import org.springframework.stereotype.Service;

@Service
public class AuthService {

    private final AuthenticationManager authenticationManager;
    private final SecurityContextRepository securityContextRepository;

    public AuthService(AuthenticationManager authenticationManager,
                      SecurityContextRepository securityContextRepository) {
        this.authenticationManager = authenticationManager;
        this.securityContextRepository = securityContextRepository;
    }

    public ClienteResponse login(LoginRequest request, HttpServletRequest httpRequest, HttpServletResponse httpResponse) {
        try {
            Authentication authentication = authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(request.email(), request.senha())
            );

            securityContextRepository.saveContext(org.springframework.security.core.context.SecurityContextHolder.createEmptyContext().getAuthentication() == null
                            ? org.springframework.security.core.context.SecurityContextHolder.createEmptyContext()
                            : org.springframework.security.core.context.SecurityContextHolder.getContext(),
                    httpRequest, httpResponse);

            org.springframework.security.core.context.SecurityContextHolder.getContext().setAuthentication(authentication);
            securityContextRepository.saveContext(org.springframework.security.core.context.SecurityContextHolder.getContext(), httpRequest, httpResponse);

            ClienteUserDetails userDetails = (ClienteUserDetails) authentication.getPrincipal();
            return new ClienteResponse(
                    userDetails.getCliente().getId(),
                    userDetails.getCliente().getNome(),
                    userDetails.getCliente().getEmail(),
                    userDetails.getCliente().getPapel()
            );
        } catch (BadCredentialsException e) {
            throw new CredenciaisInvalidasException("E-mail ou senha inválidos");
        }
    }

    public ClienteResponse me(Authentication authentication) {
        ClienteUserDetails userDetails = (ClienteUserDetails) authentication.getPrincipal();
        return new ClienteResponse(
                userDetails.getCliente().getId(),
                userDetails.getCliente().getNome(),
                userDetails.getCliente().getEmail(),
                userDetails.getCliente().getPapel()
        );
    }
}
