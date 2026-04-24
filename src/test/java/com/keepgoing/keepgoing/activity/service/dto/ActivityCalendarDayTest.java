package com.keepgoing.keepgoing.activity.service.dto;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDate;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

class ActivityCalendarDayTest {

	private static final LocalDate DATE = LocalDate.of(2026, 4, 20);

	@Nested
	@DisplayName("활동 강도 레벨 계산")
	class Of {

		@Test
		@DisplayName("count가 0이면 level은 0이다")
		void returnsLevelZeroWhenCountIsZero() {
			assertThat(ActivityCalendarDay.of(DATE, 0)).isEqualTo(new ActivityCalendarDay(DATE, 0, 0));
		}

		@Test
		@DisplayName("count가 1이면 level은 1이다")
		void returnsLevelOneWhenCountIsOne() {
			assertThat(ActivityCalendarDay.of(DATE, 1)).isEqualTo(new ActivityCalendarDay(DATE, 1, 1));
		}

		@Test
		@DisplayName("count가 2 또는 3이면 level은 2이다")
		void returnsLevelTwoForTwoOrThreeCounts() {
			assertThat(ActivityCalendarDay.of(DATE, 2)).isEqualTo(new ActivityCalendarDay(DATE, 2, 2));
			assertThat(ActivityCalendarDay.of(DATE, 3)).isEqualTo(new ActivityCalendarDay(DATE, 3, 2));
		}

		@Test
		@DisplayName("count가 4 또는 5이면 level은 3이다")
		void returnsLevelThreeForFourOrFiveCounts() {
			assertThat(ActivityCalendarDay.of(DATE, 4)).isEqualTo(new ActivityCalendarDay(DATE, 4, 3));
			assertThat(ActivityCalendarDay.of(DATE, 5)).isEqualTo(new ActivityCalendarDay(DATE, 5, 3));
		}

		@Test
		@DisplayName("count가 6 이상이면 level은 4이다")
		void returnsLevelFourWhenCountIsSixOrMore() {
			assertThat(ActivityCalendarDay.of(DATE, 6)).isEqualTo(new ActivityCalendarDay(DATE, 6, 4));
		}
	}
}
