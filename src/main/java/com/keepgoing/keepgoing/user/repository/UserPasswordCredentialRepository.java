package com.keepgoing.keepgoing.user.repository;

import com.keepgoing.keepgoing.user.domain.User;
import com.keepgoing.keepgoing.user.domain.UserPasswordCredential;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UserPasswordCredentialRepository extends JpaRepository<UserPasswordCredential, Long> {

    Optional<UserPasswordCredential> findByUser(User user);

}
