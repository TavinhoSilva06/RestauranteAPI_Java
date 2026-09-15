package com.example.Restaurante.document;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

@Document(collection = "pedidos")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Pedido {
    @Id
    private String id;
    private String clienteId;
    private List<ItemPedido> itens;
    private BigDecimal valorTotal;
    private PedidoStatus status;
    private Instant dataCriacao;
    private Instant dataAtualizacao;
}
