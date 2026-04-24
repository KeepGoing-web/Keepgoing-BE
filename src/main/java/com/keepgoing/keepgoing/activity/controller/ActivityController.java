package com.keepgoing.keepgoing.activity.controller;

import com.keepgoing.keepgoing.activity.controller.dto.ActivityDashboardResponse;
import com.keepgoing.keepgoing.activity.service.ActivityDashboardService;
import com.keepgoing.keepgoing.activity.service.dto.ActivityDashboardQuery;
import com.keepgoing.keepgoing.activity.service.dto.ActivityDashboardResult;
import com.keepgoing.keepgoing.global.api.response.ApiResponse;
import java.time.LocalDate;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.format.annotation.DateTimeFormat.ISO;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/activities")
public class ActivityController {

	private final ActivityDashboardService activityDashboardService;

	@GetMapping("/me/dashboard")
	public ResponseEntity<ApiResponse<ActivityDashboardResponse>> getMyActivityDashboard(
			@RequestParam @DateTimeFormat(iso = ISO.DATE) LocalDate from,
			@RequestParam @DateTimeFormat(iso = ISO.DATE) LocalDate to,
			@AuthenticationPrincipal Long userId
	) {
		ActivityDashboardResult result = activityDashboardService.getDashboard(
				userId,
				new ActivityDashboardQuery(from, to)
		);
		ActivityDashboardResponse response = ActivityDashboardResponse.from(result);

		return ResponseEntity.ok(ApiResponse.success(response));
	}
}
