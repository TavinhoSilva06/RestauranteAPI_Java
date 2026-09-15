package com.example.Restaurante.repository;

import com.example.Restaurante.document.Pedido;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public class PedidoMongoTemplate {

    private final MongoTemplate mongoTemplate;

    public PedidoMongoTemplate(MongoTemplate mongoTemplate) {
        this.mongoTemplate = mongoTemplate;
    }

    public Pedido save(Pedido pedido) {
        return mongoTemplate.save(pedido, "pedidos");
    }

    public Optional<Pedido> findById(String id) {
        Query query = new Query(Criteria.where("_id").is(id));
        Pedido pedido = mongoTemplate.findOne(query, Pedido.class, "pedidos");
        return Optional.ofNullable(pedido);
    }

    public List<Pedido> findAll() {
        return mongoTemplate.findAll(Pedido.class, "pedidos");
    }

    public List<Pedido> findByClienteId(String clienteId) {
        Query query = new Query(Criteria.where("clienteId").is(clienteId));
        return mongoTemplate.find(query, Pedido.class, "pedidos");
    }
}
