package com.keepgoing.keepgoing.note.repository;

import com.keepgoing.keepgoing.note.domain.Note;
import com.keepgoing.keepgoing.note.domain.NoteVisibility;
import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface NoteRepository extends JpaRepository<Note, Long> {

	@Override
	@EntityGraph(attributePaths = {"author"})
	Optional<Note> findById(Long id);

	/**
	 * 삭제되지 않은 노트가 해당 폴더에 하나 이상 존재하는지 확인한다.
	 */
	boolean existsByFolder_IdAndDeletedAtIsNull(Long folderId);

	/**
	 * authorId 로 필터해서, 전달받은 Pageable 조건(페이지/정렬)에 맞게 조회한다.
	 */
	@EntityGraph(attributePaths = {"author"})
	Page<Note> findByAuthor_Id(Long authorId, Pageable pageable);

	/**
	 * visibility 값으로 필터해서, createdAt 기준 내림차순으로 정렬해서 찾는다.
	 */
	@EntityGraph(attributePaths = {"author"})
	List<Note> findByVisibilityOrderByCreatedAtDesc(NoteVisibility visibility);

	// TODO: 기존 성능 최적화(Mysql 기반)을 단순 검색으로 전환했으므로 이를 Postgres에 맞게 전환하는 작업 필요
	@Query("""
			SELECT n
			FROM Note n
			WHERE n.visibility = com.keepgoing.keepgoing.note.domain.NoteVisibility.PUBLIC
			  AND (
			    lower(n.title) like concat('%', lower(:keyword), '%')
				  OR lower(n.content) like concat('%', lower(:keyword), '%')
			  )
			ORDER BY n.createdAt DESC, n.id DESC
			""")
	Page<Note> searchPublicNotes(
			@Param("keyword") String keyword,
			Pageable pageable
	);

	// 내 글 검색(작성자 조건 포함)
	@Query("""
			SELECT n
			FROM Note n
			WHERE n.author.id = :authorId
			  AND (LOWER(n.title) LIKE CONCAT('%', LOWER(:keyword), '%')
			       OR LOWER(n.content) LIKE CONCAT('%', LOWER(:keyword), '%'))
			  ORDER BY n.createdAt DESC, n.id DESC
			""")
	Page<Note> searchMyNotes(@Param("authorId") Long authorId,
	                         @Param("keyword") String keyword,
	                         Pageable pageable);
}
