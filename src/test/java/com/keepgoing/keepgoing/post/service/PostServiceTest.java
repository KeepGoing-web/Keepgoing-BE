package com.keepgoing.keepgoing.post.service;

import com.keepgoing.keepgoing.global.common.error.BusinessException;
import com.keepgoing.keepgoing.global.common.error.ErrorCode;
import com.keepgoing.keepgoing.post.domain.Post;
import com.keepgoing.keepgoing.post.domain.PostVisibility;
import com.keepgoing.keepgoing.post.repository.PostRepository;
import com.keepgoing.keepgoing.post.service.dto.*;
import com.keepgoing.keepgoing.user.domain.User;
import com.keepgoing.keepgoing.user.repository.UserRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.*;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.AssertionsForClassTypes.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class PostServiceTest {

    @Mock
    PostRepository postRepository;

    @Mock
    UserRepository userRepository;

    @InjectMocks
    PostService postService;

    // ===== 테스트용 헬퍼 메서드들 =====

    private User createUser(Long id) {
        return User.builder()
                .id(id)
                .email("test@example.com")
                .name("테스트유저")
                .build();
    }

    // ========== createPost ==========

    @Test
    @DisplayName("createPost: 유저가 존재하면 게시글을 생성한다")
    void createPost_createsPostWhenUserExists() {
        // given
        Long userId = 1L;
        User user = createUser(userId);

        String title = "제목";
        String content = "내용";
        PostVisibility visibility = PostVisibility.PRIVATE;
        boolean aiCollectable = false;

        given(userRepository.findById(userId)).willReturn(Optional.of(user));

        Post post = Post.create(
                user,
                title,
                content,
                visibility,
                aiCollectable
        );
        given(postRepository.save(any(Post.class))).willReturn(post);

        PostCreateCommand command = new PostCreateCommand(
                userId,
                title,
                content,
                visibility,
                aiCollectable
        );

        // when
        PostDetailResult result = postService.createPost(command);

        // then
        assertThat(result.title()).isEqualTo(title);
        assertThat(result.content()).isEqualTo(content);
        assertThat(result.visibility()).isEqualTo(visibility);
        assertThat(result.aiCollectable()).isEqualTo(aiCollectable);
        verify(userRepository).findById(userId);
        verify(postRepository).save(any(Post.class));
        verifyNoMoreInteractions(userRepository, postRepository);
    }

    @Test
    @DisplayName("createPost: 유저가 없으면 USER_NOT_FOUND 예외 발생")
    void createPost_throwsWhenUserNotFound() {
        // given
        Long userId = 1L;
        String title = "제목";
        String content = "내용";
        PostVisibility visibility = PostVisibility.PRIVATE;
        boolean aiCollectable = false;

        given(userRepository.findById(userId)).willReturn(Optional.empty());

        PostCreateCommand command = new PostCreateCommand(
                userId,
                title,
                content,
                visibility,
                aiCollectable
        );
        // when & then
        assertThatThrownBy(() -> postService.createPost(command))
                .isInstanceOf(BusinessException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.USER_NOT_FOUND);
        verify(userRepository).findById(userId);
        verifyNoInteractions(postRepository);
    }

    // ========== getPost ==========

    @Test
    @DisplayName("getPost: 게시글이 존재하면 반환한다")
    void getPost_returnsPostWhenExists() {
        // given
        Long postId = 1L;
        User user = createUser(1L);
        Post post = Post.create(user, "제목", "내용", PostVisibility.PRIVATE, true);

        given(postRepository.findById(postId)).willReturn(Optional.of(post));

        // when
        PostDetailResult result = postService.getPost(postId);

        // then
        assertThat(result.title()).isEqualTo("제목");
        assertThat(result.content()).isEqualTo("내용");
        assertThat(result.visibility()).isEqualTo(PostVisibility.PRIVATE);

        verify(postRepository).findById(postId);
        verifyNoMoreInteractions(postRepository);
        verifyNoInteractions(userRepository);
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
        verify(postRepository).findById(postId);
        verifyNoMoreInteractions(postRepository);
        verifyNoInteractions(userRepository);

    }

    // ========== getPosts ==========

    @Test
    @DisplayName("getPosts: 작성자 ID와 Pageable로 게시글 페이지를 가져온다")
    void getPosts_returnsPage() {
        // given
        Long userId = 1L;
        User user = createUser(userId);

        Post post1 = Post.create(user, "제목1", "내용1", PostVisibility.PRIVATE, true);
        Post post2 = Post.create(user, "제목2", "내용2", PostVisibility.PUBLIC, true);
        var posts = java.util.List.of(post1, post2);

        Pageable pageable = PageRequest.of(
                0,
                10,
                Sort.by(Sort.Direction.DESC, "createdAt")
        );
        Page<Post> postPage = new PageImpl<>(
                posts,
                pageable,
                posts.size()
        );

        // 저장소가 페이지를 반환하는 동작을 스텁
        given(postRepository.findByAuthor_Id(userId, pageable))
                .willReturn(postPage);

        // when
        Page<PostSummaryResult> result = postService.getPosts(userId, pageable);

        // then
        assertThat(result.getTotalElements()).isEqualTo(2);
        assertThat(result.getContent()).hasSize(2);
        assertThat(result.getContent().get(0).title()).isEqualTo("제목1");
        assertThat(result.getContent().get(1).title()).isEqualTo("제목2");
        verify(postRepository).findByAuthor_Id(userId, pageable);
        verifyNoMoreInteractions(postRepository);
        verifyNoInteractions(userRepository);
    }

    // ========== updatePost ==========

    @Test
    @DisplayName("updatePost: 작성자가 맞으면 게시글이 수정된다")
    void updatePost_updatesWhenAuthorMatches() {
        // given
        Long userId = 1L;
        Long postId = 10L;

        User user = createUser(userId);
        Post post = Post.create(user, "old", "old", PostVisibility.PRIVATE, true);

        String newTitle = "수정 제목";
        String newContent = "수정 내용";
        PostVisibility newVisibility = PostVisibility.PUBLIC;
        boolean newAiCollectable = false;

        given(postRepository.findById(postId)).willReturn(Optional.of(post));

        PostUpdateCommand command = new PostUpdateCommand(
                postId,
                userId,
                newTitle,
                newContent,
                newVisibility,
                newAiCollectable
        );

        // when
        PostDetailResult result = postService.updatePost(command);

        // then
        assertThat(result.title()).isEqualTo(newTitle);
        assertThat(result.content()).isEqualTo(newContent);
        assertThat(result.visibility()).isEqualTo(newVisibility);
        assertThat(result.aiCollectable()).isEqualTo(newAiCollectable);
        verify(postRepository).findById(postId);
        verifyNoMoreInteractions(postRepository);
        verifyNoInteractions(userRepository);
    }

    @Test
    @DisplayName("updatePost: 게시글이 없으면 POST_NOT_FOUND 예외")
    void updatePost_throwsWhenPostNotFound() {
        // given
        Long userId = 1L;
        Long postId = 10L;

        String newTitle = "수정 제목";
        String newContent = "수정 내용";
        PostVisibility newVisibility = PostVisibility.PUBLIC;
        boolean newAiCollectable = false;

        given(postRepository.findById(postId)).willReturn(Optional.empty());

        PostUpdateCommand command = new PostUpdateCommand(
                postId,
                userId,
                newTitle,
                newContent,
                newVisibility,
                newAiCollectable
        );

        // when & then
        assertThatThrownBy(() -> postService.updatePost(command))
                .isInstanceOf(BusinessException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.POST_NOT_FOUND);
        verify(postRepository).findById(postId);
        verifyNoMoreInteractions(postRepository);
        verifyNoInteractions(userRepository);
    }

    @Test
    @DisplayName("updatePost: 작성자가 아니면 POST_ACCESS_DENIED 예외")
    void updatePost_throwsWhenNotAuthor() {
        // given
        Long userId = 1L;
        Long othersId = 2L;
        Long postId = 10L;

        User user = createUser(othersId); // 실제 작성자는 2번
        Post post = Post.create(user, "old", "old", PostVisibility.PRIVATE, true);

        String newTitle = "수정 제목";
        String newContent = "수정 내용";
        PostVisibility newVisibility = PostVisibility.PUBLIC;
        boolean newAiCollectable = false;

        given(postRepository.findById(postId)).willReturn(Optional.of(post));

        PostUpdateCommand command = new PostUpdateCommand(
                postId,
                userId,
                newTitle,
                newContent,
                newVisibility,
                newAiCollectable
        );

        // when & then
        assertThatThrownBy(() -> postService.updatePost(command))
                .isInstanceOf(BusinessException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.POST_ACCESS_DENIED);
        verify(postRepository).findById(postId);
        verifyNoMoreInteractions(postRepository);
        verifyNoInteractions(userRepository);
    }

    // ========== deletePost ==========

    @Test
    @DisplayName("deletePost: 작성자가 맞으면 softDelete 된다")
    void deletePost_softDeletesWhenAuthorMatches() {
        // given
        Long userId = 1L;
        Long postId = 10L;

        User user = createUser(userId);
        Post post = Post.create(user, "title", "content", PostVisibility.PRIVATE, true);

        given(postRepository.findById(postId)).willReturn(Optional.of(post));

        // when
        postService.deletePost(userId, postId);

        // then
        assertThat(post.isDeleted()).isTrue(); // isDeleted 없으면 deletedAt != null 로 체크
        verify(postRepository).findById(postId);
        verifyNoMoreInteractions(postRepository);
        verifyNoInteractions(userRepository);
    }

    @Test
    @DisplayName("deletePost: 게시글이 없으면 POST_NOT_FOUND 예외")
    void deletePost_throwsWhenPostNotFound() {
        // given
        Long userId = 1L;
        Long postId = 10L;

        given(postRepository.findById(postId)).willReturn(Optional.empty());

        // when & then
        assertThatThrownBy(() -> postService.deletePost(userId, postId))
                .isInstanceOf(BusinessException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.POST_NOT_FOUND);
        verify(postRepository).findById(postId);
        verifyNoMoreInteractions(postRepository);
        verifyNoInteractions(userRepository);
    }

    @Test
    @DisplayName("deletePost: 작성자가 아니면 POST_ACCESS_DENIED 예외")
    void deletePost_throwsWhenNotAuthor() {
        // given
        Long userId = 1L;
        Long othersId = 2L;
        Long postId = 10L;

        User user = createUser(othersId); // 작성자는 2번
        Post post = Post.create(user, "title", "content", PostVisibility.PRIVATE, true);

        given(postRepository.findById(postId)).willReturn(Optional.of(post));

        // when & then
        assertThatThrownBy(() -> postService.deletePost(userId, postId))
                .isInstanceOf(BusinessException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.POST_ACCESS_DENIED);
        verify(postRepository).findById(postId);
        verifyNoMoreInteractions(postRepository);
        verifyNoInteractions(userRepository);
    }

    // ========== searchMyPosts ==========

    @Test
    @DisplayName("searchMyPosts: keyword가 있으면 검색 결과를 페이지로 반환한다")
    void searchPost_returnsPageWhenKeywordProvided() {
        //given
        Long userId = 1L;
        User user = createUser(userId);

        Post post1 = Post.create(user, "spring 제목", "내용", PostVisibility.PUBLIC, true);
        Post post2 = Post.create(user, "제목", "spring 내용", PostVisibility.PUBLIC, true);

        Pageable pageable = PageRequest.of(0, 10, Sort.by(Sort.Direction.DESC, "createdAt"));
        Page<Post> postPage = new PageImpl<>(java.util.List.of(post1, post2), pageable, 2);

        PostSearchQuery query = new PostSearchQuery("spring", pageable);

        given(postRepository.searchMyPosts(userId, "spring", pageable))
                .willReturn(postPage);

        // when
        Page<PostSummaryResult> result = postService.searchMyPosts(userId, query);

        // then
        assertThat(result.getTotalElements()).isEqualTo(2);
        verify(postRepository).searchMyPosts(userId, "spring", pageable);
        verifyNoMoreInteractions(postRepository);
        verifyNoInteractions(userRepository);
    }

    @Test
    @DisplayName("searchMyPosts: keyword가 비어있으면 POST_SEARCH_KEYWORD_REQUIRED 예외")
    void searchPost_throwsWhenKeywordBlank() {
        // given
        Long userId = 1L;
        Pageable pageable = PageRequest.of(0, 10);
        PostSearchQuery query = new PostSearchQuery("   ", pageable);

        // when & then
        assertThatThrownBy(() -> postService.searchMyPosts(userId, query))
                .isInstanceOf(BusinessException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.POST_SEARCH_KEYWORD_REQUIRED);

        // repository 호출되면 안 됨
        verify(postRepository, never())
                .searchMyPosts(any(), any(), any());
        verifyNoInteractions(userRepository);
    }


    @Test
    @DisplayName("searchMyPosts: keyword 앞뒤 공백은 trim되어 검색된다")
    void searchPost_trimsKeywordBeforeSearching() {
        // given
        Long userId = 1L;
        User user = createUser(userId);
        Post post = Post.create(user, "spring 제목", "내용", PostVisibility.PUBLIC, true);

        Pageable pageable = PageRequest.of(0, 10, Sort.by(Sort.Direction.DESC, "createdAt"));
        Page<Post> postPage = new PageImpl<>(java.util.List.of(post), pageable, 1);

        PostSearchQuery query = new PostSearchQuery("  spring  ", pageable);

        given(postRepository.searchMyPosts(userId, "spring", pageable))
                .willReturn(postPage);

        // when
        Page<PostSummaryResult> result = postService.searchMyPosts(userId, query);

        // then
        assertThat(result.getTotalElements()).isEqualTo(1);
        verify(postRepository).searchMyPosts(userId, "spring", pageable);
        verifyNoMoreInteractions(postRepository);
        verifyNoInteractions(userRepository);
    }
}
