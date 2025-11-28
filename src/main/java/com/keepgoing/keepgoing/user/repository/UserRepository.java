package com.keepgoing.keepgoing.user.repository;

import com.keepgoing.keepgoing.user.domain.User;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UserRepository extends JpaRepository<User, Long> {

    boolean existsByEmail(String email);
}
