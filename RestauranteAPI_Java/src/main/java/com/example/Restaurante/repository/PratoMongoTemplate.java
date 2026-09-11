package com.example.Restaurante.repository;

import com.example.Restaurante.document.Prato;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public class PratoMongoTemplate {

    private final MongoTemplate mongoTemplate;

    public PratoMongoTemplate(MongoTemplate mongoTemplate) {
        this.mongoTemplate = mongoTemplate;
    }

    public Prato save(Prato prato) {
        return mongoTemplate.save(prato, "pratos");
    }

    public Optional<Prato> findById(String id) {
        Query query = new Query(Criteria.where("_id").is(id));
        Prato prato = mongoTemplate.findOne(query, Prato.class, "pratos");
        return Optional.ofNullable(prato);
    }

    public List<Prato> findAll() {
        return mongoTemplate.findAll(Prato.class, "pratos");
    }

    public List<Prato> findByCategoriaId(String categoriaId) {
        Query query = new Query(Criteria.where("categoriaId").is(categoriaId));
        return mongoTemplate.find(query, Prato.class, "pratos");
    }

    public boolean existsByCategoriaId(String categoriaId) {
        Query query = new Query(Criteria.where("categoriaId").is(categoriaId));
        return mongoTemplate.exists(query, Prato.class, "pratos");
    }

    public boolean existsById(String id) {
        return findById(id).isPresent();
    }

    public void deleteById(String id) {
        Query query = new Query(Criteria.where("_id").is(id));
        mongoTemplate.remove(query, "pratos");
    }
}
