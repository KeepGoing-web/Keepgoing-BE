package com.keepgoing.keepgoing.post.repository;

import com.keepgoing.keepgoing.post.domain.Post;
import com.keepgoing.keepgoing.post.domain.PostVisibility;
import com.keepgoing.keepgoing.user.domain.User;
import com.keepgoing.keepgoing.user.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest // JPA 관련 컴포넌트만 로드하여 테스트
class PostRepositoryTest {

    @Autowired
    private PostRepository postRepository;

    @Autowired
    private UserRepository userRepository;

    private User user1;
    private User user2;

    @BeforeEach
    void setUp() {
        // 각 테스트 전에 데이터 초기화 및 설정
        user1 = userRepository.save(User.builder().email("user1@test.com").name("유저1").build());
        user2 = userRepository.save(User.builder().email("user2@test.com").name("유저2").build());
    }

    private Pageable sortedByCreatedAtDesc(int size) {
        return PageRequest.of(0, size, Sort.by(Sort.Direction.DESC, "createdAt"));
    }

    // ========== findByAuthor_Id ==========

    @Test
    @DisplayName("findByAuthor_Id: 특정 작성자의 게시글을 createdAt 내림차순으로 페이징 조회한다")
    void findByAuthorId_returnsPostsSortedByCreatedAtDesc() {
        // given
        postRepository.save(Post.create(user1, "제목1-1", "내용", PostVisibility.PUBLIC, true));

        // NOTE: createdAt 정렬 보장용 (나중에 id 정렬 등으로 리팩터링 후보)
        try { Thread.sleep(10); } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        postRepository.save(Post.create(user2, "제목2-1", "내용", PostVisibility.PUBLIC, true)); // 다른 유저의 글
        try { Thread.sleep(10); } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        postRepository.save(Post.create(user1, "제목1-2", "내용", PostVisibility.PUBLIC, true));

        Pageable pageable = sortedByCreatedAtDesc(10);

        // when
        Page<Post> page = postRepository.findByAuthor_Id(user1.getId(), pageable);
        List<Post> result = page.getContent();

        // then
        assertThat(page.getTotalElements()).isEqualTo(2);
        assertThat(result).hasSize(2);
        assertThat(result.get(0).getTitle()).isEqualTo("제목1-2"); // 최신
        assertThat(result.get(1).getTitle()).isEqualTo("제목1-1");
    }

    @Test
    @DisplayName("findByAuthor_Id: 작성자의 게시글이 없으면 빈 리스트를 반환한다")
    void findByAuthorId_returnsEmptyList_WhenNoPosts() {
        // given
        // user1이 작성한 글 없음

        Pageable pageable = sortedByCreatedAtDesc(10);

        // when
        List<Post> result = postRepository.findByAuthor_Id(user1.getId(), pageable)
                .getContent();

        // then
        assertThat(result).isEmpty();
    }

    @Test
    @DisplayName("findByAuthor_Id: soft delete 된 게시글은 조회되지 않는다")
    void findByAuthorId_excludesSoftDeletedPosts() {
        // given
        Post post1 = postRepository.save(Post.create(user1, "살아있는 글", "내용", PostVisibility.PUBLIC, true));
        Post post2 = postRepository.save(Post.create(user1, "삭제된 글", "내용", PostVisibility.PUBLIC, true));

        // soft delete
        post2.softDelete();
        postRepository.save(post2); // 변경사항 반영

        Pageable pageable = sortedByCreatedAtDesc(10);

        // when
        List<Post> result = postRepository.findByAuthor_Id(user1.getId(), pageable)
                .getContent();

        // then
        assertThat(result)
                .hasSize(1)
                .extracting(Post::getTitle)
                .containsExactly("살아있는 글");
    }

    // ========== findById ==========

    @Test
    @DisplayName("findById: findById는 author를 함께 로딩한다(@EntityGraph)")
    void findById_loadsAuthorWithEntityGraph() {
        // given
        User author = user1;

        Post saved = postRepository.save(Post.create(author, "title", "content", PostVisibility.PRIVATE, true));

        // when
        Post found = postRepository.findById(saved.getId())
                .orElseThrow();

        // then
        assertThat(found.getAuthor().getId()).isEqualTo(author.getId());
    }

    // ========== findByVisibilityOrderByCreatedAtDesc ==========

    @Test
    @DisplayName("findByVisibilityOrderByCreatedAtDesc: 공개 범위에 따라 필터링하고 최신순으로 정렬한다")
    void findByVisibilityOrderByCreatedAtDesc() {
        // given
        postRepository.save(Post.create(user1, "비공개글1", "내용", PostVisibility.PRIVATE, true));
        postRepository.save(Post.create(user1, "비공개글2", "내용", PostVisibility.PRIVATE, true));
        postRepository.save(Post.create(user1, "공개글1", "내용", PostVisibility.PUBLIC, true));
        postRepository.save(Post.create(user2, "공개글2", "내용", PostVisibility.PUBLIC, true));

        // when
        List<Post> result = postRepository.findByVisibilityOrderByCreatedAtDesc(PostVisibility.PUBLIC);

        // then
        assertThat(result).hasSize(2);
        // 최신순 보장 (생성 순서의 역순)
        assertThat(result.get(0).getTitle()).isEqualTo("공개글2");
        assertThat(result.get(1).getTitle()).isEqualTo("공개글1");
    }

    @Test
    @DisplayName("findByVisibilityOrderByCreatedAtDesc: 해당 공개 범위의 게시글이 없으면 빈 리스트를 반환한다")
    void findByVisibilityOrderByCreatedAtDesc_returnsEmptyListWhenNoPosts() {
        // given
        // PUBLIC 글 없음

        // when
        List<Post> result = postRepository.findByVisibilityOrderByCreatedAtDesc(PostVisibility.PUBLIC);

        // then
        assertThat(result).isEmpty();
    }
}
