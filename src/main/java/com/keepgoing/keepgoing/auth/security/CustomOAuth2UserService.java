package com.keepgoing.keepgoing.auth.security;

import com.keepgoing.keepgoing.auth.service.OAuthLoginService;
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
        OAuth2User oAuth2User = super.loadUser(userRequest);

        String providerUserId = oAuth2User.getAttribute("sub");
        String email = oAuth2User.getAttribute("email");
        if (providerUserId == null || email == null) {
            throw new OAuth2AuthenticationException("Google 필수 정보 누락");
        }
        String name = oAuth2User.getAttribute("name");
        if (name == null) {
            name = email.split("@")[0];
        }
        String avatarUrl = oAuth2User.getAttribute("picture");

        oAuthLoginService.processOAuthLogin(email, providerUserId, avatarUrl, name);
        return oAuth2User;
    }
}
