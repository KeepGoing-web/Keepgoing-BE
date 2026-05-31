package com.keepgoing.keepgoing.auth.service;

import com.keepgoing.keepgoing.auth.service.dto.OAuthUserPrincipal;
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
	public OAuthUserPrincipal processOAuthLogin(
			String email,
			String providerUserId,
			String avatarUrl,
			String name
	) {
		// TODO: 다중 OAuth Provider 사용 시 하드코딩 수정
		Optional<UserOAuthAccount> userOAuthAccountOpt
				= userOAuthAccountRepository.findByProviderAndProviderUserId(OAuthProvider.GOOGLE, providerUserId);

		User user;
		// OAuth 가입자 있는 경우
		if (userOAuthAccountOpt.isPresent()) {
			UserOAuthAccount userOAuthAccount = userOAuthAccountOpt.get();
			userOAuthAccount.updateProfile(email, avatarUrl);
			user = userOAuthAccount.getUser();
		} else { // 신규
			user = userRepository.findByEmail(email).orElseGet(() ->
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
		}

		return OAuthUserPrincipal.builder()
				.userId(user.getId())
				.role(user.getRole().toAuthority())
				.build();
	}
}
