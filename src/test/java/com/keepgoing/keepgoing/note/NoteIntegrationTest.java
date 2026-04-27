package com.keepgoing.keepgoing.note;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.keepgoing.keepgoing.note.controller.dto.NoteRenameRequest;
import com.keepgoing.keepgoing.note.domain.Note;
import com.keepgoing.keepgoing.note.domain.NoteImage;
import com.keepgoing.keepgoing.note.domain.NoteVisibility;
import com.keepgoing.keepgoing.note.repository.NoteImageRepository;
import com.keepgoing.keepgoing.note.repository.NoteRepository;
import com.keepgoing.keepgoing.user.domain.User;
import com.keepgoing.keepgoing.user.domain.UserRole;
import com.keepgoing.keepgoing.user.repository.UserRepository;
import jakarta.persistence.EntityManager;
import java.util.List;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest
@AutoConfigureMockMvc(addFilters = false)
@Transactional
class NoteIntegrationTest {

	@Autowired
	MockMvc mockMvc;

	@Autowired
	ObjectMapper objectMapper;

	@Autowired
	UserRepository userRepository;

	@Autowired
	NoteRepository noteRepository;

	@Autowired
	NoteImageRepository noteImageRepository;

	@Autowired
	EntityManager entityManager;

	@AfterEach
	void tearDown() {
		SecurityContextHolder.clearContext();
	}

	@Nested
	@DisplayName("PATCH /api/notes/{noteId}/title")
	class RenameTitle {

		@Test
		@DisplayName("작성자가 제목 변경을 요청하면 응답과 DB에 변경 내용이 반영된다")
		void renamesTitleAndPersistsChange() throws Exception {
			User author = saveUser("author@test.com", "작성자");
			Note note = saveNote(author, "기존 제목");
			mockLoginUser(author.getId());

			mockMvc.perform(patch("/api/notes/{noteId}/title", note.getId())
							.contentType(APPLICATION_JSON)
							.content(requestJson(new NoteRenameRequest("변경된 제목"))))
					.andExpect(status().isOk())
					.andExpect(jsonPath("$.success").value(true))
					.andExpect(jsonPath("$.data.noteId").value(note.getId()))
					.andExpect(jsonPath("$.data.userId").value(author.getId()))
					.andExpect(jsonPath("$.data.title").value("변경된 제목"));

			entityManager.flush();
			entityManager.clear();

			Note renamed = noteRepository.findById(note.getId()).orElseThrow();
			assertThat(renamed.getTitle()).isEqualTo("변경된 제목");
		}

		@Test
		@DisplayName("제목을 공백으로 보내면 입력값 검증 오류를 응답하고 기존 제목을 유지한다")
		void returnsBadRequestWhenTitleIsBlank() throws Exception {
			User author = saveUser("author@test.com", "작성자");
			Note note = saveNote(author, "기존 제목");
			mockLoginUser(author.getId());

			mockMvc.perform(patch("/api/notes/{noteId}/title", note.getId())
							.contentType(APPLICATION_JSON)
							.content(requestJson(new NoteRenameRequest(" "))))
					.andExpect(status().isBadRequest())
					.andExpect(jsonPath("$.success").value(false))
					.andExpect(jsonPath("$.error.code").value("VALIDATION_FAILED"))
					.andExpect(jsonPath("$.error.fieldErrors[0].field").value("title"));

			entityManager.flush();
			entityManager.clear();

			Note unchanged = noteRepository.findById(note.getId()).orElseThrow();
			assertThat(unchanged.getTitle()).isEqualTo("기존 제목");
		}

		@Test
		@DisplayName("다른 사용자가 제목 변경을 요청하면 권한 오류를 응답하고 기존 제목을 유지한다")
		void returnsForbiddenWhenRequesterIsNotAuthor() throws Exception {
			User author = saveUser("author@test.com", "작성자");
			User requester = saveUser("other@test.com", "다른 사용자");
			Note note = saveNote(author, "기존 제목");
			mockLoginUser(requester.getId());

			mockMvc.perform(patch("/api/notes/{noteId}/title", note.getId())
							.contentType(APPLICATION_JSON)
							.content(requestJson(new NoteRenameRequest("변경 시도"))))
					.andExpect(status().isForbidden())
					.andExpect(jsonPath("$.success").value(false))
					.andExpect(jsonPath("$.error.code").value("NOTE_ACCESS_DENIED"));

			entityManager.flush();
			entityManager.clear();

			Note unchanged = noteRepository.findById(note.getId()).orElseThrow();
			assertThat(unchanged.getTitle()).isEqualTo("기존 제목");
		}

