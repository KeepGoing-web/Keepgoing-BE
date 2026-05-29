package com.keepgoing.keepgoing.note.repository;

import static org.assertj.core.api.Assertions.assertThat;

import com.keepgoing.keepgoing.folder.domain.Folder;
import com.keepgoing.keepgoing.folder.repository.FolderRepository;
import com.keepgoing.keepgoing.global.config.JpaAuditingConfig;
import com.keepgoing.keepgoing.note.domain.Note;
import com.keepgoing.keepgoing.note.domain.NoteVisibility;
import com.keepgoing.keepgoing.note.service.dto.NoteDetailResult;
import com.keepgoing.keepgoing.note.service.dto.NoteSummaryResult;
import com.keepgoing.keepgoing.user.domain.User;
import com.keepgoing.keepgoing.user.repository.UserRepository;
import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityManagerFactory;
import java.util.List;
import org.hibernate.SessionFactory;
import org.hibernate.stat.Statistics;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;
import org.springframework.data.domain.Sort;

@DataJpaTest
@Import(JpaAuditingConfig.class)
class NoteRepositoryTest {

	@Autowired
	private NoteRepository noteRepository;

	@Autowired
	private UserRepository userRepository;

	@Autowired
	private FolderRepository folderRepository;

	@Autowired
	private EntityManager em;

	@Autowired
	private EntityManagerFactory entityManagerFactory;

	private User user1;
	private User user2;

	@BeforeEach
	void setUp() {
		// 각 테스트 전에 데이터 초기화 및 설정
		user1 = userRepository.save(User.create("user1@test.com", "유저1"));
		user2 = userRepository.save(User.create("user2@test.com", "유저2"));
	}

	private Pageable sortedByCreatedAtDesc(int size) {
		return PageRequest.of(0, size, Sort.by(Sort.Direction.DESC, "createdAt"));
	}

	// ========== findByAuthor_Id ==========

	@Test
	@DisplayName("findByAuthor_Id: 특정 작성자의 게시글을 createdAt 내림차순으로 페이징 조회한다")
	void findByAuthorId_returnsNotesSortedByCreatedAtDesc() {
		// given
		noteRepository.save(Note.create(user1, null, "제목1-1", "내용", NoteVisibility.PUBLIC, true));

		// NOTE: createdAt 정렬 보장용 (나중에 id 정렬 등으로 리팩터링 후보)
		try {
			Thread.sleep(10);
		} catch (InterruptedException e) {
			Thread.currentThread().interrupt();
		}
		noteRepository.save(Note.create(user2, null, "제목2-1", "내용", NoteVisibility.PUBLIC, true)); // 다른 유저의 글
		try {
			Thread.sleep(10);
		} catch (InterruptedException e) {
			Thread.currentThread().interrupt();
		}
		noteRepository.save(Note.create(user1, null, "제목1-2", "내용", NoteVisibility.PUBLIC, true));

		Pageable pageable = sortedByCreatedAtDesc(10);

		// when
		Page<Note> page = noteRepository.findByAuthor_Id(user1.getId(), pageable);
		List<Note> result = page.getContent();

		// then
		assertThat(page.getTotalElements()).isEqualTo(2);
		assertThat(result).hasSize(2);
		assertThat(result.get(0).getTitle()).isEqualTo("제목1-2"); // 최신
		assertThat(result.get(1).getTitle()).isEqualTo("제목1-1");
	}

	@Test
	@DisplayName("findByAuthor_Id: 작성자의 게시글이 없으면 빈 리스트를 반환한다")
	void findByAuthorId_returnsEmptyList_WhenNoPNote() {
		// given
		// user1이 작성한 글 없음

		Pageable pageable = sortedByCreatedAtDesc(10);

		// when
		List<Note> result = noteRepository.findByAuthor_Id(user1.getId(), pageable)
				.getContent();

		// then
		assertThat(result).isEmpty();
	}

	@Test
	@DisplayName("findByAuthor_Id: soft delete 된 게시글은 조회되지 않는다")
	void findByAuthorId_excludesSoftDeletedNotesBy() {
		// given
		Note note1 = noteRepository.save(Note.create(user1, null, "살아있는 글", "내용", NoteVisibility.PUBLIC, true));
		Note note2 = noteRepository.save(Note.create(user1, null, "삭제된 글", "내용", NoteVisibility.PUBLIC, true));

		// soft delete
		note2.softDeleteBy(user1.getId());
		noteRepository.save(note2); // 변경사항 반영

		Pageable pageable = sortedByCreatedAtDesc(10);

		// when
		List<Note> result = noteRepository.findByAuthor_Id(user1.getId(), pageable)
				.getContent();

		// then
		assertThat(result)
				.hasSize(1)
				.extracting(Note::getTitle)
				.containsExactly("살아있는 글");
	}

