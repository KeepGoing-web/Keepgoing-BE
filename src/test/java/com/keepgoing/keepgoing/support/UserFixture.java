package com.keepgoing.keepgoing.support;

import com.keepgoing.keepgoing.user.domain.User;
import org.springframework.test.util.ReflectionTestUtils;

public final class UserFixture {

	private static final String DEFAULT_EMAIL = "test@test.com";
	private static final String DEFAULT_NAME = "tester";

	private UserFixture() {
	}

	public static User user(Long id) {
		return user(id, DEFAULT_EMAIL, DEFAULT_NAME);
	}

	public static User user(Long id, String email, String name) {
		User user = User.create(email, name);
		ReflectionTestUtils.setField(user, "id", id);
		return user;
	}
}
