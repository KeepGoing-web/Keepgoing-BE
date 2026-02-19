package com.keepgoing.keepgoing.user.repository;

import com.keepgoing.keepgoing.user.domain.OAuthProvider;
import com.keepgoing.keepgoing.user.domain.UserOAuthAccount;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UserOAuthAccountRepository extends JpaRepository<UserOAuthAccount, Long> {

    Optional<UserOAuthAccount> findByProviderAndProviderUserId(OAuthProvider provider, String providerUserId);

    List<UserOAuthAccount> findByUserId(Long userId);

}
