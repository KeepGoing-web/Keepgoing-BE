package com.keepgoing.keepgoing.activity.service;

import static com.keepgoing.keepgoing.support.UserFixture.user;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.verifyNoMoreInteractions;

import com.keepgoing.keepgoing.activity.domain.ActivityEvent;
import com.keepgoing.keepgoing.activity.domain.ActivityEventType;
import com.keepgoing.keepgoing.activity.repository.ActivityEventRepository;
import com.keepgoing.keepgoing.note.domain.Note;
import com.keepgoing.keepgoing.note.domain.NoteVisibility;
import com.keepgoing.keepgoing.user.domain.User;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class ActivityEventRecordTest {

	private static final LocalDate FIXED_DATE = LocalDate.of(2026, 4, 20);

	@Mock
	ActivityEventRepository activityEventRepository;

	@Spy
	Clock clock = Clock.fixed(Instant.parse("2026-04-20T00:00:00Z"), ActivityDashboardService.KST);

	@InjectMocks
	ActivityEventRecord activityEventRecord;

		@Nested
	@DisplayName("노트 생성 활동 기록")
	class RecordNoteCreated {

		@Test
		@DisplayName("고정된 KST 기준 activityDate와 NOTE_CREATED 타입으로 이벤트를 저장한다")
		void recordsCreatedEvent() {
			// given
			User author = user(1L);
			Note note = Note.create(author, null, "제목", "내용", NoteVisibility.PRIVATE, false);

			// when
			activityEventRecord.recordNoteCreated(author, note);

			// then
			assertSavedEvent(author, note, ActivityEventType.NOTE_CREATED);
		}
	}

	@Nested
	@DisplayName("노트 수정 활동 기록")
	class RecordNoteUpdated {

		@Test
		@DisplayName("고정된 KST 기준 activityDate와 NOTE_UPDATED 타입으로 이벤트를 저장한다")
		void recordsUpdatedEvent() {
			// given
			User author = user(1L);
			Note note = Note.create(author, null, "제목", "내용", NoteVisibility.PRIVATE, false);

			// when
			activityEventRecord.recordNoteUpdated(author, note);

			// then
			assertSavedEvent(author, note, ActivityEventType.NOTE_UPDATED);
		}
	}

	private void assertSavedEvent(User author, Note note, ActivityEventType expectedType) {
		ArgumentCaptor<ActivityEvent> captor = ArgumentCaptor.forClass(ActivityEvent.class);
		then(activityEventRepository).should().save(captor.capture());

		ActivityEvent saved = captor.getValue();
		assertThat(saved.getUser()).isEqualTo(author);
		assertThat(saved.getNote()).isEqualTo(note);
		assertThat(saved.getType()).isEqualTo(expectedType);
		assertThat(saved.getActivityDate()).isEqualTo(FIXED_DATE);
		verifyNoMoreInteractions(activityEventRepository);
	}
}
