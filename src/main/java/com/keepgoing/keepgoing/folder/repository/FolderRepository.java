package com.keepgoing.keepgoing.folder.repository;

import com.keepgoing.keepgoing.folder.domain.Folder;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

public interface FolderRepository extends JpaRepository<Folder, Long> {

	Optional<Folder> findByIdAndDeletedAtIsNull(Long id);

	@Query("""
			select f
			from Folder f
			where f.owner.id = :ownerId
			 and f.parent is NULL
			 and f.deletedAt is NULL
			order by f.name ASC
			""")
	List<Folder> findRootFolders(Long ownerId);

	@Query("""
			select f
			from Folder f
			where f.owner.id = :ownerId
			 and f.parent.id = :parentId
			 and f.deletedAt is NULL
			order by f.name ASC
			""")
	List<Folder> findChildFolders(Long ownerId, Long parentId);

	@Query("""
			select count(f) > 0
			from Folder f
			where f.owner.id = :userId
			 and f.parent is null
			 and f.name = :name
			 and f.deletedAt is null
			""")
	boolean existsRootFolder(Long userId, String name);

	@Query("""
			select count(f) > 0
			from Folder f
			where f.owner.id = :userId
			 and f.parent.id = :parentId
			 and f.name = :name
			 and f.deletedAt is null
			""")
	boolean existsChildFolder(Long userId, Long parentId, String name);
}
