package com.keepgoing.keepgoing.ai.service;

import com.keepgoing.keepgoing.ai.domain.AiNoteIndex;
import com.keepgoing.keepgoing.ai.domain.AiNoteIndexStatus;
import com.keepgoing.keepgoing.ai.repository.AiNoteIndexRepository;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class AiNoteIndexRecoveryScheduler {

	private static final int BATCH_SIZE = 50;
	private static final int MAX_ATTEMPT_COUNT = 3;

	private final AiNoteIndexRepository aiNoteIndexRepository;
	private final AiNoteIndexingService aiNoteIndexingService;

	@Scheduled(fixedDelayString = "${app.ai.indexing.recovery-delay-ms:60000}")
	public void recoverRetryTargets() {
		List<AiNoteIndex> targets = aiNoteIndexRepository.findRetryTargets(
				List.of(AiNoteIndexStatus.PENDING, AiNoteIndexStatus.FAILED),
				MAX_ATTEMPT_COUNT,
				PageRequest.of(0, BATCH_SIZE)
		);

		for (AiNoteIndex target : targets) {
			process(target.getNoteId());
		}
	}

	private void process(Long noteId) {
		try {
			aiNoteIndexingService.process(noteId);
		} catch (RuntimeException exception) {
			log.warn("AI note index recovery failed. noteId={}", noteId, exception);
		}
	}
}
