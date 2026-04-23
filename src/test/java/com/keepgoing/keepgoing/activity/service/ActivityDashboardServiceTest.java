package com.keepgoing.keepgoing.activity.service;

import static com.keepgoing.keepgoing.support.UserFixture.user;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.verifyNoMoreInteractions;

import com.keepgoing.keepgoing.activity.domain.ActivityEvent;
import com.keepgoing.keepgoing.activity.domain.ActivityEventType;
import com.keepgoing.keepgoing.activity.repository.ActivityEventRepository;
import com.keepgoing.keepgoing.activity.service.dto.ActivityCalendarDay;
import com.keepgoing.keepgoing.activity.service.dto.ActivityDashboardQuery;
import com.keepgoing.keepgoing.activity.service.dto.ActivityDashboardResult;
import com.keepgoing.keepgoing.activity.service.dto.ActivityStreak;
import com.keepgoing.keepgoing.activity.service.dto.ActivitySummary;
import com.keepgoing.keepgoing.global.common.error.BusinessException;
import com.keepgoing.keepgoing.global.common.error.ErrorCode;
import com.keepgoing.keepgoing.note.domain.Note;
import com.keepgoing.keepgoing.note.domain.NoteVisibility;
import com.keepgoing.keepgoing.user.domain.User;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class ActivityDashboardServiceTest {

	@Mock
	ActivityEventRepository activityEventRepository;

	@Spy
	Clock clock = Clock.fixed(Instant.parse("2026-04-20T00:00:00Z"), ActivityDashboardService.KST);

	@InjectMocks
	ActivityDashboardService activityDashboardService;

	@Nested
	@DisplayName("대시보드 조회")
	class GetDashboard {

		@Test
		@DisplayName("기간별 캘린더, 요약, 스트릭을 함께 반환한다")
		void returnsCalendarSummaryAndStreak() {
			// given
			Long userId = 1L;
			LocalDate today = LocalDate.now(clock);
			LocalDate from = today.minusDays(4);
			LocalDate to = today;
			User author = user(userId);
			Note note = Note.create(author, null, "제목", "내용", NoteVisibility.PRIVATE, false);

			List<ActivityEvent> eventsInRange = List.of(
					activityEvent(author, note, from),
					activityEvent(author, note, from.plusDays(2)),
					activityEvent(author, note, from.plusDays(2)),
					activityEvent(author, note, from.plusDays(3)),
					activityEvent(author, note, to)
			);
			List<LocalDate> activeDates = List.of(
					from,
					from.plusDays(2),
					from.plusDays(3),
					to
			);

			given(activityEventRepository.findByUser_IdAndActivityDateBetween(userId, from, to))
					.willReturn(eventsInRange);
			given(activityEventRepository.findDistinctActivityDatesByUserId(userId))
					.willReturn(activeDates);

			// when
			ActivityDashboardResult result = activityDashboardService.getDashboard(userId, new ActivityDashboardQuery(from, to));

			// then
			assertThat(result.from()).isEqualTo(from);
			assertThat(result.to()).isEqualTo(to);
			assertThat(result.timezone()).isEqualTo(ActivityDashboardService.TIME_ZONE);
			assertThat(result.backfillPolicy()).isEqualTo(ActivityDashboardService.BACKFILL_POLICY);
			assertThat(result.calendar()).containsExactly(
					ActivityCalendarDay.of(from, 1),
					ActivityCalendarDay.of(from.plusDays(1), 0),
					ActivityCalendarDay.of(from.plusDays(2), 2),
					ActivityCalendarDay.of(from.plusDays(3), 1),
					ActivityCalendarDay.of(to, 1)
			);
			assertThat(result.summary()).isEqualTo(new ActivitySummary(5, 4));
			assertThat(result.streak()).isEqualTo(new ActivityStreak(3, 3, ActivityStreak.ALL_TIME));

			verify(activityEventRepository).findByUser_IdAndActivityDateBetween(userId, from, to);
			verify(activityEventRepository).findDistinctActivityDatesByUserId(userId);
			verifyNoMoreInteractions(activityEventRepository);
		}

		@Test
		@DisplayName("활동이 없으면 0 요약과 빈 스트릭을 반환한다")
		void returnsEmptyDashboardWhenNoActivityExists() {
			// given
			Long userId = 1L;
			LocalDate today = LocalDate.now(clock);
			LocalDate from = today.minusDays(2);
			LocalDate to = today;

			given(activityEventRepository.findByUser_IdAndActivityDateBetween(userId, from, to))
					.willReturn(List.of());
			given(activityEventRepository.findDistinctActivityDatesByUserId(userId))
					.willReturn(List.of());

			// when
			ActivityDashboardResult result = activityDashboardService.getDashboard(userId, new ActivityDashboardQuery(from, to));

			// then
			assertThat(result.calendar()).containsExactly(
					ActivityCalendarDay.of(from, 0),
					ActivityCalendarDay.of(from.plusDays(1), 0),
					ActivityCalendarDay.of(to, 0)
			);
			assertThat(result.summary()).isEqualTo(new ActivitySummary(0, 0));
			assertThat(result.streak()).isEqualTo(new ActivityStreak(0, 0, ActivityStreak.ALL_TIME));

			verify(activityEventRepository).findByUser_IdAndActivityDateBetween(userId, from, to);
			verify(activityEventRepository).findDistinctActivityDatesByUserId(userId);
			verifyNoMoreInteractions(activityEventRepository);
		}

		@Test
		@DisplayName("하루 범위 조회면 단일 날짜 캘린더와 요약을 반환한다")
		void returnsSingleDayCalendarWhenRangeHasOneDay() {
			// given
			Long userId = 1L;
			LocalDate day = LocalDate.of(2026, 4, 20);
			User author = user(userId);
			Note note = Note.create(author, null, "제목", "내용", NoteVisibility.PRIVATE, false);
			List<ActivityEvent> eventsInRange = List.of(activityEvent(author, note, day));

			given(activityEventRepository.findByUser_IdAndActivityDateBetween(userId, day, day))
					.willReturn(eventsInRange);
			given(activityEventRepository.findDistinctActivityDatesByUserId(userId))
					.willReturn(List.of(day));

			// when
			ActivityDashboardResult result = activityDashboardService.getDashboard(userId, new ActivityDashboardQuery(day, day));

			// then
			assertThat(result.calendar()).containsExactly(ActivityCalendarDay.of(day, 1));
			assertThat(result.summary()).isEqualTo(new ActivitySummary(1, 1));
			assertThat(result.streak()).isEqualTo(new ActivityStreak(1, 1, ActivityStreak.ALL_TIME));

			verify(activityEventRepository).findByUser_IdAndActivityDateBetween(userId, day, day);
			verify(activityEventRepository).findDistinctActivityDatesByUserId(userId);
			verifyNoMoreInteractions(activityEventRepository);
		}

		@Test
		@DisplayName("조회 시작일이 종료일보다 늦으면 INVALID_INPUT 예외가 발생한다")
		void throwsWhenRangeIsReversed() {
			// given
			LocalDate from = LocalDate.of(2026, 4, 21);
			LocalDate to = LocalDate.of(2026, 4, 20);

			// when & then
			assertThatThrownBy(() -> activityDashboardService.getDashboard(1L, new ActivityDashboardQuery(from, to)))
					.isInstanceOf(BusinessException.class)
					.hasFieldOrPropertyWithValue("errorCode", ErrorCode.INVALID_INPUT);

			verifyNoInteractions(activityEventRepository);
		}

		@Test
		@DisplayName("조회 기간에 null이 포함되면 INVALID_INPUT 예외가 발생한다")
		void throwsWhenRangeContainsNull() {
			// when & then
			assertThatThrownBy(() -> activityDashboardService.getDashboard(1L, new ActivityDashboardQuery(null, LocalDate.of(2026, 4, 20))))
					.isInstanceOf(BusinessException.class)
					.hasFieldOrPropertyWithValue("errorCode", ErrorCode.INVALID_INPUT);
			assertThatThrownBy(() -> activityDashboardService.getDashboard(1L, new ActivityDashboardQuery(LocalDate.of(2026, 4, 20), null)))
					.isInstanceOf(BusinessException.class)
					.hasFieldOrPropertyWithValue("errorCode", ErrorCode.INVALID_INPUT);

			verifyNoInteractions(activityEventRepository);
		}
	}

	private ActivityEvent activityEvent(User author, Note note, LocalDate activityDate) {
		return ActivityEvent.create(author, note, ActivityEventType.NOTE_CREATED, activityDate);
	}
}
