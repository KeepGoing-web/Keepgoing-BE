package com.keepgoing.keepgoing.note;

import static java.nio.charset.StandardCharsets.UTF_8;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.never;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.keepgoing.keepgoing.global.storage.InputStreamSupplier;
import com.keepgoing.keepgoing.global.storage.ObjectStorageClient;
import com.keepgoing.keepgoing.global.storage.ObjectStorageException;
import com.keepgoing.keepgoing.note.domain.Note;
import com.keepgoing.keepgoing.note.domain.NoteImage;
import com.keepgoing.keepgoing.note.domain.NoteImageStatus;
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
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest
@AutoConfigureMockMvc(addFilters = false)
@Transactional
class NoteImageUploadIntegrationTest {

	private static final String IMAGE_NAME = "image.png";
	private static final String CONTENT_TYPE = MediaType.IMAGE_PNG_VALUE;
	private static final byte[] IMAGE_CONTENT = "image-content".getBytes(UTF_8);

	@Autowired
	MockMvc mockMvc;

	@Autowired
	UserRepository userRepository;

	@Autowired
	NoteRepository noteRepository;

	@Autowired
	NoteImageRepository noteImageRepository;

	@Autowired
	EntityManager entityManager;

	@MockitoBean
	ObjectStorageClient objectStorageClient;

	@AfterEach
	void tearDown() {
		SecurityContextHolder.clearContext();
		noteImageRepository.deleteAllInBatch();
		noteRepository.deleteAllInBatch();
		userRepository.deleteAllInBatch();
	}

	@Nested
	@DisplayName("POST /api/notes/{noteId}/images")
	class UploadImage {

		@Test
		@DisplayName("작성자가 이미지를 업로드하면 quarantine 저장 후 PENDING 메타데이터를 DB에 저장한다")
		void uploadsImageAndPersistsPendingMetadata() throws Exception {
			// given
			User author = saveUser("author@test.com", "작성자");
			Note note = saveNote(author, "이미지를 포함할 노트");
			String storageKey = "notes/" + note.getId() + "/generated-image-key.png";
			mockLoginUser(author.getId());

			given(objectStorageClient.upload(any(), eq("notes/" + note.getId()), eq(IMAGE_NAME),
					eq((long) IMAGE_CONTENT.length)))
					.willReturn(storageKey);

			// when & then
			mockMvc.perform(multipart("/api/notes/{noteId}/images", note.getId())
							.file(imageFile())
							.contentType(MediaType.MULTIPART_FORM_DATA))
					.andExpect(status().isCreated())
					.andExpect(jsonPath("$.success").value(true))
					.andExpect(jsonPath("$.data.publicId").isNotEmpty())
					.andExpect(jsonPath("$.data.status").value(NoteImageStatus.PENDING.name()));

			entityManager.flush();
			entityManager.clear();

			NoteImage savedImage = findSingleImage(note.getId());
			assertThat(savedImage.getStorageKey()).isEqualTo(storageKey);
			assertThat(savedImage.getOriginalName()).isEqualTo(IMAGE_NAME);
			assertThat(savedImage.getContentType()).isEqualTo(CONTENT_TYPE);
			assertThat(savedImage.getFileSize()).isEqualTo(IMAGE_CONTENT.length);
			assertThat(savedImage.getStatus()).isEqualTo(NoteImageStatus.PENDING);
			assertThat(savedImage.getUploader().getId()).isEqualTo(author.getId());
			assertThat(savedImage.getPublicId()).isNotNull();

			ArgumentCaptor<InputStreamSupplier> supplierCaptor =
					ArgumentCaptor.forClass(InputStreamSupplier.class);
			then(objectStorageClient).should().upload(
					supplierCaptor.capture(),
					eq("notes/" + note.getId()),
					eq(IMAGE_NAME),
					eq((long) IMAGE_CONTENT.length)
			);
			assertThat(supplierCaptor.getValue().get().readAllBytes()).isEqualTo(IMAGE_CONTENT);
			then(objectStorageClient).should(never()).delete(any());
		}

		@Test
		@DisplayName("빈 파일을 업로드하면 400을 반환하고 스토리지와 DB를 사용하지 않는다")
		void returnsBadRequestWhenFileIsEmpty() throws Exception {
			// given
			User author = saveUser("author@test.com", "작성자");
			Note note = saveNote(author, "빈 이미지 업로드 노트");
			mockLoginUser(author.getId());
			MockMultipartFile emptyFile = new MockMultipartFile(
					"file",
					IMAGE_NAME,
					CONTENT_TYPE,
					new byte[0]
			);

			// when & then
			mockMvc.perform(multipart("/api/notes/{noteId}/images", note.getId())
							.file(emptyFile)
							.contentType(MediaType.MULTIPART_FORM_DATA))
					.andExpect(status().isBadRequest())
					.andExpect(jsonPath("$.success").value(false))
					.andExpect(jsonPath("$.error.code").value("VALIDATION_FAILED"));

			assertThat(countAllImageRows()).isZero();
			then(objectStorageClient).shouldHaveNoInteractions();
		}

