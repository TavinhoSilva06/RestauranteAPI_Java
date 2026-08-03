package com.example.Restaurante.service;

import com.example.Restaurante.document.Cliente;
import com.example.Restaurante.document.Papel;
import com.example.Restaurante.dto.ClienteCadastroRequest;
import com.example.Restaurante.dto.ClienteResponse;
import com.example.Restaurante.exception.EmailJaCadastradoException;
import com.example.Restaurante.repository.ClienteRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.Instant;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class ClienteServiceTest {

    @Mock
    private ClienteRepository clienteRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @InjectMocks
    private ClienteService clienteService;

    private ClienteCadastroRequest request;

    @BeforeEach
    void setup() {
        request = new ClienteCadastroRequest(
                "João Silva",
                "joao@example.com",
                "senha123456"
        );
    }

    @Test
    void testCadastrarComSucesso() {
        when(clienteRepository.existsByEmail(request.email())).thenReturn(false);
        when(passwordEncoder.encode(request.senha())).thenReturn("senha_hash_bcrypt");

        Cliente clienteSalvo = Cliente.builder()
                .id("123")
                .nome(request.nome())
                .email(request.email())
                .senha("senha_hash_bcrypt")
                .papel(Papel.CLIENTE)
                .dataCriacao(Instant.now())
                .build();

        when(clienteRepository.save(any(Cliente.class))).thenReturn(clienteSalvo);

        ClienteResponse response = clienteService.cadastrar(request);

        assertNotNull(response);
        assertEquals("João Silva", response.nome());
        assertEquals("joao@example.com", response.email());
        assertEquals(Papel.CLIENTE, response.papel());
        assertNotNull(response.id());

        verify(clienteRepository, times(1)).existsByEmail(request.email());
        verify(passwordEncoder, times(1)).encode(request.senha());
        verify(clienteRepository, times(1)).save(any(Cliente.class));
    }

    @Test
    void testCadastrarComEmailDuplicado() {
        when(clienteRepository.existsByEmail(request.email())).thenReturn(true);

        assertThrows(EmailJaCadastradoException.class, () -> {
            clienteService.cadastrar(request);
        });

        verify(clienteRepository, times(1)).existsByEmail(request.email());
        verify(clienteRepository, never()).save(any(Cliente.class));
    }

    @Test
    void testSenhaNuncaEArmazenadaEmTexto() {
        when(clienteRepository.existsByEmail(request.email())).thenReturn(false);
        when(passwordEncoder.encode(request.senha())).thenReturn("hash_bcrypt_seguro");

        Cliente clienteSalvo = Cliente.builder()
                .id("456")
                .nome(request.nome())
                .email(request.email())
                .senha("hash_bcrypt_seguro")
                .papel(Papel.CLIENTE)
                .dataCriacao(Instant.now())
                .build();

        when(clienteRepository.save(any(Cliente.class))).thenReturn(clienteSalvo);

        ClienteResponse response = clienteService.cadastrar(request);

        assertNotEquals(request.senha(), "hash_bcrypt_seguro");
        assertEquals("hash_bcrypt_seguro", clienteSalvo.getSenha());
        verify(passwordEncoder, times(1)).encode(request.senha());
    }
}
