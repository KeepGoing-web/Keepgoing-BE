package com.keepgoing.keepgoing.activity.service.dto;

import java.time.LocalDate;

public record ActivityDashboardQuery(
		LocalDate from,
		LocalDate to
) {
}
