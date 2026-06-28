package com.keepgoing.keepgoing.note.controller;

import static java.nio.charset.StandardCharsets.UTF_8;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.BDDMockito.willThrow;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.keepgoing.keepgoing.common.image.domain.ImageProcessingStatus;
import com.keepgoing.keepgoing.global.api.exception.GlobalExceptionHandler;
import com.keepgoing.keepgoing.global.common.error.BusinessException;
import com.keepgoing.keepgoing.global.common.error.ErrorCode;
import com.keepgoing.keepgoing.global.config.JacksonConfig;
import com.keepgoing.keepgoing.global.security.cookie.CookieConfig;
import com.keepgoing.keepgoing.global.security.jwt.JwtProvider;
import com.keepgoing.keepgoing.note.service.NoteImageService;
import com.keepgoing.keepgoing.note.service.dto.NoteImagePresignQuery;
import com.keepgoing.keepgoing.note.service.dto.NoteImageUploadCommand;
import com.keepgoing.keepgoing.note.service.dto.NoteImageUploadResult;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.data.jpa.mapping.JpaMetamodelMappingContext;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@AutoConfigureMockMvc(addFilters = false)
@WebMvcTest(NoteImageController.class)
@Import({GlobalExceptionHandler.class, CookieConfig.class, JacksonConfig.class})
class NoteImageControllerTest {

	@Autowired
	MockMvc mockMvc;

	@MockitoBean
	NoteImageService noteImageService;

	@MockitoBean
	JpaMetamodelMappingContext jpaMappingContext;

	@MockitoBean
	JwtProvider jwtProvider;

	@AfterEach
	void tearDown() {
		SecurityContextHolder.clearContext();
	}

	@Nested
	@DisplayName("POST /api/notes/{noteId}/images")
	class UploadImage {

		@Test
		@DisplayName("인증 사용자가 이미지를 업로드하면 201과 publicId/status를 반환하고 커맨드를 전달한다")
		void uploadsImageSuccessfully() throws Exception {
			// given
			Long userId = 1L;
			Long noteId = 10L;
			UUID publicId = UUID.randomUUID();
			byte[] fileContent = "image-content".getBytes(UTF_8);
			MockMultipartFile file = new MockMultipartFile(
					"file",
					"image.png",
					MediaType.IMAGE_PNG_VALUE,
					fileContent
			);
			NoteImageUploadResult result = new NoteImageUploadResult(publicId, ImageProcessingStatus.PENDING);

			mockLoginUser(userId);
			given(noteImageService.uploadImage(eq(userId), any(NoteImageUploadCommand.class)))
					.willReturn(result);

			// when & then
			mockMvc.perform(multipart("/api/notes/{noteId}/images", noteId)
							.file(file)
							.contentType(MediaType.MULTIPART_FORM_DATA))
					.andExpect(status().isCreated())
					.andExpect(jsonPath("$.success").value(true))
					.andExpect(jsonPath("$.data.publicId").value(publicId.toString()))
					.andExpect(jsonPath("$.data.status").value(ImageProcessingStatus.PENDING.name()));

			ArgumentCaptor<NoteImageUploadCommand> captor = ArgumentCaptor.forClass(NoteImageUploadCommand.class);
			then(noteImageService).should().uploadImage(eq(userId), captor.capture());

			NoteImageUploadCommand command = captor.getValue();
			assertThat(command.noteId()).isEqualTo(noteId);
			assertThat(command.originalFileName()).isEqualTo("image.png");
			assertThat(command.contentType()).isEqualTo(MediaType.IMAGE_PNG_VALUE);
			assertThat(command.fileSize()).isEqualTo(fileContent.length);
			assertThat(new String(command.inputStreamSupplier().get().readAllBytes(), UTF_8))
					.isEqualTo("image-content");
		}

		@Test
		@DisplayName("파일이 없으면 400을 반환하고 서비스를 호출하지 않는다")
		void returnsBadRequestWhenFileIsMissing() throws Exception {
			// given
			Long userId = 1L;
			Long noteId = 10L;
			mockLoginUser(userId);

			// when & then
			mockMvc.perform(multipart("/api/notes/{noteId}/images", noteId)
							.contentType(MediaType.MULTIPART_FORM_DATA))
					.andExpect(status().isBadRequest())
					.andExpect(jsonPath("$.success").value(false))
					.andExpect(jsonPath("$.error.code").value(ErrorCode.VALIDATION_FAILED.name()));

			then(noteImageService).shouldHaveNoInteractions();
		}

