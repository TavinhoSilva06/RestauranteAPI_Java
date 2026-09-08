package com.example.Restaurante.repository;

import com.example.Restaurante.document.Registro;
import com.example.Restaurante.document.Papel;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public class RegistroMongoTemplate {

    private final MongoTemplate mongoTemplate;

    public RegistroMongoTemplate(MongoTemplate mongoTemplate) {
        this.mongoTemplate = mongoTemplate;
    }

    private String colecaoDoPapel(Papel papel) {
        return switch (papel) {
            case CLIENTE -> "clientes";
            case FUNCIONARIO -> "funcionarios";
            case ADMIN -> "admins";
        };
    }

    public Registro save(Registro registro) {
        return mongoTemplate.save(registro, colecaoDoPapel(registro.getPapel()));
    }

    public Optional<Registro> findByEmail(String email) {
        for (Papel papel : Papel.values()) {
            Query query = new Query(Criteria.where("email").is(email));
            Registro registro = mongoTemplate.findOne(query, Registro.class, colecaoDoPapel(papel));
            if (registro != null) {
                return Optional.of(registro);
            }
        }
        return Optional.empty();
    }

    public boolean existsByEmail(String email) {
        return findByEmail(email).isPresent();
    }

    public boolean existsByPapel(Papel papel) {
        Query query = new Query();
        return mongoTemplate.exists(query, Registro.class, colecaoDoPapel(papel));
    }

    public List<Registro> findAllClientes() {
        return mongoTemplate.findAll(Registro.class, "clientes");
    }
}