	@Test
	@DisplayName("findByAuthor_Id: 조회 결과를 NoteSummaryResult로 매핑할 때 folder id 접근으로 추가 쿼리가 발생하지 않는다")
	void findByAuthorId_doesNotTriggerExtraQueryWhenMappingFolderId() {
		// given
		Folder folder = folderRepository.save(Folder.create(user1, null, "업무"));
		noteRepository.save(Note.create(user1, folder, "제목1", "내용1", NoteVisibility.PUBLIC, true));
		noteRepository.save(Note.create(user1, null, "제목2", "내용2", NoteVisibility.PUBLIC, true));

		em.flush();
		em.clear();

		Statistics statistics = statistics();
		statistics.clear();

		Pageable pageable = sortedByCreatedAtDesc(10);

		// when
		Page<Note> page = noteRepository.findByAuthor_Id(user1.getId(), pageable);
		long queryCountAfterFetch = statistics.getPrepareStatementCount();

		List<NoteSummaryResult> results = page.getContent().stream()
				.map(NoteSummaryResult::from)
				.toList();
		long queryCountAfterMapping = statistics.getPrepareStatementCount();

		// then
		assertThat(results).hasSize(2);
		assertThat(results)
				.extracting(NoteSummaryResult::folderId)
				.containsExactlyInAnyOrder(folder.getId(), null);

		assertThat(queryCountAfterMapping).isEqualTo(queryCountAfterFetch);
	}

	@Test
	@DisplayName("searchMyNotesSlice: 특정 작성자의 검색 결과를 최신순 슬라이스로 조회한다")
	void searchMyNotesSlice_returnsSliceSortedByCreatedAtDesc() {
		// given
		noteRepository.save(Note.create(user1, null, "일반 제목", "일반 내용", NoteVisibility.PRIVATE, false));

		try {
			Thread.sleep(10);
		} catch (InterruptedException e) {
			Thread.currentThread().interrupt();
		}
		noteRepository.save(Note.create(user2, null, "spring 다른 유저", "내용", NoteVisibility.PRIVATE, false));

		try {
			Thread.sleep(10);
		} catch (InterruptedException e) {
			Thread.currentThread().interrupt();
		}
		noteRepository.save(Note.create(user1, null, "spring 제목1", "내용", NoteVisibility.PRIVATE, false));

		try {
			Thread.sleep(10);
		} catch (InterruptedException e) {
			Thread.currentThread().interrupt();
		}
		noteRepository.save(Note.create(user1, null, "spring 제목2", "내용", NoteVisibility.PRIVATE, false));

		Pageable pageable = PageRequest.of(0, 1);

		// when
		Slice<Note> slice = noteRepository.searchMyNotesSlice(user1.getId(), "spring", pageable);

		// then
		assertThat(slice.getContent()).hasSize(1);
		assertThat(slice.hasNext()).isTrue();
		assertThat(slice.getContent().get(0).getTitle()).isEqualTo("spring 제목2");
	}

	@Test
	@DisplayName("searchMyNotes: Page 검색은 content query와 count query를 함께 실행한다")
	void searchMyNotes_executesContentAndCountQueries() {
		// given
		noteRepository.save(Note.create(user1, null, "spring 제목1", "내용", NoteVisibility.PRIVATE, false));
		noteRepository.save(Note.create(user1, null, "spring 제목2", "내용", NoteVisibility.PRIVATE, false));
		noteRepository.save(Note.create(user1, null, "spring 제목3", "내용", NoteVisibility.PRIVATE, false));

		em.flush();
		em.clear();

		Statistics statistics = statistics();
		statistics.clear();

		Pageable pageable = PageRequest.of(0, 2);

		// when
		Page<Note> page = noteRepository.searchMyNotes(user1.getId(), "spring", pageable);
		long queryCount = statistics.getPrepareStatementCount();

		// then
		assertThat(page.getContent()).hasSize(2);
		assertThat(page.getTotalElements()).isEqualTo(3);
		assertThat(queryCount).isEqualTo(2L);
	}

	@Test
	@DisplayName("searchMyNotesSlice: Slice 검색은 content query만 실행한다")
	void searchMyNotesSlice_executesOnlyContentQuery() {
		// given
		noteRepository.save(Note.create(user1, null, "spring 제목1", "내용", NoteVisibility.PRIVATE, false));
		noteRepository.save(Note.create(user1, null, "spring 제목2", "내용", NoteVisibility.PRIVATE, false));
		noteRepository.save(Note.create(user1, null, "spring 제목3", "내용", NoteVisibility.PRIVATE, false));

		em.flush();
		em.clear();

		Statistics statistics = statistics();
		statistics.clear();

		Pageable pageable = PageRequest.of(0, 2);

		// when
		Slice<Note> slice = noteRepository.searchMyNotesSlice(user1.getId(), "spring", pageable);
		long queryCount = statistics.getPrepareStatementCount();

		// then
		assertThat(slice.getContent()).hasSize(2);
		assertThat(slice.hasNext()).isTrue();
		assertThat(queryCount).isEqualTo(1L);
	}

	// ========== findById ==========

