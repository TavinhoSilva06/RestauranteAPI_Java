package com.example.Restaurante.service;

import com.example.Restaurante.document.Prato;
import com.example.Restaurante.dto.PratoRequest;
import com.example.Restaurante.dto.PratoResponse;
import com.example.Restaurante.exception.CategoriaNaoEncontradaException;
import com.example.Restaurante.exception.PratoNaoEncontradoException;
import com.example.Restaurante.repository.CategoriaMongoTemplate;
import com.example.Restaurante.repository.PratoMongoTemplate;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class PratoServiceTest {

    @Mock
    private PratoMongoTemplate pratoMongoTemplate;

    @Mock
    private CategoriaMongoTemplate categoriaMongoTemplate;

    @InjectMocks
    private PratoService pratoService;

    private PratoRequest request;

    @BeforeEach
    void setup() {
        request = new PratoRequest(
                "Margherita",
                "Pizza clássica italiana",
                new BigDecimal("25.00"),
                "cat-pizzas-123",
                "https://example.com/margherita.jpg"
        );
    }

    @Test
    void testCriarComSucesso() {
        when(categoriaMongoTemplate.existsById(request.categoriaId())).thenReturn(true);

        Prato pratoSalvo = Prato.builder()
                .id("prato-123")
                .nome(request.nome())
                .descricao(request.descricao())
                .preco(request.preco())
                .categoriaId(request.categoriaId())
                .disponivel(true)
                .imagemUrl(request.imagemUrl())
                .dataCriacao(Instant.now())
                .build();

        when(pratoMongoTemplate.save(any(Prato.class))).thenReturn(pratoSalvo);

        PratoResponse response = pratoService.criar(request);

        assertNotNull(response);
        assertEquals("Margherita", response.nome());
        assertEquals(new BigDecimal("25.00"), response.preco());
        assertTrue(response.disponivel());
        assertEquals("prato-123", response.id());

        verify(categoriaMongoTemplate, times(1)).existsById(request.categoriaId());
        verify(pratoMongoTemplate, times(1)).save(any(Prato.class));
    }

    @Test
    void testCriarComCategoriaInexistente() {
        when(categoriaMongoTemplate.existsById(request.categoriaId())).thenReturn(false);

        assertThrows(CategoriaNaoEncontradaException.class, () -> {
            pratoService.criar(request);
        });

        verify(categoriaMongoTemplate, times(1)).existsById(request.categoriaId());
        verify(pratoMongoTemplate, never()).save(any(Prato.class));
    }

    @Test
    void testEditarComSucesso() {
        String pratoId = "prato-123";
        Prato pratoExistente = Prato.builder()
                .id(pratoId)
                .nome("Margherita")
                .descricao("Pizza clássica")
                .preco(new BigDecimal("25.00"))
                .categoriaId("cat-pizzas-123")
                .disponivel(true)
                .imagemUrl("https://example.com/margherita.jpg")
                .dataCriacao(Instant.now())
                .build();

        when(pratoMongoTemplate.findById(pratoId)).thenReturn(Optional.of(pratoExistente));
        when(categoriaMongoTemplate.existsById(request.categoriaId())).thenReturn(true);

        Prato pratoAtualizado = Prato.builder()
                .id(pratoId)
                .nome(request.nome())
                .descricao(request.descricao())
                .preco(request.preco())
                .categoriaId(request.categoriaId())
                .disponivel(true)
                .imagemUrl(request.imagemUrl())
                .dataCriacao(pratoExistente.getDataCriacao())
                .build();

        when(pratoMongoTemplate.save(any(Prato.class))).thenReturn(pratoAtualizado);

        PratoResponse response = pratoService.editar(pratoId, request);

        assertNotNull(response);
        assertEquals("Margherita", response.nome());

        verify(pratoMongoTemplate, times(1)).findById(pratoId);
        verify(categoriaMongoTemplate, times(1)).existsById(request.categoriaId());
        verify(pratoMongoTemplate, times(1)).save(any(Prato.class));
    }

    @Test
    void testEditarPratoInexistente() {
        String pratoId = "prato-999";
        when(pratoMongoTemplate.findById(pratoId)).thenReturn(Optional.empty());

        assertThrows(PratoNaoEncontradoException.class, () -> {
            pratoService.editar(pratoId, request);
        });

        verify(pratoMongoTemplate, times(1)).findById(pratoId);
        verify(pratoMongoTemplate, never()).save(any(Prato.class));
    }

    @Test
    void testAlternarDisponibilidade() {
        String pratoId = "prato-123";
        Prato pratoExistente = Prato.builder()
                .id(pratoId)
                .nome("Margherita")
                .descricao("Pizza clássica")
                .preco(new BigDecimal("25.00"))
                .categoriaId("cat-pizzas-123")
                .disponivel(true)
                .imagemUrl("https://example.com/margherita.jpg")
                .dataCriacao(Instant.now())
                .build();

        when(pratoMongoTemplate.findById(pratoId)).thenReturn(Optional.of(pratoExistente));

        Prato pratoDesativado = Prato.builder()
                .id(pratoId)
                .nome("Margherita")
                .descricao("Pizza clássica")
                .preco(new BigDecimal("25.00"))
                .categoriaId("cat-pizzas-123")
                .disponivel(false)
                .imagemUrl("https://example.com/margherita.jpg")
                .dataCriacao(pratoExistente.getDataCriacao())
                .build();

        when(pratoMongoTemplate.save(any(Prato.class))).thenReturn(pratoDesativado);

        PratoResponse response = pratoService.alternarDisponibilidade(pratoId);

        assertNotNull(response);
        assertFalse(response.disponivel());

        verify(pratoMongoTemplate, times(1)).findById(pratoId);
        verify(pratoMongoTemplate, times(1)).save(any(Prato.class));
    }

    @Test
    void testRemoverComSucesso() {
        String pratoId = "prato-123";
        when(pratoMongoTemplate.existsById(pratoId)).thenReturn(true);

        pratoService.remover(pratoId);

        verify(pratoMongoTemplate, times(1)).existsById(pratoId);
        verify(pratoMongoTemplate, times(1)).deleteById(pratoId);
    }

    @Test
    void testRemoverPratoInexistente() {
        String pratoId = "prato-999";
        when(pratoMongoTemplate.existsById(pratoId)).thenReturn(false);

        assertThrows(PratoNaoEncontradoException.class, () -> {
            pratoService.remover(pratoId);
        });

        verify(pratoMongoTemplate, times(1)).existsById(pratoId);
        verify(pratoMongoTemplate, never()).deleteById(pratoId);
    }
}
