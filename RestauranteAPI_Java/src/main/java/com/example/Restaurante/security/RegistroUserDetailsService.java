package com.example.Restaurante.security;

import com.example.Restaurante.repository.RegistroRepository;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

@Service
public class RegistroUserDetailsService implements UserDetailsService {

    private final RegistroRepository registroRepository;

    public RegistroUserDetailsService(RegistroRepository registroRepository) {
        this.registroRepository = registroRepository;
    }

    @Override
    public UserDetails loadUserByUsername(String email) throws UsernameNotFoundException {
        return registroRepository.findByEmail(email)
                .map(RegistroUserDetails::new)
                .orElseThrow(() -> new UsernameNotFoundException("Cliente não encontrado: " + email));
    }
}
