package com.keepgoing.keepgoing.activity.service.dto;

import java.time.LocalDate;
import java.util.List;
import lombok.Builder;

@Builder
public record ActivityDashboardResult(
		LocalDate from,
		LocalDate to,
		String timezone,
		List<ActivityCalendarDay> calendar,
		ActivitySummary summary,
		ActivityStreak streak,
		String backfillPolicy
) {
}
