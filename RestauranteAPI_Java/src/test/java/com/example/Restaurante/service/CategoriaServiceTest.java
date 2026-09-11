package com.example.Restaurante.service;

import com.example.Restaurante.document.Categoria;
import com.example.Restaurante.dto.CategoriaRequest;
import com.example.Restaurante.dto.CategoriaResponse;
import com.example.Restaurante.exception.CategoriaEmUsoException;
import com.example.Restaurante.exception.CategoriaNaoEncontradaException;
import com.example.Restaurante.exception.ValidacaoException;
import com.example.Restaurante.repository.CategoriaMongoTemplate;
import com.example.Restaurante.repository.PratoMongoTemplate;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class CategoriaServiceTest {

    @Mock
    private CategoriaMongoTemplate categoriaMongoTemplate;

    @Mock
    private PratoMongoTemplate pratoMongoTemplate;

    @InjectMocks
    private CategoriaService categoriaService;

    private CategoriaRequest request;

    @BeforeEach
    void setup() {
        request = new CategoriaRequest("Antipasti", "Entradas italianas");
    }

    @Test
    void testCriarComSucesso() {
        when(categoriaMongoTemplate.existsByNome(request.nome())).thenReturn(false);

        Categoria categoriaSalva = Categoria.builder()
                .id("123")
                .nome(request.nome())
                .descricao(request.descricao())
                .dataCriacao(Instant.now())
                .build();

        when(categoriaMongoTemplate.save(any(Categoria.class))).thenReturn(categoriaSalva);

        CategoriaResponse response = categoriaService.criar(request);

        assertNotNull(response);
        assertEquals("Antipasti", response.nome());
        assertEquals("Entradas italianas", response.descricao());
        assertEquals("123", response.id());

        verify(categoriaMongoTemplate, times(1)).existsByNome(request.nome());
        verify(categoriaMongoTemplate, times(1)).save(any(Categoria.class));
    }

    @Test
    void testCriarComNomeDuplicado() {
        when(categoriaMongoTemplate.existsByNome(request.nome())).thenReturn(true);

        assertThrows(ValidacaoException.class, () -> {
            categoriaService.criar(request);
        });

        verify(categoriaMongoTemplate, times(1)).existsByNome(request.nome());
        verify(categoriaMongoTemplate, never()).save(any(Categoria.class));
    }

    @Test
    void testEditarComSucesso() {
        String categoriaId = "123";
        Categoria categoriaExistente = Categoria.builder()
                .id(categoriaId)
                .nome("Entradas")
                .descricao("Entradas italianas")
                .dataCriacao(Instant.now())
                .build();

        when(categoriaMongoTemplate.findById(categoriaId)).thenReturn(Optional.of(categoriaExistente));
        when(categoriaMongoTemplate.existsByNome(request.nome())).thenReturn(false);

        Categoria categoriaAtualizada = Categoria.builder()
                .id(categoriaId)
                .nome(request.nome())
                .descricao(request.descricao())
                .dataCriacao(categoriaExistente.getDataCriacao())
                .build();

        when(categoriaMongoTemplate.save(any(Categoria.class))).thenReturn(categoriaAtualizada);

        CategoriaResponse response = categoriaService.editar(categoriaId, request);

        assertNotNull(response);
        assertEquals("Antipasti", response.nome());

        verify(categoriaMongoTemplate, times(1)).findById(categoriaId);
        verify(categoriaMongoTemplate, times(1)).existsByNome(request.nome());
        verify(categoriaMongoTemplate, times(1)).save(any(Categoria.class));
    }

    @Test
    void testEditarCategoriaInexistente() {
        String categoriaId = "999";
        when(categoriaMongoTemplate.findById(categoriaId)).thenReturn(Optional.empty());

        assertThrows(CategoriaNaoEncontradaException.class, () -> {
            categoriaService.editar(categoriaId, request);
        });

        verify(categoriaMongoTemplate, times(1)).findById(categoriaId);
        verify(categoriaMongoTemplate, never()).save(any(Categoria.class));
    }

    @Test
    void testRemoverComSucesso() {
        String categoriaId = "123";
        when(categoriaMongoTemplate.existsById(categoriaId)).thenReturn(true);
        when(pratoMongoTemplate.existsByCategoriaId(categoriaId)).thenReturn(false);

        categoriaService.remover(categoriaId);

        verify(categoriaMongoTemplate, times(1)).existsById(categoriaId);
        verify(pratoMongoTemplate, times(1)).existsByCategoriaId(categoriaId);
        verify(categoriaMongoTemplate, times(1)).deleteById(categoriaId);
    }

    @Test
    void testRemoverCategoriaInexistente() {
        String categoriaId = "999";
        when(categoriaMongoTemplate.existsById(categoriaId)).thenReturn(false);

        assertThrows(CategoriaNaoEncontradaException.class, () -> {
            categoriaService.remover(categoriaId);
        });

        verify(categoriaMongoTemplate, times(1)).existsById(categoriaId);
        verify(categoriaMongoTemplate, never()).deleteById(categoriaId);
    }

    @Test
    void testRemoverCategoriaEmUso() {
        String categoriaId = "123";
        when(categoriaMongoTemplate.existsById(categoriaId)).thenReturn(true);
        when(pratoMongoTemplate.existsByCategoriaId(categoriaId)).thenReturn(true);

        assertThrows(CategoriaEmUsoException.class, () -> {
            categoriaService.remover(categoriaId);
        });

        verify(categoriaMongoTemplate, times(1)).existsById(categoriaId);
        verify(pratoMongoTemplate, times(1)).existsByCategoriaId(categoriaId);
        verify(categoriaMongoTemplate, never()).deleteById(categoriaId);
    }
}
