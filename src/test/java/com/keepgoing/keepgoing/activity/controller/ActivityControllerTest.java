package com.keepgoing.keepgoing.activity.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.BDDMockito.willThrow;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.keepgoing.keepgoing.activity.service.ActivityDashboardService;
import com.keepgoing.keepgoing.activity.service.dto.ActivityCalendarDay;
import com.keepgoing.keepgoing.activity.service.dto.ActivityDashboardQuery;
import com.keepgoing.keepgoing.activity.service.dto.ActivityDashboardResult;
import com.keepgoing.keepgoing.activity.service.dto.ActivityStreak;
import com.keepgoing.keepgoing.activity.service.dto.ActivitySummary;
import com.keepgoing.keepgoing.global.api.exception.GlobalExceptionHandler;
import com.keepgoing.keepgoing.global.common.error.BusinessException;
import com.keepgoing.keepgoing.global.common.error.ErrorCode;
import com.keepgoing.keepgoing.global.config.JacksonConfig;
import com.keepgoing.keepgoing.global.security.jwt.JwtAuthenticationFilter;
import java.time.LocalDate;
import java.util.List;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(ActivityController.class)
@AutoConfigureMockMvc(addFilters = false)
@Import({GlobalExceptionHandler.class, JacksonConfig.class})
class ActivityControllerTest {

	@Autowired
	MockMvc mockMvc;

	@MockitoBean
	ActivityDashboardService activityDashboardService;

	@MockitoBean
	JwtAuthenticationFilter jwtAuthenticationFilter;

	@AfterEach
	void tearDown() {
		SecurityContextHolder.clearContext();
	}

	@Nested
	@DisplayName("GET /api/activities/me/dashboard")
	class GetMyDashboard {

		@Test
		@DisplayName("유효한 요청이면 대시보드 응답을 반환한다")
		void returnsDashboard() throws Exception {
			// given
			Long userId = 1L;
			LocalDate from = LocalDate.of(2026, 4, 16);
			LocalDate to = LocalDate.of(2026, 4, 20);
			ActivityDashboardResult result = ActivityDashboardResult.builder()
					.from(from)
					.to(to)
					.timezone("Asia/Seoul")
					.calendar(List.of(
							ActivityCalendarDay.of(from, 1),
							ActivityCalendarDay.of(from.plusDays(1), 0)
					))
					.summary(new ActivitySummary(1, 1))
					.streak(new ActivityStreak(1, 3, ActivityStreak.ALL_TIME))
					.backfillPolicy("CREATE_ONLY")
					.build();

			given(activityDashboardService.getDashboard(any(), any(ActivityDashboardQuery.class)))
					.willReturn(result);
			mockLoginUser(userId);

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
					.andExpect(jsonPath("$.data.summary.totalCount").value(1))
					.andExpect(jsonPath("$.data.summary.activeDays").value(1))
					.andExpect(jsonPath("$.data.streak.current").value(1))
					.andExpect(jsonPath("$.data.streak.longest").value(3))
					.andExpect(jsonPath("$.data.streak.scope").value("ALL_TIME"));

			then(activityDashboardService).should().getDashboard(argThat(id -> id.equals(userId)), argThat(query ->
					query.from().equals(from) && query.to().equals(to)
			));
		}

		@Test
		@DisplayName("조회 기간이 잘못되면 400과 INVALID_INPUT을 반환한다")
		void returnsBadRequestWhenRangeIsInvalid() throws Exception {
			// given
			Long userId = 1L;
			given(activityDashboardService.getDashboard(any(), any(ActivityDashboardQuery.class)))
					.willThrow(new BusinessException(ErrorCode.INVALID_INPUT));
			mockLoginUser(userId);

			// when & then
			mockMvc.perform(get("/api/activities/me/dashboard")
							.param("from", "2026-04-20")
							.param("to", "2026-04-16"))
					.andExpect(status().isBadRequest())
					.andExpect(jsonPath("$.success").value(false))
					.andExpect(jsonPath("$.error.code").value(ErrorCode.INVALID_INPUT.toString()));

			then(activityDashboardService).should().getDashboard(argThat(id -> id.equals(userId)), argThat(query ->
					query.from().equals(LocalDate.of(2026, 4, 20))
							&& query.to().equals(LocalDate.of(2026, 4, 16))
			));
		}

		@Test
		@DisplayName("from 파라미터가 없으면 400과 INVALID_INPUT을 반환한다")
		void returnsBadRequestWhenFromIsMissing() throws Exception {
			// given
			mockLoginUser(1L);

			// when & then
			mockMvc.perform(get("/api/activities/me/dashboard")
							.param("to", "2026-04-20"))
					.andExpect(status().isBadRequest())
					.andExpect(jsonPath("$.success").value(false))
					.andExpect(jsonPath("$.error.code").value(ErrorCode.INVALID_INPUT.toString()));

			then(activityDashboardService).shouldHaveNoInteractions();
		}

		@Test
		@DisplayName("날짜 형식이 올바르지 않으면 400과 INVALID_INPUT을 반환한다")
		void returnsBadRequestWhenDateFormatIsMalformed() throws Exception {
			// given
			mockLoginUser(1L);

			// when & then
			mockMvc.perform(get("/api/activities/me/dashboard")
							.param("from", "2026/04/16")
							.param("to", "2026-04-20"))
					.andExpect(status().isBadRequest())
					.andExpect(jsonPath("$.success").value(false))
					.andExpect(jsonPath("$.error.code").value(ErrorCode.INVALID_INPUT.toString()));

			then(activityDashboardService).shouldHaveNoInteractions();
		}
	}

	private void mockLoginUser(Long userId) {
		SecurityContextHolder.getContext()
				.setAuthentication(new UsernamePasswordAuthenticationToken(userId, null, List.of()));
	}
}
