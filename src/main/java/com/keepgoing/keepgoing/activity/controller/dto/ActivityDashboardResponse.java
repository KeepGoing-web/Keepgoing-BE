package com.keepgoing.keepgoing.activity.controller.dto;

import com.keepgoing.keepgoing.activity.service.dto.ActivityCalendarDay;
import com.keepgoing.keepgoing.activity.service.dto.ActivityDashboardResult;
import com.keepgoing.keepgoing.activity.service.dto.ActivityStreak;
import com.keepgoing.keepgoing.activity.service.dto.ActivitySummary;
import java.time.LocalDate;
import java.util.List;

public record ActivityDashboardResponse(
		LocalDate from,
		LocalDate to,
		String timezone,
		List<ActivityCalendarDay> calendar,
		ActivitySummary summary,
		ActivityStreak streak
) {
	public static ActivityDashboardResponse from(ActivityDashboardResult result) {
		return new ActivityDashboardResponse(
				result.from(),
				result.to(),
				result.timezone(),
				result.calendar(),
				result.summary(),
				result.streak()
		);
	}
}
