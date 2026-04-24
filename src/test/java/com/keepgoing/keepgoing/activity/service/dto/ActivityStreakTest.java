package com.keepgoing.keepgoing.activity.service.dto;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDate;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

class ActivityStreakTest {

	@Nested
	@DisplayName("연속 활동 계산")
	class FromActiveDates {

		@Test
		@DisplayName("활동 날짜가 없으면 current와 longest를 0으로 반환한다")
		void returnsZeroWhenNoActiveDatesExist() {
			// when
			ActivityStreak streak = ActivityStreak.fromActiveDates(List.of(), LocalDate.of(2026, 4, 20));

			// then
			assertThat(streak).isEqualTo(new ActivityStreak(0, 0, ActivityStreak.ALL_TIME));
		}

		@Test
		@DisplayName("오늘 활동이 있으면 오늘부터 current streak를 계산한다")
		void calculatesCurrentStreakFromTodayWhenTodayIsActive() {
			// given
			LocalDate today = LocalDate.of(2026, 4, 20);
			List<LocalDate> activeDates = List.of(
					LocalDate.of(2026, 4, 10),
					LocalDate.of(2026, 4, 11),
					LocalDate.of(2026, 4, 18),
					LocalDate.of(2026, 4, 19),
					today
			);

			// when
			ActivityStreak streak = ActivityStreak.fromActiveDates(activeDates, today);

			// then
			assertThat(streak).isEqualTo(new ActivityStreak(3, 3, ActivityStreak.ALL_TIME));
		}

		@Test
		@DisplayName("오늘 활동이 없으면 어제부터 current streak를 계산하고 가장 긴 streak도 함께 반환한다")
		void calculatesCurrentStreakFromYesterdayWhenTodayIsInactive() {
			// given
			LocalDate today = LocalDate.of(2026, 4, 20);
			List<LocalDate> activeDates = List.of(
					LocalDate.of(2026, 4, 1),
					LocalDate.of(2026, 4, 2),
					LocalDate.of(2026, 4, 3),
					LocalDate.of(2026, 4, 18),
					LocalDate.of(2026, 4, 19)
			);

			// when
			ActivityStreak streak = ActivityStreak.fromActiveDates(activeDates, today);

			// then
			assertThat(streak).isEqualTo(new ActivityStreak(2, 3, ActivityStreak.ALL_TIME));
		}

		@Test
		@DisplayName("마지막 활동이 그저께면 current streak는 0이다")
		void returnsZeroCurrentWhenLastActivityWasTwoDaysAgo() {
			// given
			LocalDate today = LocalDate.of(2026, 4, 20);
			List<LocalDate> activeDates = List.of(
					LocalDate.of(2026, 4, 1),
					LocalDate.of(2026, 4, 2),
					LocalDate.of(2026, 4, 18)
			);

			// when
			ActivityStreak streak = ActivityStreak.fromActiveDates(activeDates, today);

			// then
			assertThat(streak).isEqualTo(new ActivityStreak(0, 2, ActivityStreak.ALL_TIME));
		}
	}
}
