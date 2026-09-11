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

@Document(collection = "pratos")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Prato {

    @Id
    private String id;

    private String nome;

    private String descricao;

    private BigDecimal preco;

    private String categoriaId;

    private boolean disponivel;

    private String imagemUrl;

    private Instant dataCriacao;
}
