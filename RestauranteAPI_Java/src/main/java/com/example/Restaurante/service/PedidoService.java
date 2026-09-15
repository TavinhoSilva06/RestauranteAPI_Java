package com.example.Restaurante.service;

import com.example.Restaurante.document.ItemPedido;
import com.example.Restaurante.document.Pedido;
import com.example.Restaurante.document.PedidoStatus;
import com.example.Restaurante.document.Prato;
import com.example.Restaurante.document.Registro;
import com.example.Restaurante.dto.PedidoItemResponse;
import com.example.Restaurante.dto.PedidoRequest;
import com.example.Restaurante.dto.PedidoResponse;
import com.example.Restaurante.dto.PedidoStatusUpdateRequest;
import com.example.Restaurante.exception.PedidoNaoEncontradoException;
import com.example.Restaurante.exception.PratoIndisponivelException;
import com.example.Restaurante.exception.PratoNaoEncontradoException;
import com.example.Restaurante.exception.TransicaoStatusInvalidaException;
import com.example.Restaurante.repository.PedidoMongoTemplate;
import com.example.Restaurante.repository.PratoMongoTemplate;
import com.example.Restaurante.security.RegistroUserDetails;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

@Service
public class PedidoService {

    private final PedidoMongoTemplate pedidoMongoTemplate;
    private final PratoMongoTemplate pratoMongoTemplate;

    public PedidoService(PedidoMongoTemplate pedidoMongoTemplate, PratoMongoTemplate pratoMongoTemplate) {
        this.pedidoMongoTemplate = pedidoMongoTemplate;
        this.pratoMongoTemplate = pratoMongoTemplate;
    }

    public PedidoResponse criar(PedidoRequest request, Authentication authentication) {
        Registro cliente = obterUsuarioAutenticado(authentication);

        List<ItemPedido> itens = request.itens().stream()
                .map(itemRequest -> {
                    Prato prato = pratoMongoTemplate.findById(itemRequest.pratoId())
                            .orElseThrow(() -> new PratoNaoEncontradoException(itemRequest.pratoId()));

                    if (!prato.isDisponivel()) {
                        throw new PratoIndisponivelException(itemRequest.pratoId());
                    }

                    BigDecimal subtotal = prato.getPreco().multiply(BigDecimal.valueOf(itemRequest.quantidade()));

                    return ItemPedido.builder()
                            .pratoId(prato.getId())
                            .nomePrato(prato.getNome())
                            .precoUnitario(prato.getPreco())
                            .quantidade(itemRequest.quantidade())
                            .subtotal(subtotal)
                            .build();
                })
                .toList();

        BigDecimal valorTotal = itens.stream()
                .map(ItemPedido::getSubtotal)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        Pedido pedido = Pedido.builder()
                .clienteId(cliente.getId())
                .itens(itens)
                .valorTotal(valorTotal)
                .status(PedidoStatus.PENDENTE)
                .dataCriacao(Instant.now())
                .dataAtualizacao(Instant.now())
                .build();

        Pedido pedidoGravado = pedidoMongoTemplate.save(pedido);
        return toResponse(pedidoGravado);
    }

    public List<PedidoResponse> listarTodos() {
        return pedidoMongoTemplate.findAll().stream()
                .map(this::toResponse)
                .toList();
    }

    public List<PedidoResponse> listarMeusPedidos(Authentication authentication) {
        Registro cliente = obterUsuarioAutenticado(authentication);
        return pedidoMongoTemplate.findByClienteId(cliente.getId()).stream()
                .map(this::toResponse)
                .toList();
    }

    public PedidoResponse buscarPorId(String id, Authentication authentication) {
        Pedido pedido = pedidoMongoTemplate.findById(id)
                .orElseThrow(() -> new PedidoNaoEncontradoException(id));

        Registro usuario = obterUsuarioAutenticado(authentication);
        validarAcesso(pedido, usuario);

        return toResponse(pedido);
    }

    public PedidoResponse atualizarStatus(String id, PedidoStatusUpdateRequest request) {
        Pedido pedido = pedidoMongoTemplate.findById(id)
                .orElseThrow(() -> new PedidoNaoEncontradoException(id));

        if (!pedido.getStatus().podeTransicionarPara(request.status())) {
            throw new TransicaoStatusInvalidaException(pedido.getStatus(), request.status());
        }

        pedido.setStatus(request.status());
        pedido.setDataAtualizacao(Instant.now());

        Pedido pedidoGravado = pedidoMongoTemplate.save(pedido);
        return toResponse(pedidoGravado);
    }

    private Registro obterUsuarioAutenticado(Authentication authentication) {
        return ((RegistroUserDetails) authentication.getPrincipal()).getCliente();
    }

    private void validarAcesso(Pedido pedido, Registro usuario) {
        boolean isCliente = "CLIENTE".equals(usuario.getPapel().name());
        boolean isOwner = pedido.getClienteId().equals(usuario.getId());

        if (isCliente && !isOwner) {
            throw new AccessDeniedException("Acesso negado");
        }
    }

    private PedidoResponse toResponse(Pedido pedido) {
        List<PedidoItemResponse> itensResponse = pedido.getItens().stream()
                .map(item -> new PedidoItemResponse(
                        item.getPratoId(),
                        item.getNomePrato(),
                        item.getPrecoUnitario(),
                        item.getQuantidade(),
                        item.getSubtotal()
                ))
                .toList();

        return new PedidoResponse(
                pedido.getId(),
                pedido.getClienteId(),
                itensResponse,
                pedido.getValorTotal(),
                pedido.getStatus(),
                pedido.getDataCriacao(),
                pedido.getDataAtualizacao()
        );
    }
}