	@Test
	@DisplayName("findById: findById는 author를 함께 로딩한다(@EntityGraph)")
	void findById_loadsAuthorWithEntityGraph() {
		// given
		User author = user1;

		Note saved = noteRepository.save(Note.create(author, null, "title", "content", NoteVisibility.PRIVATE, true));

		// when
		Note found = noteRepository.findById(saved.getId())
				.orElseThrow();

		// then
		assertThat(found.getAuthor().getId()).isEqualTo(author.getId());
	}

	@Test
	@DisplayName("findById: 조회 결과를 NoteDetailResult로 매핑할 때 folder id 접근으로 추가 쿼리가 발생하지 않는다")
	void findById_doesNotTriggerExtraQueryWhenMappingFolderId() {
		// given
		Folder folder = folderRepository.save(Folder.create(user1, null, "업무"));
		Note saved = noteRepository.save(
				Note.create(user1, folder, "제목", "내용", NoteVisibility.PRIVATE, true)
		);

		em.flush();
		em.clear();

		Statistics statistics = statistics();
		statistics.clear();

		// when
		Note found = noteRepository.findById(saved.getId()).orElseThrow();
		long queryCountAfterFetch = statistics.getPrepareStatementCount();

		NoteDetailResult result = NoteDetailResult.from(found);
		long queryCountAfterMapping = statistics.getPrepareStatementCount();

		// then
		assertThat(result.folderId()).isEqualTo(folder.getId());
		assertThat(queryCountAfterMapping).isEqualTo(queryCountAfterFetch);
	}

	// ========== findByVisibilityOrderByCreatedAtDesc ==========

	@Test
	@DisplayName("findByVisibilityOrderByCreatedAtDesc: 공개 범위에 따라 필터링하고 최신순으로 정렬한다")
	void findByVisibilityOrderByCreatedAtDesc() {
		// given
		noteRepository.save(Note.create(user1, null, "비공개글1", "내용", NoteVisibility.PRIVATE, true));
		noteRepository.save(Note.create(user1, null, "비공개글2", "내용", NoteVisibility.PRIVATE, true));
		noteRepository.save(Note.create(user1, null, "공개글1", "내용", NoteVisibility.PUBLIC, true));
		noteRepository.save(Note.create(user2, null, "공개글2", "내용", NoteVisibility.PUBLIC, true));

		// when
		List<Note> result = noteRepository.findByVisibilityOrderByCreatedAtDesc(NoteVisibility.PUBLIC);

		// then
		assertThat(result).hasSize(2);
		// 최신순 보장 (생성 순서의 역순)
		assertThat(result.get(0).getTitle()).isEqualTo("공개글2");
		assertThat(result.get(1).getTitle()).isEqualTo("공개글1");
	}

	@Test
	@DisplayName("findByVisibilityOrderByCreatedAtDesc: 해당 공개 범위의 게시글이 없으면 빈 리스트를 반환한다")
	void findByVisibilityOrderByCreatedAtDesc_returnsEmptyListWhenNoNotes() {
		// given
		// PUBLIC 글 없음

		// when
		List<Note> result = noteRepository.findByVisibilityOrderByCreatedAtDesc(NoteVisibility.PUBLIC);

		// then
		assertThat(result).isEmpty();
	}

	@Nested
	@DisplayName("ExistsNotes")
	class ExistsNotes {

		@Test
		@DisplayName("existsByFolder_IdAndDeletedAtIsNull: 활성 노트가 있으면 true를 반환한다")
		void existsActiveNoteInFolder_returnsTrue() {
			// given
			Folder folder = folderRepository.save(Folder.create(user1, null, "업무"));
			noteRepository.save(Note.create(user1, folder, "title", "content",
					NoteVisibility.PRIVATE, false));

			// when
			boolean result = noteRepository.existsByFolder_IdAndDeletedAtIsNull(folder.getId());

			// then
			assertThat(result).isTrue();
		}

		@Test
		@DisplayName("existsByFolder_IdAndDeletedAtIsNull: 노트가 없으면 false를 반환한다")
		void existsActiveNoteInFolder_returnsFalseWhenNoNoteExists() {
			// given
			Folder folder = folderRepository.save(Folder.create(user1, null, "업무"));

			// when
			boolean result = noteRepository.existsByFolder_IdAndDeletedAtIsNull(folder.getId());

			// then
			assertThat(result).isFalse();
		}

		@Test
		@DisplayName("existsByFolder_IdAndDeletedAtIsNull: 삭제된 노트만 있으면 false를 반환한다")
		void existsActiveNoteInFolder_returnsFalseWhenOnlyDeletedNoteExists() {
			// given
			Folder folder = folderRepository.save(Folder.create(user1, null, "업무"));
			Note note = noteRepository.save(
					Note.create(user1, folder, "title", "content", NoteVisibility.PRIVATE, false)
			);
			note.softDeleteBy(user1.getId());

			// when
			boolean result = noteRepository.existsByFolder_IdAndDeletedAtIsNull(folder.getId());

			// then
			assertThat(result).isFalse();
		}
	}

	private Statistics statistics() {
		return entityManagerFactory.unwrap(SessionFactory.class).getStatistics();
	}
}
