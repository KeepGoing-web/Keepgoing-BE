package com.keepgoing.keepgoing.post.service;

import com.keepgoing.keepgoing.global.common.error.BusinessException;
import com.keepgoing.keepgoing.global.common.error.ErrorCode;
import com.keepgoing.keepgoing.post.domain.Post;
import com.keepgoing.keepgoing.post.domain.PostVisibility;
import com.keepgoing.keepgoing.post.dto.PostCreateRequest;
import com.keepgoing.keepgoing.post.dto.PostResponse;
import com.keepgoing.keepgoing.post.dto.PostUpdateRequest;
import com.keepgoing.keepgoing.post.repository.PostRepository;
import com.keepgoing.keepgoing.user.domain.User;
import com.keepgoing.keepgoing.user.repository.UserRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.AssertionsForClassTypes.assertThatThrownBy;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
public class PostServiceTest {

    @Mock
    PostRepository postRepository;

    @Mock
    UserRepository userRepository;

    @InjectMocks
    PostService postService;

    // ===== 테스트용 헬퍼 메서드들 =====

    private User createAuthor(Long id) {
        return User.builder()
                .id(id)
                .email("test@example.com")
                .name("테스트유저")
                .build();
    }

    private PostCreateRequest createPostCreateRequest() {
        return PostCreateRequest.builder()
                .title("제목")
                .content("내용")
                .visibility(PostVisibility.PRIVATE)
                .aiCollectable(true)
                .build();
    }


    private PostUpdateRequest createPostUpdateRequest() {
        return PostUpdateRequest.builder()
                .title("수정 제목")
                .content("수정 내용")
                .visibility(PostVisibility.PUBLIC)
                .aiCollectable(false)
                .build();
    }

    // ========== createPost ==========

    @Test
    @DisplayName("createPost: 유저가 존재하면 게시글을 생성한다")
    void createPost_createsPostWhenUserExists() {
        // given
        Long authorId = 1L;
        User author = createAuthor(authorId);
        PostCreateRequest request = createPostCreateRequest();

        given(userRepository.findById(authorId)).willReturn(Optional.of(author));

        Post post = Post.create(
                author,
                request.getTitle(),
                request.getContent(),
                request.getVisibility(),
                request.isAiCollectable()
        );
        given(postRepository.save(any(Post.class))).willReturn(post);

        // when
        PostResponse response = postService.createPost(authorId, request);

        // then
        assertThat(response.getTitle()).isEqualTo("제목");
        verify(userRepository).findById(authorId);
        verify(postRepository).save(any(Post.class));
    }

    @Test
    @DisplayName("createPost: 유저가 없으면 USER_NOT_FOUND 예외 발생")
    void createPost_throwsWhenUserNotFound() {
        // given
        Long authorId = 1L;
        PostCreateRequest request = createPostCreateRequest();

        given(userRepository.findById(authorId)).willReturn(Optional.empty());

        // when & then
        assertThatThrownBy(() -> postService.createPost(authorId, request))
                .isInstanceOf(BusinessException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.USER_NOT_FOUND);
    }

    // ========== getPost ==========

    @Test
    @DisplayName("getPost: 게시글이 존재하면 반환한다")
    void getPost_returnsPostWhenExists() {
        // given
        Long postId = 1L;
        User author = createAuthor(1L);
        Post post = Post.create(author, "제목", "내용", PostVisibility.PRIVATE, true);

        given(postRepository.findById(postId)).willReturn(Optional.of(post));

        // when
        PostResponse response = postService.getPost(postId);

        // then
        assertThat(response.getTitle()).isEqualTo("제목");
        verify(postRepository).findById(postId);
    }

    @Test
    @DisplayName("getPost: 게시글이 없으면 POST_NOT_FOUND 예외 발생")
    void getPost_throwsWhenNotFound() {
        // given
        Long postId = 1L;
        given(postRepository.findById(postId)).willReturn(Optional.empty());

        // when & then
        assertThatThrownBy(() -> postService.getPost(postId))
                .isInstanceOf(BusinessException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.POST_NOT_FOUND);
    }

    // ========== getMyPosts ==========

    @Test
    @DisplayName("getMyPosts: 작성자 ID로 게시글 목록을 최신순으로 가져온다")
    void getMyPosts_returnsList() {
        // given
        Long authorId = 1L;
        User author = createAuthor(authorId);

        Post post1 = Post.create(author, "제목1", "내용1", PostVisibility.PRIVATE, true);
        Post post2 = Post.create(author, "제목2", "내용2", PostVisibility.PUBLIC, true);

        given(postRepository.findByAuthor_IdOrderByCreatedAtDesc(authorId))
                .willReturn(List.of(post1, post2));

        // when
        var responses = postService.getMyPosts(authorId);

        // then
        assertThat(responses).hasSize(2);
        assertThat(responses.get(0).getTitle()).isEqualTo("제목1");
        assertThat(responses.get(1).getTitle()).isEqualTo("제목2");
        verify(postRepository).findByAuthor_IdOrderByCreatedAtDesc(authorId);
    }

