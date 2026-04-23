package com.keepgoing.keepgoing.activity;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.keepgoing.keepgoing.activity.domain.ActivityEvent;
import com.keepgoing.keepgoing.activity.domain.ActivityEventType;
import com.keepgoing.keepgoing.activity.repository.ActivityEventRepository;
import com.keepgoing.keepgoing.note.domain.Note;
import com.keepgoing.keepgoing.note.domain.NoteVisibility;
import com.keepgoing.keepgoing.note.repository.NoteRepository;
import com.keepgoing.keepgoing.user.domain.User;
import com.keepgoing.keepgoing.user.domain.UserRole;
import com.keepgoing.keepgoing.user.repository.UserRepository;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Primary;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest
@AutoConfigureMockMvc(addFilters = false)
@Transactional
@Import(ActivityIntegrationTest.FixedClockConfig.class)
class ActivityIntegrationTest {

	private static final ZoneId KST = ZoneId.of("Asia/Seoul");

	@Autowired
	MockMvc mockMvc;

	@Autowired
	UserRepository userRepository;

	@Autowired
	NoteRepository noteRepository;

	@Autowired
	ActivityEventRepository activityEventRepository;

	@AfterEach
	void tearDown() {
		SecurityContextHolder.clearContext();
	}

	@Nested
	@DisplayName("GET /api/activities/me/dashboard")
	class GetMyDashboard {

		@Test
		@DisplayName("활동 데이터가 있으면 응답과 계산 결과를 함께 반환한다")
		void returnsDashboardWithCalculatedFields() throws Exception {
			// given
			User author = saveUser("activity@test.com", "활동 사용자");
			Note note = saveNote(author, "기록용 노트");
			saveActivityEvents(author, note, List.of(
					LocalDate.of(2026, 4, 16),
					LocalDate.of(2026, 4, 18),
					LocalDate.of(2026, 4, 18),
					LocalDate.of(2026, 4, 19),
					LocalDate.of(2026, 4, 20)
			));
			mockLoginUser(author.getId());

			// when & then
			mockMvc.perform(get("/api/activities/me/dashboard")
							.param("from", "2026-04-16")
							.param("to", "2026-04-20"))
					.andExpect(status().isOk())
					.andExpect(jsonPath("$.success").value(true))
					.andExpect(jsonPath("$.data.from").value("2026-04-16"))
					.andExpect(jsonPath("$.data.to").value("2026-04-20"))
					.andExpect(jsonPath("$.data.timezone").value("Asia/Seoul"))
					.andExpect(jsonPath("$.data.calendar[0].date").value("2026-04-16"))
					.andExpect(jsonPath("$.data.calendar[0].count").value(1))
					.andExpect(jsonPath("$.data.calendar[1].count").value(0))
					.andExpect(jsonPath("$.data.calendar[2].count").value(2))
					.andExpect(jsonPath("$.data.calendar[2].level").value(2))
					.andExpect(jsonPath("$.data.summary.totalCount").value(5))
					.andExpect(jsonPath("$.data.summary.activeDays").value(4))
					.andExpect(jsonPath("$.data.streak.current").value(3))
					.andExpect(jsonPath("$.data.streak.longest").value(3))
					.andExpect(jsonPath("$.data.streak.scope").value("ALL_TIME"))
					.andExpect(jsonPath("$.data.backfillPolicy").value("CREATE_ONLY"));
		}

		@Test
		@DisplayName("조회 시작일이 종료일보다 늦으면 400과 INVALID_INPUT을 반환한다")
		void returnsBadRequestWhenRangeIsInvalid() throws Exception {
			// given
			User author = saveUser("invalid-range@test.com", "범위 사용자");
			mockLoginUser(author.getId());

			// when & then
			mockMvc.perform(get("/api/activities/me/dashboard")
							.param("from", "2026-04-20")
							.param("to", "2026-04-16"))
					.andExpect(status().isBadRequest())
					.andExpect(jsonPath("$.success").value(false))
					.andExpect(jsonPath("$.error.code").value("INVALID_INPUT"));
		}

		@Test
		@DisplayName("다른 사용자의 활동은 내 대시보드에 포함되지 않는다")
		void excludesOtherUsersActivities() throws Exception {
			// given
			User requester = saveUser("requester@test.com", "요청 사용자");
			Note requesterNote = saveNote(requester, "요청자 노트");
			saveActivityEvents(requester, requesterNote, List.of(
					LocalDate.of(2026, 4, 16),
					LocalDate.of(2026, 4, 19),
					LocalDate.of(2026, 4, 20)
			));

			User otherUser = saveUser("other@test.com", "다른 사용자");
			Note otherNote = saveNote(otherUser, "다른 사용자 노트");
			saveActivityEvents(otherUser, otherNote, List.of(
					LocalDate.of(2026, 4, 17),
					LocalDate.of(2026, 4, 18),
					LocalDate.of(2026, 4, 18),
					LocalDate.of(2026, 4, 20)
			));

			mockLoginUser(requester.getId());

			// when & then
			mockMvc.perform(get("/api/activities/me/dashboard")
							.param("from", "2026-04-16")
							.param("to", "2026-04-20"))
					.andExpect(status().isOk())
					.andExpect(jsonPath("$.success").value(true))
					.andExpect(jsonPath("$.data.calendar[0].count").value(1))
					.andExpect(jsonPath("$.data.calendar[1].count").value(0))
					.andExpect(jsonPath("$.data.calendar[2].count").value(0))
					.andExpect(jsonPath("$.data.calendar[3].count").value(1))
					.andExpect(jsonPath("$.data.calendar[4].count").value(1))
					.andExpect(jsonPath("$.data.summary.totalCount").value(3))
					.andExpect(jsonPath("$.data.summary.activeDays").value(3))
					.andExpect(jsonPath("$.data.streak.current").value(2))
					.andExpect(jsonPath("$.data.streak.longest").value(2))
					.andExpect(jsonPath("$.data.backfillPolicy").value("CREATE_ONLY"));
		}
	}

	private User saveUser(String email, String name) {
		return userRepository.saveAndFlush(User.create(email, name));
	}

	private Note saveNote(User author, String title) {
		return noteRepository.saveAndFlush(Note.create(author, null, title, "본문", NoteVisibility.PRIVATE, false));
	}

	private void saveActivityEvents(User author, Note note, List<LocalDate> dates) {
		activityEventRepository.saveAllAndFlush(
				dates.stream()
						.map(date -> ActivityEvent.create(author, note, ActivityEventType.NOTE_CREATED, date))
						.toList()
		);
	}

	private void mockLoginUser(Long userId) {
		Authentication authenticated = UsernamePasswordAuthenticationToken.authenticated(
				userId,
				null,
				List.of(new SimpleGrantedAuthority(UserRole.USER.toAuthority()))
		);
		SecurityContextHolder.getContext().setAuthentication(authenticated);
	}

	@TestConfiguration
	static class FixedClockConfig {

		@Bean
		@Primary
		Clock fixedClock() {
			return Clock.fixed(Instant.parse("2026-04-20T00:00:00Z"), KST);
		}
	}
}
