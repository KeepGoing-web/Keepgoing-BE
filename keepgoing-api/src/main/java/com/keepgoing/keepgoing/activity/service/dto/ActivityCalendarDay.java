package com.keepgoing.keepgoing.activity.service.dto;

import java.time.LocalDate;

public record ActivityCalendarDay(
		LocalDate date,
		int count,
		int level
) {

	public static ActivityCalendarDay of(LocalDate date, int count) {
		return new ActivityCalendarDay(date, count, calculateLevel(count));
	}

	private static int calculateLevel(int count) {
		if (count == 0) {
			return 0;
		}
		if (count == 1) {
			return 1;
		}
		if (count <= 3) {
			return 2;
		}
		if (count <= 5) {
			return 3;
		}
		return 4;
	}
}
