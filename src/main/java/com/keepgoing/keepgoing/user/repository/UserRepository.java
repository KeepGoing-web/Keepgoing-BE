package com.keepgoing.keepgoing.user.repository;

import com.keepgoing.keepgoing.user.domain.User;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UserRepository extends JpaRepository<User, Long> {

    boolean existsByEmail(String email);

    Optional<User> findByEmail(String email);

    Long countByEmail(String email);
}
