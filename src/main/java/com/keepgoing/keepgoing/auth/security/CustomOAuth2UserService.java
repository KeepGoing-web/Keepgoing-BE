package com.keepgoing.keepgoing.auth.security;

import com.keepgoing.keepgoing.auth.service.OAuthLoginService;
import com.keepgoing.keepgoing.auth.service.dto.OAuthUserPrincipal;
import lombok.RequiredArgsConstructor;
import org.springframework.security.oauth2.client.userinfo.DefaultOAuth2UserService;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserRequest;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.stereotype.Service;

// TODO: 외부 요청에 의한 응답값 null 체크 로직 추가
@Service
@RequiredArgsConstructor
public class CustomOAuth2UserService extends DefaultOAuth2UserService {

	private final OAuthLoginService oAuthLoginService;

	@Override
	public OAuth2User loadUser(OAuth2UserRequest userRequest) throws OAuth2AuthenticationException {
		OAuth2User oAuth2User = loadProviderUser(userRequest);
		GoogleOAuthAttributes attr = GoogleOAuthAttributes.from(oAuth2User);
		OAuthUserPrincipal principal =
				oAuthLoginService.processOAuthLogin(attr.email(), attr.providerUserId(), attr.avatarUrl(), attr.name());
		return new CustomOAuth2User(oAuth2User, principal.userId(), principal.role());
	}

	protected OAuth2User loadProviderUser(OAuth2UserRequest  userRequest) {
		return super.loadUser(userRequest);
	}
}
