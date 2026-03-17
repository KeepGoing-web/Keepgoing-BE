package com.keepgoing.keepgoing.auth.service;

import com.keepgoing.keepgoing.auth.AuthMapper;
import com.keepgoing.keepgoing.auth.security.CustomUserDetails;
import com.keepgoing.keepgoing.auth.service.dto.LoginCommand;
import com.keepgoing.keepgoing.auth.service.dto.LoginResult;
import com.keepgoing.keepgoing.auth.service.dto.MyInfoResult;
import com.keepgoing.keepgoing.auth.service.dto.SignupCommand;
import com.keepgoing.keepgoing.auth.service.dto.SignupResult;
import com.keepgoing.keepgoing.global.common.error.BusinessException;
import com.keepgoing.keepgoing.global.common.error.ErrorCode;
import com.keepgoing.keepgoing.global.security.jwt.JwtProvider;
import com.keepgoing.keepgoing.user.domain.User;
import com.keepgoing.keepgoing.user.domain.UserPasswordCredential;
import com.keepgoing.keepgoing.user.repository.UserPasswordCredentialRepository;
import com.keepgoing.keepgoing.user.repository.UserRepository;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class AuthService {

	private final AuthenticationManager authenticationManager;
	private final JwtProvider jwtProvider;

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

	@Transactional
	public LoginResult login(LoginCommand command) {
		UsernamePasswordAuthenticationToken unauthenticated =
				UsernamePasswordAuthenticationToken.unauthenticated(command.email(), command.rawPassword());
		Authentication authenticate = authenticationManager.authenticate(unauthenticated);

		CustomUserDetails principal = (CustomUserDetails) authenticate.getPrincipal();
		List<String> roles = principal.getRoles();

		Long userId = principal.getUserId();
		String accessToken = jwtProvider.generateAccessToken(userId, roles);
		String refreshToken = jwtProvider.generateRefreshToken(userId);

		String email = principal.getUsername();
		return new LoginResult(accessToken, refreshToken, userId, email);
	}

	@Transactional(readOnly = true)
	public MyInfoResult getMyInfo(Long userId) {
		User user = userRepository.findById(userId)
				.orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));

		return new MyInfoResult(userId, user.getEmail(), user.getName(), user.getRole());
	}
}
