package com.keepgoing.keepgoing.activity.service.dto;

import java.time.LocalDate;
import java.util.List;
import java.util.Set;

public record ActivityStreak(
		int current,
		int longest,
		String scope
) {
	public static final String ALL_TIME = "ALL_TIME";

	public static ActivityStreak fromActiveDates(List<LocalDate> activeDates, LocalDate today) {
		if (activeDates.isEmpty()) {
			return new ActivityStreak(0, 0, "ALL_TIME");
		}

		Set<LocalDate> activeDateSet = Set.copyOf(activeDates);

		int current = 0;
		LocalDate cursor = activeDateSet.contains(today) ? today : today.minusDays(1);
		while (activeDateSet.contains(cursor)) {
			current++;
			cursor = cursor.minusDays(1);
		}

		int longest = 1;
		int currentRun = 1;

		for (int i = 1; i < activeDates.size(); i++) {
			LocalDate previous = activeDates.get(i - 1);
			LocalDate currentDate = activeDates.get(i);

			if (previous.plusDays(1).equals(currentDate)) {
				currentRun++;
				longest = Math.max(longest, currentRun);
			} else {
				currentRun = 1;
			}
		}

		return new ActivityStreak(current, longest, ALL_TIME);
	}
}