    // ========== updatePost ==========

    @Test
    @DisplayName("updatePost: 작성자가 맞으면 게시글이 수정된다")
    void updatePost_updatesWhenAuthorMatches() {
        // given
        Long authorId = 1L;
        Long postId = 10L;

        User author = createAuthor(authorId);
        Post post = Post.create(author, "old", "old", PostVisibility.PRIVATE, true);

        PostUpdateRequest request = createPostUpdateRequest(); // title/content/visibility/aiCollectable 세팅

        given(postRepository.findById(postId)).willReturn(Optional.of(post));

        // when
        PostResponse response = postService.updatePost(authorId, postId, request);

        // then
        assertThat(response.getTitle()).isEqualTo(request.getTitle());
        assertThat(response.getContent()).isEqualTo(request.getContent());
        assertThat(response.getVisibility()).isEqualTo(request.getVisibility());
        assertThat(response.isAiCollectable()).isEqualTo(request.isAiCollectable());
        verify(postRepository).findById(postId);
    }

    @Test
    @DisplayName("updatePost: 게시글이 없으면 POST_NOT_FOUND 예외")
    void updatePost_throwsWhenPostNotFound() {
        // given
        Long authorId = 1L;
        Long postId = 10L;
        PostUpdateRequest request = createPostUpdateRequest();

        given(postRepository.findById(postId)).willReturn(Optional.empty());

        // when & then
        assertThatThrownBy(() -> postService.updatePost(authorId, postId, request))
                .isInstanceOf(BusinessException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.POST_NOT_FOUND);
    }

    @Test
    @DisplayName("updatePost: 작성자가 아니면 POST_ACCESS_DENIED 예외")
    void updatePost_throwsWhenNotAuthor() {
        // given
        Long authorId = 1L;
        Long othersId = 2L;
        Long postId = 10L;

        User author = createAuthor(othersId); // 실제 작성자는 2번
        Post post = Post.create(author, "old", "old", PostVisibility.PRIVATE, true);

        PostUpdateRequest request = createPostUpdateRequest();

        given(postRepository.findById(postId)).willReturn(Optional.of(post));

        // when & then
        assertThatThrownBy(() -> postService.updatePost(authorId, postId, request))
                .isInstanceOf(BusinessException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.POST_ACCESS_DENIED);
    }

    // ========== deletePost ==========

    @Test
    @DisplayName("deletePost: 작성자가 맞으면 softDelete 된다")
    void deletePost_softDeletesWhenAuthorMatches() {
        // given
        Long authorId = 1L;
        Long postId = 10L;

        User author = createAuthor(authorId);
        Post post = Post.create(author, "title", "content", PostVisibility.PRIVATE, true);

        given(postRepository.findById(postId)).willReturn(Optional.of(post));

        // when
        postService.deletePost(authorId, postId);

        // then
        assertThat(post.isDeleted()).isTrue(); // isDeleted 없으면 deletedAt != null 로 체크
        verify(postRepository).findById(postId);
    }

    @Test
    @DisplayName("deletePost: 게시글이 없으면 POST_NOT_FOUND 예외")
    void deletePost_throwsWhenPostNotFound() {
        // given
        Long authorId = 1L;
        Long postId = 10L;

        given(postRepository.findById(postId)).willReturn(Optional.empty());

        // when & then
        assertThatThrownBy(() -> postService.deletePost(authorId, postId))
                .isInstanceOf(BusinessException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.POST_NOT_FOUND);
    }

    @Test
    @DisplayName("deletePost: 작성자가 아니면 POST_ACCESS_DENIED 예외")
    void deletePost_throwsWhenNotAuthor() {
        // given
        Long authorId = 1L;
        Long othersId = 2L;
        Long postId = 10L;

        User author = createAuthor(othersId); // 작성자는 2번
        Post post = Post.create(author, "title", "content", PostVisibility.PRIVATE, true);

        given(postRepository.findById(postId)).willReturn(Optional.of(post));

        // when & then
        assertThatThrownBy(() -> postService.deletePost(authorId, postId))
                .isInstanceOf(BusinessException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.POST_ACCESS_DENIED);
    }
}
