package com.keepgoing.keepgoing.ai.service;

import com.keepgoing.keepgoing.ai.repository.AiNoteIndexRepository;
import java.time.LocalDateTime;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class AiNoteIndexFailureRecorder {

	private final AiNoteIndexRepository aiNoteIndexRepository;

	@Transactional(propagation = Propagation.REQUIRES_NEW)
	public void markFailed(Long noteId, LocalDateTime failedAt, String errorMessage) {
		aiNoteIndexRepository.findById(noteId)
				.ifPresent(index -> index.markFailed(failedAt, errorMessage));
	}
}
