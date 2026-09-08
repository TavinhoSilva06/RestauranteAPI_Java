package com.example.Restaurante.security;

import com.example.Restaurante.repository.RegistroMongoTemplate;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

@Service
public class RegistroUserDetailsService implements UserDetailsService {

    private final RegistroMongoTemplate registroMongoTemplate;

    public RegistroUserDetailsService(RegistroMongoTemplate registroMongoTemplate) {
        this.registroMongoTemplate = registroMongoTemplate;
    }

    @Override
    public UserDetails loadUserByUsername(String email) throws UsernameNotFoundException {
        return registroMongoTemplate.findByEmail(email)
                .map(RegistroUserDetails::new)
                .orElseThrow(() -> new UsernameNotFoundException("Cliente não encontrado: " + email));
    }
}