		@Test
		@DisplayName("빈 파일이면 400을 반환하고 서비스를 호출하지 않는다")
		void returnsBadRequestWhenFileIsEmpty() throws Exception {
			// given
			Long userId = 1L;
			Long noteId = 10L;
			mockLoginUser(userId);
			MockMultipartFile file = new MockMultipartFile(
					"file",
					"image.png",
					MediaType.IMAGE_PNG_VALUE,
					new byte[0]
			);

			// when & then
			mockMvc.perform(multipart("/api/notes/{noteId}/images", noteId)
							.file(file)
							.contentType(MediaType.MULTIPART_FORM_DATA))
					.andExpect(status().isBadRequest())
					.andExpect(jsonPath("$.success").value(false))
					.andExpect(jsonPath("$.error.code").value(ErrorCode.VALIDATION_FAILED.name()));

			then(noteImageService).shouldHaveNoInteractions();
		}

		@Test
		@DisplayName("파일명이 비어 있으면 400을 반환하고 서비스를 호출하지 않는다")
		void returnsBadRequestWhenOriginalFilenameIsBlank() throws Exception {
			// given
			Long userId = 1L;
			Long noteId = 10L;
			mockLoginUser(userId);
			MockMultipartFile file = new MockMultipartFile(
					"file",
					" ",
					MediaType.IMAGE_PNG_VALUE,
					"image-content".getBytes(UTF_8)
			);

			// when & then
			mockMvc.perform(multipart("/api/notes/{noteId}/images", noteId)
							.file(file)
							.contentType(MediaType.MULTIPART_FORM_DATA))
					.andExpect(status().isBadRequest())
					.andExpect(jsonPath("$.success").value(false))
					.andExpect(jsonPath("$.error.code").value(ErrorCode.VALIDATION_FAILED.name()));

			then(noteImageService).shouldHaveNoInteractions();
		}

		@Test
		@DisplayName("Content-Type이 없으면 400을 반환하고 서비스를 호출하지 않는다")
		void returnsBadRequestWhenContentTypeIsMissing() throws Exception {
			// given
			Long userId = 1L;
			Long noteId = 10L;
			mockLoginUser(userId);
			MockMultipartFile file = new MockMultipartFile(
					"file",
					"image.png",
					null,
					"image-content".getBytes(UTF_8)
			);

			// when & then
			mockMvc.perform(multipart("/api/notes/{noteId}/images", noteId)
							.file(file)
							.contentType(MediaType.MULTIPART_FORM_DATA))
					.andExpect(status().isBadRequest())
					.andExpect(jsonPath("$.success").value(false))
					.andExpect(jsonPath("$.error.code").value(ErrorCode.VALIDATION_FAILED.name()));

			then(noteImageService).shouldHaveNoInteractions();
		}

		@Test
		@DisplayName("지원하지 않는 Content-Type이면 400을 반환하고 서비스를 호출하지 않는다")
		void returnsBadRequestWhenContentTypeIsNotAllowed() throws Exception {
			// given
			Long userId = 1L;
			Long noteId = 10L;
			mockLoginUser(userId);
			MockMultipartFile file = new MockMultipartFile(
					"file",
					"image.txt",
					MediaType.TEXT_PLAIN_VALUE,
					"image-content".getBytes(UTF_8)
			);

			// when & then
			mockMvc.perform(multipart("/api/notes/{noteId}/images", noteId)
							.file(file)
							.contentType(MediaType.MULTIPART_FORM_DATA))
					.andExpect(status().isBadRequest())
					.andExpect(jsonPath("$.success").value(false))
					.andExpect(jsonPath("$.error.code").value(ErrorCode.VALIDATION_FAILED.name()));

			then(noteImageService).shouldHaveNoInteractions();
		}

		@Test
		@DisplayName("존재하지 않는 노트면 404와 NOTE_NOT_FOUND를 반환한다")
		void returnsNotFoundWhenNoteDoesNotExist() throws Exception {
			// given
			Long userId = 1L;
			Long noteId = 10L;
			MockMultipartFile file = imageFile();

			mockLoginUser(userId);
			willThrow(new BusinessException(ErrorCode.NOTE_NOT_FOUND))
					.given(noteImageService)
					.uploadImage(eq(userId), any(NoteImageUploadCommand.class));

			// when & then
			mockMvc.perform(multipart("/api/notes/{noteId}/images", noteId)
							.file(file)
							.contentType(MediaType.MULTIPART_FORM_DATA))
					.andExpect(status().isNotFound())
					.andExpect(jsonPath("$.success").value(false))
					.andExpect(jsonPath("$.error.code").value(ErrorCode.NOTE_NOT_FOUND.name()));

			then(noteImageService).should().uploadImage(eq(userId), any(NoteImageUploadCommand.class));
		}

