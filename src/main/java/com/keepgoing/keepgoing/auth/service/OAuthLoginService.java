package com.keepgoing.keepgoing.auth.service;

import com.keepgoing.keepgoing.user.domain.OAuthProvider;
import com.keepgoing.keepgoing.user.domain.User;
import com.keepgoing.keepgoing.user.domain.UserOAuthAccount;
import com.keepgoing.keepgoing.user.repository.UserOAuthAccountRepository;
import com.keepgoing.keepgoing.user.repository.UserRepository;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class OAuthLoginService {

    private final UserOAuthAccountRepository userOAuthAccountRepository;
    private final UserRepository userRepository;

    // 동시성 문제 발생 가능
    // 그러나 나중에 문제 발생 시 해결하는 방향성으로 나둠
    @Transactional
    public void processOAuthLogin(
            String email,
            String providerUserId,
            String avatarUrl,
            String name
    ) {
        Optional<UserOAuthAccount> userOAuthAccountOpt
                = userOAuthAccountRepository.findByProviderAndProviderUserId(OAuthProvider.GOOGLE, providerUserId);

        if (userOAuthAccountOpt.isEmpty()) {
            Optional<User> userOpt = userRepository.findByEmail(email);
            User user = userOpt.orElseGet(() ->
                    userRepository.save(User.create(email, name))
            );
            UserOAuthAccount newUserOAuthAccount = UserOAuthAccount.create(
                    user,
                    OAuthProvider.GOOGLE,
                    providerUserId,
                    email,
                    avatarUrl
            );
            userOAuthAccountRepository.save(newUserOAuthAccount);
        } else {
            userOAuthAccountOpt.get().updateProfile(email, avatarUrl);
        }
    }
}
