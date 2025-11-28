package com.keepgoing.keepgoing.post.domain;

import com.keepgoing.keepgoing.global.common.error.BusinessException;
import com.keepgoing.keepgoing.global.common.error.ErrorCode;
import com.keepgoing.keepgoing.user.domain.User;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.AssertionsForClassTypes.assertThatThrownBy;

class PostTest {

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
        Post post = Post.create(
                author,
                title,
                content,
                null,
                true
        );

        // then
        assertThat(post.getAuthor()).isEqualTo(author);
        assertThat(post.getTitle()).isEqualTo(title);
        assertThat(post.getContent()).isEqualTo(content);
        assertThat(post.getVisibility()).isEqualTo(PostVisibility.PRIVATE); // null일 때 기본값
        assertThat(post.isAiCollectable()).isTrue();
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
        PostVisibility visibility = null;
        boolean aiCollectable = true;

        //when
        Post post = Post.create(
                author,
                title,
                content,
                visibility,
                aiCollectable
        );

        //then
        assertThat(post.getAuthorId()).isEqualTo(1L);
        assertThat(post.getTitle()).isEqualTo(title);
        assertThat(post.getContent()).isEqualTo(content);
        assertThat(post.getVisibility()).isEqualTo(PostVisibility.PRIVATE);
        assertThat(post.isAiCollectable()).isTrue();
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
        PostVisibility visibility = PostVisibility.PUBLIC;
        boolean aiCollectable = false;

        //when
        Post post = Post.create(author, title, content, visibility, aiCollectable);

        //then
        assertThat(post.getVisibility()).isEqualTo(PostVisibility.PUBLIC);
        assertThat(post.isAiCollectable()).isFalse();
    }

    // ========== update ==========
    @Test
    @DisplayName("update: 제목/내용/visibility/aiCollectable 변경")
    void update_changeFields() {
        // given
        User author = User.builder()
                .id(1L)
                .build();

        Post post = Post.create(
                author,
                "old title",
                "old content",
                PostVisibility.PRIVATE,
                true
        );

        // when
        post.update(
                "new title",
                "new content",
                PostVisibility.PUBLIC,
                false);

        // then
        assertThat(post.getTitle()).isEqualTo("new title");
        assertThat(post.getContent()).isEqualTo("new content");
        assertThat(post.getVisibility()).isEqualTo(PostVisibility.PUBLIC);
        assertThat(post.isAiCollectable()).isFalse();
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

        Post post = Post.create(
                author,
                "title",
                "content",
                PostVisibility.PRIVATE,
                true
        );

        // when
        post.softDelete();

        // then
        assertThat(post.isDeleted()).isTrue();
        assertThat(post.getDeletedAt()).isNotNull();
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

        Post post = Post.create(
                author,
                "title",
                "content",
                PostVisibility.PRIVATE,
                true
        );

        // when & then
        assertThatThrownBy(() -> post.validateAuthor(2L))
                .isInstanceOf(BusinessException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.POST_ACCESS_DENIED);
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

        Post post = Post.create(
                author,
                "title",
                "content",
                PostVisibility.PRIVATE,
                true
        );

        // expect
        assertThat(post.isAuthor(1L)).isTrue();
        assertThat(post.isAuthor(2L)).isFalse();
    }

    // ========== getAuthorId ==========
    @Test
    @DisplayName("getAuthorId: 작성자 ID를 정상적으로 반환")
    void getAuthorId_returnsAuthorId() {
        // given
        User author = User.builder()
                .id(1L)
                .build();

        Post post = Post.create(
                author,
                "title",
                "content",
                PostVisibility.PRIVATE,
                true);

        // when & then
        assertThat(post.getAuthorId()).isEqualTo(1L);
    }
}