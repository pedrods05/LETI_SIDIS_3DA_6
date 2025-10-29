package leti_sisdis_6.hapauth.usermanagement.repository;

import leti_sisdis_6.hapauth.usermanagement.model.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface UserRepository extends JpaRepository<User, String> {
    Optional<User> findByUsername(String username);
}