		@Test
		@DisplayName("없는 노트의 제목 변경을 요청하면 찾을 수 없음을 응답한다")
		void returnsNotFoundWhenNoteDoesNotExist() throws Exception {
			User author = saveUser("author@test.com", "작성자");
			mockLoginUser(author.getId());

			mockMvc.perform(patch("/api/notes/{noteId}/title", 999999L)
							.contentType(APPLICATION_JSON)
							.content(requestJson(new NoteRenameRequest("새 제목"))))
					.andExpect(status().isNotFound())
					.andExpect(jsonPath("$.success").value(false))
					.andExpect(jsonPath("$.error.code").value("NOTE_NOT_FOUND"));
		}
	}

	@Nested
	@DisplayName("DELETE /api/notes/{noteId}")
	class DeleteNote {

		@Test
		@DisplayName("작성자가 노트 삭제를 요청하면 노트와 연결 이미지가 soft delete된다")
		void softDeletesNoteAndImages() throws Exception {
			User author = saveUser("author@test.com", "작성자");
			Note targetNote = saveNote(author, "삭제할 노트");
			Note otherNote = saveNote(author, "유지할 노트");
			saveNoteImage(targetNote, author, "note-images/target-1.png");
			saveNoteImage(targetNote, author, "note-images/target-2.png");
			saveNoteImage(otherNote, author, "note-images/other.png");
			mockLoginUser(author.getId());

			mockMvc.perform(delete("/api/notes/{noteId}", targetNote.getId()))
					.andExpect(status().isNoContent());

			entityManager.flush();
			entityManager.clear();

			assertThat(countDeletedNotes(targetNote.getId())).isEqualTo(1);
			assertThat(countDeletedImages(targetNote.getId())).isEqualTo(2);
			assertThat(countActiveImages(otherNote.getId())).isEqualTo(1);
		}
	}

	private User saveUser(String email, String name) {
		return userRepository.saveAndFlush(User.create(email, name));
	}

	private Note saveNote(User author, String title) {
		Note note = Note.create(author, null, title, "본문", NoteVisibility.PRIVATE, false);
		return noteRepository.saveAndFlush(note);
	}

	private NoteImage saveNoteImage(Note note, User uploader, String storageKey) {
		NoteImage image = NoteImage.create(
				note,
				uploader,
				storageKey,
				"image.png",
				"image/png",
				1024L
		);
		return noteImageRepository.saveAndFlush(image);
	}

	private long countDeletedNotes(Long noteId) {
		return ((Number) entityManager.createNativeQuery("""
					select count(*)
					from notes
					where id = :noteId
					  and deleted_at is not null
					""")
				.setParameter("noteId", noteId)
				.getSingleResult())
				.longValue();
	}

	private long countDeletedImages(Long noteId) {
		return ((Number) entityManager.createNativeQuery("""
					select count(*)
					from note_images
					where note_id = :noteId
					  and deleted_at is not null
					""")
				.setParameter("noteId", noteId)
				.getSingleResult())
				.longValue();
	}

	private long countActiveImages(Long noteId) {
		return ((Number) entityManager.createNativeQuery("""
					select count(*)
					from note_images
					where note_id = :noteId
					  and deleted_at is null
					""")
				.setParameter("noteId", noteId)
				.getSingleResult())
				.longValue();
	}

	private String requestJson(Object request) throws Exception {
		return objectMapper.writeValueAsString(request);
	}

	private void mockLoginUser(Long userId) {
		Authentication authenticated = UsernamePasswordAuthenticationToken.authenticated(
				userId,
				null,
				List.of(new SimpleGrantedAuthority(UserRole.USER.toAuthority()))
		);
		SecurityContextHolder.getContext().setAuthentication(authenticated);
	}
}
