package com.keepgoing.keepgoing.activity.service;

import com.keepgoing.keepgoing.activity.domain.ActivityEvent;
import com.keepgoing.keepgoing.activity.domain.ActivityEventType;
import com.keepgoing.keepgoing.activity.repository.ActivityEventRepository;
import com.keepgoing.keepgoing.note.domain.Note;
import com.keepgoing.keepgoing.user.domain.User;
import java.time.Clock;
import java.time.LocalDate;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class ActivityEventRecord {

	private final ActivityEventRepository activityEventRepository;
	private final Clock clock;

	@Transactional
	public void recordNoteCreated(User user, Note note) {
		ActivityEvent activityEvent = ActivityEvent.create(
				user,
				note,
				ActivityEventType.NOTE_CREATED,
				LocalDate.now(clock)
		);
		activityEventRepository.save(activityEvent);
	}

	@Transactional
	public void recordNoteUpdated(User user, Note note) {
		ActivityEvent activityEvent = ActivityEvent.create(
				user,
				note,
				ActivityEventType.NOTE_UPDATED,
				LocalDate.now(clock)
		);
		activityEventRepository.save(activityEvent);
	}
}
