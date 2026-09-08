package com.example.Restaurante.config;

import com.example.Restaurante.document.Registro;
import com.example.Restaurante.document.Papel;
import com.example.Restaurante.repository.RegistroMongoTemplate;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import java.time.Instant;

@Component
public class AdminSeedRunner implements ApplicationRunner {

    private final RegistroMongoTemplate registroMongoTemplate;
    private final PasswordEncoder passwordEncoder;

    @Value("${app.admin-seed.nome}")
    private String adminNome;

    @Value("${app.admin-seed.email}")
    private String adminEmail;

    @Value("${app.admin-seed.senha}")
    private String adminSenha;

    public AdminSeedRunner(RegistroMongoTemplate registroMongoTemplate, PasswordEncoder passwordEncoder) {
        this.registroMongoTemplate = registroMongoTemplate;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    public void run(ApplicationArguments args) {
        if (registroMongoTemplate.existsByPapel(Papel.ADMIN)) {
            return;
        }

        Registro admin = Registro.builder()
                .nome(adminNome)
                .email(adminEmail)
                .senha(passwordEncoder.encode(adminSenha))
                .papel(Papel.ADMIN)
                .dataCriacao(Instant.now())
                .build();

        registroMongoTemplate.save(admin);
    }
}
