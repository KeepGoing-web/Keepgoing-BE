package com.keepgoing.keepgoing.activity.domain;

import com.keepgoing.keepgoing.activity.repository.ActivityEventRepository;
import com.keepgoing.keepgoing.note.domain.Note;
import com.keepgoing.keepgoing.user.domain.User;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EntityListeners;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.time.LocalDate;
import java.time.LocalDateTime;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

@Entity
@Table(name = "activity_events")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@EntityListeners(AuditingEntityListener.class)
public class ActivityEvent {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "user_id", nullable = false)
	private User user;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "note_id", nullable = false)
	private Note note;

	@Enumerated(EnumType.STRING)
	@Column(name = "type", nullable = false, length = 30)
	private ActivityEventType type;

	@Column(name = "activity_date", nullable = false)
	private LocalDate activityDate;

	@CreatedDate
	@Column(name = "created_at")
	private LocalDateTime createdAt;

	public ActivityEvent(
			User user,
			Note note,
			ActivityEventType type,
			LocalDate activityDate
	) {
		this.user = user;
		this.note = note;
		this.type = type;
		this.activityDate = activityDate;
	}

	public static ActivityEvent create(
			User user,
			Note note,
			ActivityEventType type,
			LocalDate activityDate
	) {
		return new ActivityEvent(
				user,
				note,
				type,
				activityDate
		);
	}
}
