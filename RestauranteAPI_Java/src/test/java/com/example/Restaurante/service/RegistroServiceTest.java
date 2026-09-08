package com.example.Restaurante.service;

import com.example.Restaurante.document.Papel;
import com.example.Restaurante.dto.RegistroCadastroRequest;
import com.example.Restaurante.dto.Registro;
import com.example.Restaurante.exception.EmailJaCadastradoException;
import com.example.Restaurante.repository.RegistroMongoTemplate;
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
public class RegistroServiceTest {

    @Mock
    private RegistroMongoTemplate registroMongoTemplate;

    @Mock
    private PasswordEncoder passwordEncoder;

    @InjectMocks
    private RegistroService registroService;

    private RegistroCadastroRequest request;

    @BeforeEach
    void setup() {
        request = new RegistroCadastroRequest(
                "João Silva",
                "joao@example.com",
                "senha123456"
        );
    }

    @Test
    void testCadastrarComSucesso() {
        when(registroMongoTemplate.existsByEmail(request.email())).thenReturn(false);
        when(passwordEncoder.encode(request.senha())).thenReturn("senha_hash_bcrypt");

        com.example.Restaurante.document.Registro registroSalvo = com.example.Restaurante.document.Registro.builder()
                .id("123")
                .nome(request.nome())
                .email(request.email())
                .senha("senha_hash_bcrypt")
                .papel(Papel.CLIENTE)
                .dataCriacao(Instant.now())
                .build();

        when(registroMongoTemplate.save(any(com.example.Restaurante.document.Registro.class))).thenReturn(registroSalvo);

        Registro response = registroService.cadastrar(request);

        assertNotNull(response);
        assertEquals("João Silva", response.nome());
        assertEquals("joao@example.com", response.email());
        assertEquals(Papel.CLIENTE, response.papel());
        assertNotNull(response.id());

        verify(registroMongoTemplate, times(1)).existsByEmail(request.email());
        verify(passwordEncoder, times(1)).encode(request.senha());
        verify(registroMongoTemplate, times(1)).save(any(com.example.Restaurante.document.Registro.class));
    }

    @Test
    void testCadastrarComEmailDuplicado() {
        when(registroMongoTemplate.existsByEmail(request.email())).thenReturn(true);

        assertThrows(EmailJaCadastradoException.class, () -> {
            registroService.cadastrar(request);
        });

        verify(registroMongoTemplate, times(1)).existsByEmail(request.email());
        verify(registroMongoTemplate, never()).save(any(com.example.Restaurante.document.Registro.class));
    }

    @Test
    void testSenhaNuncaEArmazenadaEmTexto() {
        when(registroMongoTemplate.existsByEmail(request.email())).thenReturn(false);
        when(passwordEncoder.encode(request.senha())).thenReturn("hash_bcrypt_seguro");

        com.example.Restaurante.document.Registro registroSalvo = com.example.Restaurante.document.Registro.builder()
                .id("456")
                .nome(request.nome())
                .email(request.email())
                .senha("hash_bcrypt_seguro")
                .papel(Papel.CLIENTE)
                .dataCriacao(Instant.now())
                .build();

        when(registroMongoTemplate.save(any(com.example.Restaurante.document.Registro.class))).thenReturn(registroSalvo);

        Registro response = registroService.cadastrar(request);

        assertNotEquals(request.senha(), "hash_bcrypt_seguro");
        assertEquals("hash_bcrypt_seguro", registroSalvo.getSenha());
        verify(passwordEncoder, times(1)).encode(request.senha());
    }
}
