package com.example.Restaurante.service;

import com.example.Restaurante.document.ItemPedido;
import com.example.Restaurante.document.Papel;
import com.example.Restaurante.document.Pedido;
import com.example.Restaurante.document.PedidoStatus;
import com.example.Restaurante.document.Prato;
import com.example.Restaurante.document.Registro;
import com.example.Restaurante.dto.PedidoItemRequest;
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
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class PedidoServiceTest {

    @Mock
    private PedidoMongoTemplate pedidoMongoTemplate;

    @Mock
    private PratoMongoTemplate pratoMongoTemplate;

    @Mock
    private Authentication authentication;

    @InjectMocks
    private PedidoService pedidoService;

    private Registro clienteLogado;
    private Registro clienteDiferente;
    private Registro admin;
    private Prato prato;
    private PedidoRequest request;

    @BeforeEach
    void setup() {
        clienteLogado = Registro.builder()
                .id("cliente-123")
                .nome("João Cliente")
                .email("joao@example.com")
                .papel(Papel.CLIENTE)
                .build();

        clienteDiferente = Registro.builder()
                .id("cliente-456")
                .nome("Maria Cliente")
                .email("maria@example.com")
                .papel(Papel.CLIENTE)
                .build();

        admin = Registro.builder()
                .id("admin-789")
                .nome("Admin User")
                .email("admin@example.com")
                .papel(Papel.ADMIN)
                .build();

        prato = Prato.builder()
                .id("prato-001")
                .nome("Pizza Margherita")
                .descricao("Pizza clássica italiana")
                .preco(new BigDecimal("35.00"))
                .categoriaId("cat-pizzas")
                .disponivel(true)
                .dataCriacao(Instant.now())
                .build();

        request = new PedidoRequest(List.of(
                new PedidoItemRequest("prato-001", 2)
        ));
    }

    @Test
    void testCriarComSucesso() {
        RegistroUserDetails userDetails = new RegistroUserDetails(clienteLogado);
        when(authentication.getPrincipal()).thenReturn(userDetails);
        when(pratoMongoTemplate.findById("prato-001")).thenReturn(Optional.of(prato));

        Pedido pedidoSalvo = Pedido.builder()
                .id("pedido-123")
                .clienteId(clienteLogado.getId())
                .itens(List.of(
                        ItemPedido.builder()
                                .pratoId("prato-001")
                                .nomePrato("Pizza Margherita")
                                .precoUnitario(new BigDecimal("35.00"))
                                .quantidade(2)
                                .subtotal(new BigDecimal("70.00"))
                                .build()
                ))
                .valorTotal(new BigDecimal("70.00"))
                .status(PedidoStatus.PENDENTE)
                .dataCriacao(Instant.now())
                .dataAtualizacao(Instant.now())
                .build();

        when(pedidoMongoTemplate.save(any(Pedido.class))).thenReturn(pedidoSalvo);

        PedidoResponse response = pedidoService.criar(request, authentication);

        assertNotNull(response);
        assertEquals("pedido-123", response.id());
        assertEquals(clienteLogado.getId(), response.clienteId());
        assertEquals(new BigDecimal("70.00"), response.valorTotal());
        assertEquals(PedidoStatus.PENDENTE, response.status());
        assertEquals(1, response.itens().size());

        verify(pratoMongoTemplate, times(1)).findById("prato-001");
        verify(pedidoMongoTemplate, times(1)).save(any(Pedido.class));
    }

    @Test
    void testCriarComPratoIndisponivel() {
        Prato pratoIndisponivel = Prato.builder()
                .id("prato-001")
                .nome("Pizza Margherita")
                .descricao("Pizza clássica italiana")
                .preco(new BigDecimal("35.00"))
                .categoriaId("cat-pizzas")
                .disponivel(false)
                .dataCriacao(Instant.now())
                .build();
        RegistroUserDetails userDetails = new RegistroUserDetails(clienteLogado);
        when(authentication.getPrincipal()).thenReturn(userDetails);
        when(pratoMongoTemplate.findById("prato-001")).thenReturn(Optional.of(pratoIndisponivel));

        assertThrows(PratoIndisponivelException.class, () -> {
            pedidoService.criar(request, authentication);
        });

        verify(pratoMongoTemplate, times(1)).findById("prato-001");
        verify(pedidoMongoTemplate, never()).save(any(Pedido.class));
    }

    @Test
    void testCriarComPratoInexistente() {
        RegistroUserDetails userDetails = new RegistroUserDetails(clienteLogado);
        when(authentication.getPrincipal()).thenReturn(userDetails);
        when(pratoMongoTemplate.findById("prato-001")).thenReturn(Optional.empty());

        assertThrows(PratoNaoEncontradoException.class, () -> {
            pedidoService.criar(request, authentication);
        });

        verify(pratoMongoTemplate, times(1)).findById("prato-001");
        verify(pedidoMongoTemplate, never()).save(any(Pedido.class));
    }

    @Test
    void testListarTodos() {
        Pedido pedido1 = Pedido.builder()
                .id("pedido-001")
                .clienteId(clienteLogado.getId())
                .status(PedidoStatus.PENDENTE)
                .dataCriacao(Instant.now())
                .build();

        Pedido pedido2 = Pedido.builder()
                .id("pedido-002")
                .clienteId(clienteDiferente.getId())
                .status(PedidoStatus.ENTREGUE)
                .dataCriacao(Instant.now())
                .build();

        when(pedidoMongoTemplate.findAll()).thenReturn(List.of(pedido1, pedido2));

        List<PedidoResponse> response = pedidoService.listarTodos();

        assertEquals(2, response.size());
        verify(pedidoMongoTemplate, times(1)).findAll();
    }

    @Test
    void testListarPorCliente() {
        Pedido pedido = Pedido.builder()
                .id("pedido-001")
                .clienteId(clienteLogado.getId())
                .status(PedidoStatus.PENDENTE)
                .dataCriacao(Instant.now())
                .build();

        RegistroUserDetails userDetails = new RegistroUserDetails(clienteLogado);
        when(authentication.getPrincipal()).thenReturn(userDetails);
        when(pedidoMongoTemplate.findByClienteId(clienteLogado.getId())).thenReturn(List.of(pedido));

        List<PedidoResponse> response = pedidoService.listarMeusPedidos(authentication);

        assertEquals(1, response.size());
        assertEquals("pedido-001", response.get(0).id());
        verify(pedidoMongoTemplate, times(1)).findByClienteId(clienteLogado.getId());
    }

    @Test
    void testBuscarPorIdComoDono() {
        Pedido pedido = Pedido.builder()
                .id("pedido-001")
                .clienteId(clienteLogado.getId())
                .status(PedidoStatus.PENDENTE)
                .dataCriacao(Instant.now())
                .build();

        RegistroUserDetails userDetails = new RegistroUserDetails(clienteLogado);
        when(authentication.getPrincipal()).thenReturn(userDetails);
        when(pedidoMongoTemplate.findById("pedido-001")).thenReturn(Optional.of(pedido));

        PedidoResponse response = pedidoService.buscarPorId("pedido-001", authentication);

        assertNotNull(response);
        assertEquals("pedido-001", response.id());
        verify(pedidoMongoTemplate, times(1)).findById("pedido-001");
    }

    @Test
    void testBuscarPorIdComoStaff() {
        Pedido pedido = Pedido.builder()
                .id("pedido-001")
                .clienteId(clienteLogado.getId())
                .status(PedidoStatus.PENDENTE)
                .dataCriacao(Instant.now())
                .build();

        RegistroUserDetails userDetails = new RegistroUserDetails(admin);
        when(authentication.getPrincipal()).thenReturn(userDetails);
        when(pedidoMongoTemplate.findById("pedido-001")).thenReturn(Optional.of(pedido));

        PedidoResponse response = pedidoService.buscarPorId("pedido-001", authentication);

        assertNotNull(response);
        assertEquals("pedido-001", response.id());
        verify(pedidoMongoTemplate, times(1)).findById("pedido-001");
    }

    @Test
    void testBuscarPorIdAcessoNegado() {
        Pedido pedido = Pedido.builder()
                .id("pedido-001")
                .clienteId(clienteDiferente.getId())
                .status(PedidoStatus.PENDENTE)
                .dataCriacao(Instant.now())
                .build();

        RegistroUserDetails userDetails = new RegistroUserDetails(clienteLogado);
        when(authentication.getPrincipal()).thenReturn(userDetails);
        when(pedidoMongoTemplate.findById("pedido-001")).thenReturn(Optional.of(pedido));

        assertThrows(AccessDeniedException.class, () -> {
            pedidoService.buscarPorId("pedido-001", authentication);
        });

        verify(pedidoMongoTemplate, times(1)).findById("pedido-001");
    }

    @Test
    void testBuscarPedidoInexistente() {
        RegistroUserDetails userDetails = new RegistroUserDetails(clienteLogado);
        when(authentication.getPrincipal()).thenReturn(userDetails);
        when(pedidoMongoTemplate.findById("pedido-inexistente")).thenReturn(Optional.empty());

        assertThrows(PedidoNaoEncontradoException.class, () -> {
            pedidoService.buscarPorId("pedido-inexistente", authentication);
        });

        verify(pedidoMongoTemplate, times(1)).findById("pedido-inexistente");
    }

    @Test
    void testAtualizarStatusComSucesso() {
        Pedido pedidoExistente = Pedido.builder()
                .id("pedido-001")
                .clienteId(clienteLogado.getId())
                .status(PedidoStatus.PENDENTE)
                .dataCriacao(Instant.now())
                .dataAtualizacao(Instant.now())
                .build();

        Pedido pedidoAtualizado = Pedido.builder()
                .id("pedido-001")
                .clienteId(clienteLogado.getId())
                .status(PedidoStatus.EM_PREPARO)
                .dataCriacao(pedidoExistente.getDataCriacao())
                .dataAtualizacao(Instant.now())
                .build();

        when(pedidoMongoTemplate.findById("pedido-001")).thenReturn(Optional.of(pedidoExistente));
        when(pedidoMongoTemplate.save(any(Pedido.class))).thenReturn(pedidoAtualizado);

        PedidoStatusUpdateRequest statusRequest = new PedidoStatusUpdateRequest(PedidoStatus.EM_PREPARO);
        PedidoResponse response = pedidoService.atualizarStatus("pedido-001", statusRequest);

        assertEquals(PedidoStatus.EM_PREPARO, response.status());
        verify(pedidoMongoTemplate, times(1)).findById("pedido-001");
        verify(pedidoMongoTemplate, times(1)).save(any(Pedido.class));
    }

    @Test
    void testAtualizarStatusTransicaoInvalida() {
        Pedido pedidoEntregue = Pedido.builder()
                .id("pedido-001")
                .clienteId(clienteLogado.getId())
                .status(PedidoStatus.ENTREGUE)
                .dataCriacao(Instant.now())
                .dataAtualizacao(Instant.now())
                .build();

        when(pedidoMongoTemplate.findById("pedido-001")).thenReturn(Optional.of(pedidoEntregue));

        PedidoStatusUpdateRequest statusRequest = new PedidoStatusUpdateRequest(PedidoStatus.PENDENTE);

        assertThrows(TransicaoStatusInvalidaException.class, () -> {
            pedidoService.atualizarStatus("pedido-001", statusRequest);
        });

        verify(pedidoMongoTemplate, times(1)).findById("pedido-001");
        verify(pedidoMongoTemplate, never()).save(any(Pedido.class));
    }

    @Test
    void testAtualizarStatusPedidoInexistente() {
        when(pedidoMongoTemplate.findById("pedido-inexistente")).thenReturn(Optional.empty());

        PedidoStatusUpdateRequest statusRequest = new PedidoStatusUpdateRequest(PedidoStatus.EM_PREPARO);

        assertThrows(PedidoNaoEncontradoException.class, () -> {
            pedidoService.atualizarStatus("pedido-inexistente", statusRequest);
        });

        verify(pedidoMongoTemplate, times(1)).findById("pedido-inexistente");
        verify(pedidoMongoTemplate, never()).save(any(Pedido.class));
    }
}
