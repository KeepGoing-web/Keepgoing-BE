package com.keepgoing.keepgoing.auth.security;

import com.keepgoing.keepgoing.auth.service.OAuthLoginService;
import com.keepgoing.keepgoing.auth.service.dto.OAuthUserPrincipal;
import lombok.RequiredArgsConstructor;
import org.springframework.security.oauth2.client.oidc.userinfo.OidcUserRequest;
import org.springframework.security.oauth2.client.oidc.userinfo.OidcUserService;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class CustomOidcUserService extends OidcUserService {

	private final OAuthLoginService oAuthLoginService;

	@Override
	public OidcUser loadUser(OidcUserRequest userRequest) throws OAuth2AuthenticationException {
		OidcUser oidcUser = loadProviderUser(userRequest);

		GoogleOAuthAttributes attr = GoogleOAuthAttributes.from(oidcUser);

		OAuthUserPrincipal principal = oAuthLoginService.processOAuthLogin(
				attr.email(),
				attr.providerUserId(),
				attr.avatarUrl(),
				attr.name()
		);
		return new CustomOidcUser(oidcUser, principal.userId(), principal.role());
	}

	protected OidcUser loadProviderUser(OidcUserRequest userRequest) {
		return super.loadUser(userRequest);
	}
}
