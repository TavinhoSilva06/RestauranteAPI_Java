package com.example.Restaurante.security;

import com.example.Restaurante.document.Registro;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.Collection;
import java.util.List;

public class RegistroUserDetails implements UserDetails {

    private final Registro registro;

    public RegistroUserDetails(Registro registro) {
        this.registro = registro;
    }

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return List.of(new SimpleGrantedAuthority("ROLE_" + registro.getPapel().name()));
    }

    @Override
    public String getPassword() {
        return registro.getSenha();
    }

    @Override
    public String getUsername() {
        return registro.getEmail();
    }

    @Override
    public boolean isAccountNonExpired() {
        return true;
    }

    @Override
    public boolean isAccountNonLocked() {
        return true;
    }

    @Override
    public boolean isCredentialsNonExpired() {
        return true;
    }

    @Override
    public boolean isEnabled() {
        return true;
    }

    public Registro getCliente() {
        return registro;
    }
}
