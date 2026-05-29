package com.keepgoing.keepgoing.auth;

import java.util.HashMap;
import java.util.List;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.core.user.DefaultOAuth2User;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class OAuthTestFixtures {

	public static final String EMAIL = "user@gmail.com";
	public static final String USER_ID = "google-123";
	public static final String PICTURE = "https://image.test/profile.png";
	public static final String NAME = "홍길동";

	public static DefaultOAuth2User googleUser() {
		return googleUserBuilder()
				.build()
				.oauth2User();
	}

	public static GoogleUserFixture.GoogleUserFixtureBuilder googleUserBuilder() {
		return GoogleUserFixture.builder();
	}


	@Getter
	@Builder(toBuilder = true)
	public static class GoogleUserFixture {

		@Builder.Default
		private String sub = "google-123";

		@Builder.Default
		private String email = "user@gmail.com";

		@Builder.Default
		private String name = "홍길동";

		@Builder.Default
		private String picture = "https://image.test/profile.png";

		public DefaultOAuth2User oauth2User() {
			HashMap<String, Object> attr = new HashMap<>();

			if (sub != null) {
				attr.put("sub", sub);
			}
			if (email != null) {
				attr.put("email", email);
			}
			if (name != null) {
				attr.put("name", name);
			}
			if (picture != null) {
				attr.put("picture", picture);
			}

			String nameAttrKey = attr.containsKey("sub") ? "sub" : "email";

			return new DefaultOAuth2User(
					List.of(new SimpleGrantedAuthority("ROLE_USER")),
					attr,
					nameAttrKey
			);
		}
	}
}
