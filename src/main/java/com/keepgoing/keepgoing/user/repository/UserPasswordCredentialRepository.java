package com.keepgoing.keepgoing.user.repository;

import com.keepgoing.keepgoing.user.domain.UserPasswordCredential;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UserPasswordCredentialRepository extends JpaRepository<UserPasswordCredential, Long> {
}
