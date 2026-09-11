package com.example.Restaurante.repository;

import com.example.Restaurante.document.Categoria;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public class CategoriaMongoTemplate {

    private final MongoTemplate mongoTemplate;

    public CategoriaMongoTemplate(MongoTemplate mongoTemplate) {
        this.mongoTemplate = mongoTemplate;
    }

    public Categoria save(Categoria categoria) {
        return mongoTemplate.save(categoria, "categorias");
    }

    public Optional<Categoria> findById(String id) {
        Query query = new Query(Criteria.where("_id").is(id));
        Categoria categoria = mongoTemplate.findOne(query, Categoria.class, "categorias");
        return Optional.ofNullable(categoria);
    }

    public List<Categoria> findAll() {
        return mongoTemplate.findAll(Categoria.class, "categorias");
    }

    public boolean existsById(String id) {
        return findById(id).isPresent();
    }

    public boolean existsByNome(String nome) {
        Query query = new Query(Criteria.where("nome").is(nome));
        return mongoTemplate.exists(query, Categoria.class, "categorias");
    }

    public void deleteById(String id) {
        Query query = new Query(Criteria.where("_id").is(id));
        mongoTemplate.remove(query, "categorias");
    }
}