		@Test
		@DisplayName("존재하지 않는 노트에 업로드하면 404를 반환하고 스토리지와 DB를 사용하지 않는다")
		void returnsNotFoundWhenNoteDoesNotExist() throws Exception {
			// given
			User uploader = saveUser("uploader@test.com", "업로더");
			mockLoginUser(uploader.getId());

			// when & then
			mockMvc.perform(multipart("/api/notes/{noteId}/images", 999999L)
							.file(imageFile())
							.contentType(MediaType.MULTIPART_FORM_DATA))
					.andExpect(status().isNotFound())
					.andExpect(jsonPath("$.success").value(false))
					.andExpect(jsonPath("$.error.code").value("NOTE_NOT_FOUND"));

			assertThat(countAllImageRows()).isZero();
			then(objectStorageClient).shouldHaveNoInteractions();
		}

		@Test
		@DisplayName("타인의 노트에 업로드하면 403을 반환하고 스토리지와 DB를 사용하지 않는다")
		void returnsForbiddenWhenRequesterIsNotAuthor() throws Exception {
			// given
			User author = saveUser("author@test.com", "작성자");
			User requester = saveUser("requester@test.com", "요청자");
			Note note = saveNote(author, "타인의 노트");
			mockLoginUser(requester.getId());

			// when & then
			mockMvc.perform(multipart("/api/notes/{noteId}/images", note.getId())
							.file(imageFile())
							.contentType(MediaType.MULTIPART_FORM_DATA))
					.andExpect(status().isForbidden())
					.andExpect(jsonPath("$.success").value(false))
					.andExpect(jsonPath("$.error.code").value("NOTE_ACCESS_DENIED"));

			assertThat(countAllImageRows()).isZero();
			then(objectStorageClient).shouldHaveNoInteractions();
		}

		@Test
		@DisplayName("스토리지 업로드가 실패하면 503을 반환하고 이미지 메타데이터를 저장하지 않는다")
		void returnsServiceUnavailableWhenStorageUploadFails() throws Exception {
			// given
			User author = saveUser("author@test.com", "작성자");
			Note note = saveNote(author, "이미지 업로드 실패 노트");
			mockLoginUser(author.getId());

			given(objectStorageClient.upload(any(), eq("notes/" + note.getId()), eq(IMAGE_NAME),
					eq((long) IMAGE_CONTENT.length)))
					.willThrow(new ObjectStorageException("upload failed", new RuntimeException("network")));

			// when & then
			mockMvc.perform(multipart("/api/notes/{noteId}/images", note.getId())
							.file(imageFile())
							.contentType(MediaType.MULTIPART_FORM_DATA))
					.andExpect(status().isServiceUnavailable())
					.andExpect(jsonPath("$.success").value(false))
					.andExpect(jsonPath("$.error.code").value("SERVICE_UNAVAILABLE"));

			assertThat(countAllImageRows()).isZero();
			then(objectStorageClient).should()
					.upload(any(), eq("notes/" + note.getId()), eq(IMAGE_NAME), eq((long) IMAGE_CONTENT.length));
			then(objectStorageClient).should(never()).delete(any());
		}

		@Test
		@Transactional(propagation = Propagation.NOT_SUPPORTED)
		@DisplayName("스토리지 업로드 후 DB 저장이 실패하면 업로드된 파일을 삭제하고 500을 반환한다")
		void cleansUpUploadedFileWhenMetadataSaveFails() throws Exception {
			// given
			User author = saveUser("author@test.com", "작성자");
			Note note = saveNote(author, "이미지 메타데이터 저장 실패 노트");
			String duplicatedStorageKey = "notes/" + note.getId() + "/duplicated-image-key.png";
			saveNoteImage(note, author, duplicatedStorageKey);
			mockLoginUser(author.getId());

			given(objectStorageClient.upload(any(), eq("notes/" + note.getId()), eq(IMAGE_NAME),
					eq((long) IMAGE_CONTENT.length)))
					.willReturn(duplicatedStorageKey);

			// when & then
			mockMvc.perform(multipart("/api/notes/{noteId}/images", note.getId())
							.file(imageFile())
							.contentType(MediaType.MULTIPART_FORM_DATA))
					.andExpect(status().isInternalServerError())
					.andExpect(jsonPath("$.success").value(false))
					.andExpect(jsonPath("$.error.code").value("INTERNAL_SERVER_ERROR"));

			assertThat(noteImageRepository.count()).isEqualTo(1);
			then(objectStorageClient).should().upload(
					any(),
					eq("notes/" + note.getId()),
					eq(IMAGE_NAME),
					eq((long) IMAGE_CONTENT.length)
			);
			then(objectStorageClient).should().delete(duplicatedStorageKey);
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
		NoteImage noteImage = NoteImage.create(
				note,
				uploader,
				storageKey,
				IMAGE_NAME,
				CONTENT_TYPE,
				(long) IMAGE_CONTENT.length
		);
		return noteImageRepository.saveAndFlush(noteImage);
	}

	private MockMultipartFile imageFile() {
		return new MockMultipartFile(
				"file",
				IMAGE_NAME,
				CONTENT_TYPE,
				IMAGE_CONTENT
		);
	}

	private NoteImage findSingleImage(Long noteId) {
		return noteImageRepository.findAll().stream()
				.filter(noteImage -> noteImage.getNote().getId().equals(noteId))
				.findFirst()
				.orElseThrow();
	}

	private long countAllImageRows() {
		return noteImageRepository.count();
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
