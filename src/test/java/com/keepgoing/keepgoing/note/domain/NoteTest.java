package com.keepgoing.keepgoing.note.domain;

import com.keepgoing.keepgoing.global.common.error.BusinessException;
import com.keepgoing.keepgoing.global.common.error.ErrorCode;
import com.keepgoing.keepgoing.user.domain.User;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.AssertionsForClassTypes.assertThatThrownBy;

class NoteTest {

    // ========== create ==========
    @Test
    @DisplayName("create: 기본값이 잘 세팅 되는지 테스트")
    void create_setsDefault() {
        // given
        User author = User.builder()
                .id(1L)
                .build();

        String title = "제목";
        String content = "내용";

        // when
        Note note = Note.create(
                author,
                title,
                content,
                null,
                true
        );

        // then
        assertThat(note.getAuthor()).isEqualTo(author);
        assertThat(note.getTitle()).isEqualTo(title);
        assertThat(note.getContent()).isEqualTo(content);
        assertThat(note.getVisibility()).isEqualTo(NoteVisibility.PRIVATE); // null일 때 기본값
        assertThat(note.isAiCollectable()).isTrue();
    }

    @Test
    @DisplayName("create: visibility가 null이면 PRIVATE 기본값")
    void create_defaultVisibilityWhenNull() {
        //given
        User author = User.builder()
                .id(1L)
                .email("test@example.com")
                .name("테스트유저")
                .build();

        String title = "제목";
        String content = "내용";
        NoteVisibility visibility = null;
        boolean aiCollectable = false;

        //when
        Note note = Note.create(
                author,
                title,
                content,
                visibility,
                aiCollectable
        );

        //then
        assertThat(note.getAuthorId()).isEqualTo(1L);
        assertThat(note.getTitle()).isEqualTo(title);
        assertThat(note.getContent()).isEqualTo(content);
        assertThat(note.getVisibility()).isEqualTo(NoteVisibility.PRIVATE);
        assertThat(note.isAiCollectable()).isFalse();
    }

    @Test
    @DisplayName("create: visibility가 주어지면 그 값을 사용")
    void create_useGivenVisibility() {
        //given
        User author = User.builder()
                .id(1L)
                .email("test@example.com")
                .name("테스트유저")
                .build();

        String title = "제목";
        String content = "내용";
        NoteVisibility visibility = NoteVisibility.PUBLIC;
        boolean aiCollectable = false;

        //when
        Note note = Note.create(author, title, content, visibility, aiCollectable);

        //then
        assertThat(note.getVisibility()).isEqualTo(NoteVisibility.PUBLIC);
        assertThat(note.isAiCollectable()).isFalse();
    }

    // ========== update ==========
    @Test
    @DisplayName("update: 제목/내용/visibility/aiCollectable 변경")
    void update_changeFields() {
        // given
        User author = User.builder()
                .id(1L)
                .build();

        Note note = Note.create(
                author,
                "old title",
                "old content",
                NoteVisibility.PRIVATE,
                true
        );

        // when
        note.update(
                "new title",
                "new content",
                NoteVisibility.PUBLIC,
                false);

        // then
        assertThat(note.getTitle()).isEqualTo("new title");
        assertThat(note.getContent()).isEqualTo("new content");
        assertThat(note.getVisibility()).isEqualTo(NoteVisibility.PUBLIC);
        assertThat(note.isAiCollectable()).isFalse();
    }

    // ========== softDelete ==========
    @Test
    @DisplayName("softDelete: deletedAt 채워지고 isDeleted true 반환")
    void softDelete_setsDeletedAt() {
        // given
        User author = User.builder()
                .id(1L)
                .email("test@example.com")
                .name("테스트유저")
                .build();

        Note note = Note.create(
                author,
                "title",
                "content",
                NoteVisibility.PRIVATE,
                true
        );

        // when
        note.softDelete();

        // then
        assertThat(note.isDeleted()).isTrue();
        assertThat(note.getDeletedAt()).isNotNull();
    }

    // ========== validateAuthor ==========
    @Test
    @DisplayName("validateAuthor: 작성자가 아니면 예외 발생")
    void validateAuthor_throws_whenNotAuthor() {
        // given
        User author = User.builder()
                .id(1L)
                .email("a@a.com")
                .name("작성자")
                .build();

        Note note = Note.create(
                author,
                "title",
                "content",
                NoteVisibility.PRIVATE,
                true
        );

        // when & then
        assertThatThrownBy(() -> note.validateAuthor(2L))
                .isInstanceOf(BusinessException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.NOTE_ACCESS_DENIED);
    }

    // ========== isAuthor ==========
    @Test
    @DisplayName("isAuthor: 작성자인지 여부를 반환")
    void isAuthor_returnsTrueOnlyForAuthor() {
        // given
        User author = User.builder()
                .id(1L)
                .email("a@a.com")
                .name("작성자")
                .build();

        Note note = Note.create(
                author,
                "title",
                "content",
                NoteVisibility.PRIVATE,
                true
        );

        // expect
        assertThat(note.isAuthor(1L)).isTrue();
        assertThat(note.isAuthor(2L)).isFalse();
    }

    // ========== getAuthorId ==========
    @Test
    @DisplayName("getAuthorId: 작성자 ID를 정상적으로 반환")
    void getAuthorId_returnsAuthorId() {
        // given
        User author = User.builder()
                .id(1L)
                .build();

        Note note = Note.create(
                author,
                "title",
                "content",
                NoteVisibility.PRIVATE,
                true);

        // when & then
        assertThat(note.getAuthorId()).isEqualTo(1L);
    }
}