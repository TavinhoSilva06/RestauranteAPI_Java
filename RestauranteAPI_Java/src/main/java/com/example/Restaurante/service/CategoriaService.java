package com.example.Restaurante.service;

import com.example.Restaurante.document.Categoria;
import com.example.Restaurante.dto.CategoriaRequest;
import com.example.Restaurante.dto.CategoriaResponse;
import com.example.Restaurante.exception.CategoriaEmUsoException;
import com.example.Restaurante.exception.CategoriaNaoEncontradaException;
import com.example.Restaurante.exception.ValidacaoException;
import com.example.Restaurante.repository.CategoriaMongoTemplate;
import com.example.Restaurante.repository.PratoMongoTemplate;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.List;

@Service
public class CategoriaService {

    private final CategoriaMongoTemplate categoriaMongoTemplate;
    private final PratoMongoTemplate pratoMongoTemplate;

    public CategoriaService(CategoriaMongoTemplate categoriaMongoTemplate, PratoMongoTemplate pratoMongoTemplate) {
        this.categoriaMongoTemplate = categoriaMongoTemplate;
        this.pratoMongoTemplate = pratoMongoTemplate;
    }

    public CategoriaResponse criar(CategoriaRequest request) {
        if (categoriaMongoTemplate.existsByNome(request.nome())) {
            throw new ValidacaoException("Categoria já cadastrada com esse nome");
        }

        Categoria categoria = Categoria.builder()
                .nome(request.nome())
                .descricao(request.descricao())
                .dataCriacao(Instant.now())
                .build();

        Categoria categoriaGravada = categoriaMongoTemplate.save(categoria);
        return toResponse(categoriaGravada);
    }

    public CategoriaResponse editar(String id, CategoriaRequest request) {
        Categoria categoria = categoriaMongoTemplate.findById(id)
                .orElseThrow(() -> new CategoriaNaoEncontradaException(id));

        if (!categoria.getNome().equals(request.nome()) && categoriaMongoTemplate.existsByNome(request.nome())) {
            throw new ValidacaoException("Categoria já cadastrada com esse nome");
        }

        categoria.setNome(request.nome());
        categoria.setDescricao(request.descricao());

        Categoria categoriaGravada = categoriaMongoTemplate.save(categoria);
        return toResponse(categoriaGravada);
    }

    public List<CategoriaResponse> listarTodas() {
        return categoriaMongoTemplate.findAll().stream()
                .map(this::toResponse)
                .toList();
    }

    public CategoriaResponse buscarPorId(String id) {
        Categoria categoria = categoriaMongoTemplate.findById(id)
                .orElseThrow(() -> new CategoriaNaoEncontradaException(id));
        return toResponse(categoria);
    }

    public void remover(String id) {
        if (!categoriaMongoTemplate.existsById(id)) {
            throw new CategoriaNaoEncontradaException(id);
        }

        if (pratoMongoTemplate.existsByCategoriaId(id)) {
            throw new CategoriaEmUsoException(id);
        }

        categoriaMongoTemplate.deleteById(id);
    }

    private CategoriaResponse toResponse(Categoria categoria) {
        return new CategoriaResponse(categoria.getId(), categoria.getNome(), categoria.getDescricao());
    }
}
