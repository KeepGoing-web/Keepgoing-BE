package com.keepgoing.keepgoing.note.controller.dto.request;

import static org.assertj.core.api.Assertions.assertThat;

import com.keepgoing.keepgoing.note.service.dto.NoteImageUploadCommand;
import java.nio.charset.StandardCharsets;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;

class NoteImageUploadRequestTest {

	@Test
	@DisplayName("toCommand는 Content-Type을 정규화해서 변환한다")
	void toCommandNormalizesContentType() {
		// given
		Long noteId = 1L;
		MockMultipartFile file = new MockMultipartFile(
				"file",
				"image.jpg",
				" IMAGE/JPEG ",
				"image-content".getBytes(StandardCharsets.UTF_8)
		);
		NoteImageUploadRequest request = new NoteImageUploadRequest(file);

		// when
		NoteImageUploadCommand command = request.toCommand(noteId);

		// then
		assertThat(command.noteId()).isEqualTo(noteId);
		assertThat(command.originalFileName()).isEqualTo("image.jpg");
		assertThat(command.contentType()).isEqualTo(MediaType.IMAGE_JPEG_VALUE);
		assertThat(command.fileSize()).isEqualTo(file.getSize());
	}
}