		@Test
		@DisplayName("타인의 노트면 403과 NOTE_ACCESS_DENIED를 반환한다")
		void returnsForbiddenWhenRequesterIsNotOwner() throws Exception {
			// given
			Long userId = 1L;
			Long noteId = 10L;
			MockMultipartFile file = imageFile();

			mockLoginUser(userId);
			willThrow(new BusinessException(ErrorCode.NOTE_ACCESS_DENIED))
					.given(noteImageService)
					.uploadImage(eq(userId), any(NoteImageUploadCommand.class));

			// when & then
			mockMvc.perform(multipart("/api/notes/{noteId}/images", noteId)
							.file(file)
							.contentType(MediaType.MULTIPART_FORM_DATA))
					.andExpect(status().isForbidden())
					.andExpect(jsonPath("$.success").value(false))
					.andExpect(jsonPath("$.error.code").value(ErrorCode.NOTE_ACCESS_DENIED.name()));

			then(noteImageService).should().uploadImage(eq(userId), any(NoteImageUploadCommand.class));
		}
	}

	@Nested
	@DisplayName("GET /api/notes/{noteId}/images/{publicId}")
	class RedirectToImageTest {

		@Test
		@DisplayName("PUBLIC SAFE 이미지 조회 시 302 Found로 Presigned URL로 redirect한다")
		void redirectsToPresignedUrl() throws Exception {
			// given
			UUID publicId = UUID.randomUUID();
			String presignedUrl = "https://minio/secure/key?X-Amz-Signature=abc";
			given(noteImageService.getPresignedUrl(any(NoteImagePresignQuery.class)))
					.willReturn(presignedUrl);

			// when & then
			mockMvc.perform(get("/api/notes/{noteId}/images/{publicId}", 1L, publicId))
					.andExpect(status().isFound())
					.andExpect(header().string("Location", presignedUrl))
					.andExpect(header().string(
							"Cache-Control",
							org.hamcrest.Matchers.containsString("no-cache"))
					);
		}

		@Test
		@DisplayName("존재하지 않는 이미지 조회 시 404를 반환한다")
		void returns404WhenImageNotFound() throws Exception {
			// given
			UUID publicId = UUID.randomUUID();
			given(noteImageService.getPresignedUrl(any(NoteImagePresignQuery.class)))
					.willThrow(new BusinessException(ErrorCode.NOTE_IMAGE_NOT_FOUND));

			// when & then
			mockMvc.perform(get("/api/notes/{noteId}/images/{publicId}", 1L, publicId))
					.andExpect(status().isNotFound());
		}

		@Test
		@DisplayName("익명 사용자가 PUBLIC SAFE 이미지를 조회하면 302로 redirect한다")
		void redirectsAnonymousUserForPublicImage() throws Exception {
			UUID publicId = UUID.randomUUID();
			given(noteImageService.getPresignedUrl(any(NoteImagePresignQuery.class)))
					.willReturn("https://minio/secure/key?X-Amz-Signature=abc");

			mockMvc.perform(get("/api/notes/{noteId}/images/{publicId}", 1L, publicId))
					.andExpect(status().isFound())
					.andExpect(header().exists("Location"));
		}

		// #6: 잘못된 noteId → 404
		@Test
		@DisplayName("유효한 publicId + 잘못된 noteId로 조회하면 404를 반환한다")
		void returns404WhenNoteIdMismatch() throws Exception {
			UUID publicId = UUID.randomUUID();
			given(noteImageService.getPresignedUrl(any(NoteImagePresignQuery.class)))
					.willThrow(new BusinessException(ErrorCode.NOTE_IMAGE_NOT_FOUND));

			mockMvc.perform(get("/api/notes/{noteId}/images/{publicId}", 999L, publicId))
					.andExpect(status().isNotFound());
		}
	}

	private void mockLoginUser(Long userId) {
		SecurityContextHolder.getContext()
				.setAuthentication(new UsernamePasswordAuthenticationToken(userId, null, List.of()));
	}

	private MockMultipartFile imageFile() {
		return new MockMultipartFile(
				"file",
				"image.png",
				MediaType.IMAGE_PNG_VALUE,
				"image-content".getBytes(UTF_8)
		);
	}
}
