package com.keepgoing.keepgoing.auth.service;

import com.keepgoing.keepgoing.auth.AuthMapper;
import com.keepgoing.keepgoing.global.common.error.BusinessException;
import com.keepgoing.keepgoing.global.common.error.ErrorCode;
import com.keepgoing.keepgoing.user.domain.User;
import com.keepgoing.keepgoing.user.domain.UserPasswordCredential;
import com.keepgoing.keepgoing.user.repository.UserPasswordCredentialRepository;
import com.keepgoing.keepgoing.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AuthService {

    private final UserRepository userRepository;
    private final UserPasswordCredentialRepository credentialRepository;
    private final PasswordEncoder passwordEncoder;
    private final AuthMapper authMapper;

    @Transactional
    public SignupResult signup(SignupCommand command) {
        // 중복 이메일 체크
        if (userRepository.existsByEmail(command.email())) {
            throw new BusinessException(ErrorCode.USER_ALREADY_EXISTS);
        }

        // User 생성 후 저장
        User user = User.create(command.email(), command.name());
        userRepository.save(user);

        // encodedPassword 생성 후 저장
        UserPasswordCredential credential = UserPasswordCredential.create(
                user,
                command.email(),
                passwordEncoder.encode(command.rawPassword())
        );
        credentialRepository.save(credential);

        return authMapper.toSignupResult(user);
    }
}
