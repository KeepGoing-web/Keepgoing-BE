package com.keepgoing.keepgoing.auth.security;

import com.keepgoing.keepgoing.user.domain.User;
import com.keepgoing.keepgoing.user.domain.UserPasswordCredential;
import com.keepgoing.keepgoing.user.repository.UserPasswordCredentialRepository;
import com.keepgoing.keepgoing.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class CustomUserDetailsService implements UserDetailsService {

    private static final String USER_NOT_FOUND = "Authentication failed";

    private final UserRepository userRepository;
    private final UserPasswordCredentialRepository credentialRepository;

    @Override
    @Transactional(readOnly = true)
    public UserDetails loadUserByUsername(String email) throws UsernameNotFoundException {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> {
                    log.debug("User not found: {}", email);
                    return new UsernameNotFoundException(USER_NOT_FOUND);
                });

        UserPasswordCredential credential = credentialRepository.findByUser(user)
                .orElseThrow(() -> {
                    log.debug("Password Credential not found: {}", email);
                    return new UsernameNotFoundException(USER_NOT_FOUND);
                });

        return CustomUserDetails.of(user, credential.getPasswordHash());
    }
}
