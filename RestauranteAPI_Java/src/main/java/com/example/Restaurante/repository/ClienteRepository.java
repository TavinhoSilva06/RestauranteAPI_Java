package com.example.Restaurante.repository;

import com.example.Restaurante.document.Cliente;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.Optional;

public interface ClienteRepository extends MongoRepository<Cliente, String> {

    boolean existsByEmail(String email);

    Optional<Cliente> findByEmail(String email);
}
