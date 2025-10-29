package leti_sisdis_6.hapauth.usermanagement.model;

import jakarta.persistence.*;
import lombok.*;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import com.fasterxml.jackson.annotation.JsonIgnore;

import java.util.Collection;
import java.util.List;

@Entity
@Table(name = "users")
@Getter @Setter
@NoArgsConstructor @AllArgsConstructor
@Builder
public class User implements UserDetails {

    @Id
    @Column(length = 32, nullable = false, updatable = false)
    private String id;

    @Column(nullable = false, unique = true, length = 100)
    private String username;

    @JsonIgnore                 // evita expor a password em respostas JSON
    @Column(nullable = false)
    private String password;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private Role role;

    // ---- UserDetails ----
    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        // Mapeia o enum para "ROLE_<NOME>"
        return List.of(new SimpleGrantedAuthority("ROLE_" + role.name()));
    }

    @Override public boolean isAccountNonExpired()  { return true; }
    @Override public boolean isAccountNonLocked()   { return true; }
    @Override public boolean isCredentialsNonExpired(){ return true; }
    @Override public boolean isEnabled()            { return true; }
}
