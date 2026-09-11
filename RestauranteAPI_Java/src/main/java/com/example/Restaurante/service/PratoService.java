package com.example.Restaurante.service;

import com.example.Restaurante.document.Prato;
import com.example.Restaurante.dto.PratoRequest;
import com.example.Restaurante.dto.PratoResponse;
import com.example.Restaurante.exception.CategoriaNaoEncontradaException;
import com.example.Restaurante.exception.PratoNaoEncontradoException;
import com.example.Restaurante.repository.CategoriaMongoTemplate;
import com.example.Restaurante.repository.PratoMongoTemplate;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.List;

@Service
public class PratoService {

    private final PratoMongoTemplate pratoMongoTemplate;
    private final CategoriaMongoTemplate categoriaMongoTemplate;

    public PratoService(PratoMongoTemplate pratoMongoTemplate, CategoriaMongoTemplate categoriaMongoTemplate) {
        this.pratoMongoTemplate = pratoMongoTemplate;
        this.categoriaMongoTemplate = categoriaMongoTemplate;
    }

    public PratoResponse criar(PratoRequest request) {
        if (!categoriaMongoTemplate.existsById(request.categoriaId())) {
            throw new CategoriaNaoEncontradaException(request.categoriaId());
        }

        Prato prato = Prato.builder()
                .nome(request.nome())
                .descricao(request.descricao())
                .preco(request.preco())
                .categoriaId(request.categoriaId())
                .disponivel(true)
                .imagemUrl(request.imagemUrl())
                .dataCriacao(Instant.now())
                .build();

        Prato pratoGravado = pratoMongoTemplate.save(prato);
        return toResponse(pratoGravado);
    }

    public PratoResponse editar(String id, PratoRequest request) {
        Prato prato = pratoMongoTemplate.findById(id)
                .orElseThrow(() -> new PratoNaoEncontradoException(id));

        if (!categoriaMongoTemplate.existsById(request.categoriaId())) {
            throw new CategoriaNaoEncontradaException(request.categoriaId());
        }

        prato.setNome(request.nome());
        prato.setDescricao(request.descricao());
        prato.setPreco(request.preco());
        prato.setCategoriaId(request.categoriaId());
        prato.setImagemUrl(request.imagemUrl());

        Prato pratoGravado = pratoMongoTemplate.save(prato);
        return toResponse(pratoGravado);
    }

    public List<PratoResponse> listarTodos() {
        return pratoMongoTemplate.findAll().stream()
                .map(this::toResponse)
                .toList();
    }

    public List<PratoResponse> listarPorCategoria(String categoriaId) {
        return pratoMongoTemplate.findByCategoriaId(categoriaId).stream()
                .map(this::toResponse)
                .toList();
    }

    public PratoResponse buscarPorId(String id) {
        Prato prato = pratoMongoTemplate.findById(id)
                .orElseThrow(() -> new PratoNaoEncontradoException(id));
        return toResponse(prato);
    }

    public PratoResponse alternarDisponibilidade(String id) {
        Prato prato = pratoMongoTemplate.findById(id)
                .orElseThrow(() -> new PratoNaoEncontradoException(id));

        prato.setDisponivel(!prato.isDisponivel());

        Prato pratoGravado = pratoMongoTemplate.save(prato);
        return toResponse(pratoGravado);
    }

    public void remover(String id) {
        if (!pratoMongoTemplate.existsById(id)) {
            throw new PratoNaoEncontradoException(id);
        }

        pratoMongoTemplate.deleteById(id);
    }

    private PratoResponse toResponse(Prato prato) {
        return new PratoResponse(
                prato.getId(),
                prato.getNome(),
                prato.getDescricao(),
                prato.getPreco(),
                prato.getCategoriaId(),
                prato.isDisponivel(),
                prato.getImagemUrl()
        );
    }
}
