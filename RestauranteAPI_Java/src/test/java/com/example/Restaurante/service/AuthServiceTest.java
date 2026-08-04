package com.example.Restaurante.service;

import com.example.Restaurante.document.Cliente;
import com.example.Restaurante.document.Papel;
import com.example.Restaurante.dto.ClienteResponse;
import com.example.Restaurante.dto.LoginRequest;
import com.example.Restaurante.exception.CredenciaisInvalidasException;
import com.example.Restaurante.security.ClienteUserDetails;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.web.context.SecurityContextRepository;

import java.time.Instant;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class AuthServiceTest {

    @Mock
    private AuthenticationManager authenticationManager;

    @Mock
    private SecurityContextRepository securityContextRepository;

    @Mock
    private HttpServletRequest httpRequest;

    @Mock
    private HttpServletResponse httpResponse;

    @InjectMocks
    private AuthService authService;

    private LoginRequest loginRequest;
    private Cliente cliente;
    private ClienteUserDetails userDetails;

    @BeforeEach
    void setup() {
        loginRequest = new LoginRequest("joao@example.com", "senha123456");

        cliente = Cliente.builder()
                .id("123")
                .nome("João Silva")
                .email("joao@example.com")
                .senha("senha_hash_bcrypt")
                .papel(Papel.CLIENTE)
                .dataCriacao(Instant.now())
                .build();

        userDetails = new ClienteUserDetails(cliente);
    }

    @Test
    void testLoginComSucesso() {
        Authentication authentication = mock(Authentication.class);
        when(authentication.getPrincipal()).thenReturn(userDetails);
        when(authenticationManager.authenticate(any(UsernamePasswordAuthenticationToken.class)))
                .thenReturn(authentication);

        ClienteResponse response = authService.login(loginRequest, httpRequest, httpResponse);

        assertNotNull(response);
        assertEquals("João Silva", response.nome());
        assertEquals("joao@example.com", response.email());
        assertEquals(Papel.CLIENTE, response.papel());
        assertEquals("123", response.id());

        verify(authenticationManager, times(1)).authenticate(any(UsernamePasswordAuthenticationToken.class));
        verify(securityContextRepository, times(2)).saveContext(any(), any(), any());
    }

    @Test
    void testLoginComSenhaInvalida() {
        when(authenticationManager.authenticate(any(UsernamePasswordAuthenticationToken.class)))
                .thenThrow(new BadCredentialsException("Bad credentials"));

        assertThrows(CredenciaisInvalidasException.class, () -> {
            authService.login(loginRequest, httpRequest, httpResponse);
        });

        verify(authenticationManager, times(1)).authenticate(any(UsernamePasswordAuthenticationToken.class));
    }

    @Test
    void testMeComAutenticado() {
        Authentication authentication = mock(Authentication.class);
        when(authentication.getPrincipal()).thenReturn(userDetails);

        ClienteResponse response = authService.me(authentication);

        assertNotNull(response);
        assertEquals("João Silva", response.nome());
        assertEquals("joao@example.com", response.email());
        assertEquals(Papel.CLIENTE, response.papel());
        assertEquals("123", response.id());
    }
}
