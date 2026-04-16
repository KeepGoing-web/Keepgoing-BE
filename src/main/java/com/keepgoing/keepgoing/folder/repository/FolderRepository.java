package com.keepgoing.keepgoing.folder.repository;

import com.keepgoing.keepgoing.folder.domain.Folder;
import com.keepgoing.keepgoing.folder.repository.dto.FolderTreeRow;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface FolderRepository extends JpaRepository<Folder, Long> {

	Optional<Folder> findByIdAndDeletedAtIsNull(Long id);

	boolean existsByOwner_IdAndParent_IdAndDeletedAtIsNull(Long ownerId, Long parentId);

	@Query("""
			select f
			from Folder f
			where f.owner.id = :ownerId
			 and f.parent is NULL
			 and f.deletedAt is NULL
			order by f.name ASC
			""")
	List<Folder> findRootFolders(@Param("ownerId") Long ownerId);

	@Query("""
			select f
			from Folder f
			where f.owner.id = :ownerId
			 and f.parent.id = :parentId
			 and f.deletedAt is NULL
			order by f.name ASC
			""")
	List<Folder> findChildFolders(@Param("ownerId") Long ownerId,
	                              @Param("parentId") Long parentId);

	@Query("""
			select count(f) > 0
			from Folder f
			where f.owner.id = :userId
			 and f.parent is null
			 and f.name = :name
			 and f.deletedAt is null
			""")
	boolean existsRootFolder(@Param("userId") Long userId,
	                         @Param("name") String name);

	@Query("""
			select count(f) > 0
			from Folder f
			where f.owner.id = :userId
			 and f.parent.id = :parentId
			 and f.name = :name
			 and f.deletedAt is null
			""")
	boolean existsChildFolder(@Param("userId") Long userId,
	                          @Param("parentId") Long parentId,
	                          @Param("name") String name);

	@Query("""
			select new com.keepgoing.keepgoing.folder.repository.dto.FolderTreeRow(f.id, p.id, f.name)
			from Folder f
			left join f.parent p
			where f.owner.id = :ownerId
			  and f.deletedAt is null
			order by f.name asc
			""")
	List<FolderTreeRow> findTreeRows(@Param("ownerId") Long ownerId);

	@Lock(LockModeType.PESSIMISTIC_WRITE)
	@Query("""
			select f
			from Folder f
			where f.id = :targetFolderId
			  and f.deletedAt is null 
			""")
	Optional<Folder> findForUpdate(@Param("targetFolderId") Long targetFolderId);

	@Lock(LockModeType.PESSIMISTIC_WRITE)
	@Query("""
			select f
			from Folder f
			where f.id in :targetFolderIds
			  and f.deletedAt is null
			order by f.id asc
			""")
	List<Folder> findAllForUpdate(@Param("targetFolderIds") Collection<Long> targetFolderIds);
}
