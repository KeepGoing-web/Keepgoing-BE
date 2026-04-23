package com.keepgoing.keepgoing.activity.service;

import com.keepgoing.keepgoing.activity.domain.ActivityEvent;
import com.keepgoing.keepgoing.activity.repository.ActivityEventRepository;
import com.keepgoing.keepgoing.activity.service.dto.ActivityCalendarDay;
import com.keepgoing.keepgoing.activity.service.dto.ActivityDashboardQuery;
import com.keepgoing.keepgoing.activity.service.dto.ActivityDashboardResult;
import com.keepgoing.keepgoing.activity.service.dto.ActivityStreak;
import com.keepgoing.keepgoing.activity.service.dto.ActivitySummary;
import com.keepgoing.keepgoing.global.common.error.BusinessException;
import com.keepgoing.keepgoing.global.common.error.ErrorCode;
import java.time.Clock;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class ActivityDashboardService {

	public static final String TIME_ZONE = "Asia/Seoul";
	public static final ZoneId KST = ZoneId.of(TIME_ZONE);
	public static final String BACKFILL_POLICY = "CREATE_ONLY";

	private final ActivityEventRepository activityEventRepository;
	private final Clock clock;

	@Transactional(readOnly = true)
	public ActivityDashboardResult getDashboard(Long userId, ActivityDashboardQuery query) {
		LocalDate from = query.from();
		LocalDate to = query.to();
		validateRange(from, to);

		List<ActivityEvent> eventsInRange =
				activityEventRepository.findByUser_IdAndActivityDateBetween(userId, from, to);

		List<ActivityCalendarDay> calendar = createCalendar(eventsInRange, from, to);

		int totalCount = calendar.stream()
				.mapToInt(ActivityCalendarDay::count)
				.sum();

		int activeDays = (int) calendar.stream()
				.filter(day -> day.count() > 0)
				.count();

		List<LocalDate> activeDates = activityEventRepository.findDistinctActivityDatesByUserId(userId);

		ActivityStreak streak = ActivityStreak.fromActiveDates(activeDates, LocalDate.now(clock));

		return ActivityDashboardResult.builder()
				.from(from)
				.to(to)
				.timezone(TIME_ZONE)
				.calendar(calendar)
				.summary(new ActivitySummary(totalCount, activeDays))
				.streak(streak)
				.backfillPolicy(BACKFILL_POLICY)
				.build();
	}

	private static void validateRange(LocalDate from, LocalDate to) {
		if (from == null || to == null || from.isAfter(to) || to.isAfter(from.plusYears(1))) {
			throw new BusinessException(ErrorCode.INVALID_INPUT);
		}
	}

	private List<ActivityCalendarDay> createCalendar(
			List<ActivityEvent> eventsInRange,
			LocalDate from,
			LocalDate to
	) {
		Map<LocalDate, Long> countByDate = eventsInRange.stream()
				.collect(Collectors.groupingBy(
						ActivityEvent::getActivityDate,
						Collectors.counting()
				));

		return from.datesUntil(to.plusDays(1))
				.map(date -> {
					int count = countByDate.getOrDefault(date, 0L).intValue();
					return ActivityCalendarDay.of(date, count);
				})
				.toList();
	}
}
