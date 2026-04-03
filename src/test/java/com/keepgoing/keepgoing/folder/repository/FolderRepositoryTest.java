package com.keepgoing.keepgoing.folder.repository;


import static org.assertj.core.api.Assertions.assertThat;

import com.keepgoing.keepgoing.folder.domain.Folder;
import com.keepgoing.keepgoing.folder.repository.dto.FolderTreeRow;
import com.keepgoing.keepgoing.global.config.JpaAuditingConfig;
import com.keepgoing.keepgoing.user.domain.User;
import com.keepgoing.keepgoing.user.repository.UserRepository;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.context.annotation.Import;

@DataJpaTest
@Import(JpaAuditingConfig.class)
public class FolderRepositoryTest {

	@Autowired
	private FolderRepository folderRepository;

	@Autowired
	private UserRepository userRepository;

	private User user1;
	private User user2;

	@BeforeEach
	void setUp() {
		user1 = userRepository.save(User.create("user1@test.com", "유저1"));
		user2 = userRepository.save(User.create("user2@test.com", "유저2"));
	}

	@Nested
	@DisplayName("FindFolders")
	class FindFolders {

		@Test
		@DisplayName("findRootFolders: 내 루트 폴더를 name ASC로 조회하고 soft delete는 제외한다")
		void findRootFolders_ordersByName_and_excludesDeleted() {
			// given
			Folder a = folderRepository.save(Folder.create(user1, null, "a"));
			Folder b = folderRepository.save(Folder.create(user1, null, "b"));
			Folder other = folderRepository.save(Folder.create(user2, null, "aa"));

			Folder deleted = folderRepository.save(Folder.create(user1, null, "c"));
			deleted.softDelete();
			folderRepository.save(deleted);

			// when
			List<Folder> result = folderRepository.findRootFolders(user1.getId());

			// then
			assertThat(result).extracting(Folder::getName)
					.containsExactly("a", "b");
		}

		@Test
		@DisplayName("findChildFolders: 특정 parent의 자식 폴더를 name ASC로 조회하고 soft delete는 제외한다")
		void findChildFolders_ordersByName_and_excludesDeleted() {
			// given
			Folder parent = folderRepository.save(Folder.create(user1, null, "root"));

			Folder c1 = folderRepository.save(Folder.create(user1, parent, "backend"));
			Folder c2 = folderRepository.save(Folder.create(user1, parent, "frontend"));
			Folder otherUserChild = folderRepository.save(Folder.create(user2, parent, "zzz"));

			Folder deleted = folderRepository.save(Folder.create(user1, parent, "old"));
			deleted.softDelete();
			folderRepository.save(deleted);

			// when
			List<Folder> result = folderRepository.findChildFolders(user1.getId(), parent.getId());

			// then
			assertThat(result).extracting(Folder::getName)
					.containsExactly("backend", "frontend");

		}

		@Test
		@DisplayName("existChildFolder: 내 동일 parent 아래 같은 이름이 있으면 true를 반환한다")
		void existsChildFolder_works() {
			//given
			Folder parent = folderRepository.save(Folder.create(user1, null, "root"));
			folderRepository.save(Folder.create(user1, parent, "spring"));

			// expect
			assertThat(folderRepository.existsChildFolder(user1.getId(), parent.getId(), "spring")).isTrue();
			assertThat(folderRepository.existsChildFolder(user1.getId(), parent.getId(), "DB")).isFalse();
			assertThat(folderRepository.existsChildFolder(user2.getId(), parent.getId(), "spring")).isFalse();
		}

		@Test
		@DisplayName("findTreeRows: 루트 포함 + parentId 매핑 + name ASC + soft delete 제외")
		void findTreeRows_includesRoot_and_mapsParentId_and_ordersByName() {
			// given
			Folder rootA = folderRepository.save(Folder.create(user1, null, "A"));
			Folder rootB = folderRepository.save(Folder.create(user1, null, "B"));
			Folder child = folderRepository.save(Folder.create(user1, rootA, "AA"));

			Folder deleted = folderRepository.save(Folder.create(user1, null, "C"));
			deleted.softDelete();
			folderRepository.save(deleted);

			folderRepository.save(Folder.create(user2, null, "2"));

			// when
			List<FolderTreeRow> rows = folderRepository.findTreeRows(user1.getId());

			// then
			assertThat(rows).extracting(FolderTreeRow::name)
					.containsExactly("A", "AA", "B");

			FolderTreeRow aRow = rows.stream()
					.filter(r -> r.folderId()
							.equals(rootA.getId()))
					.findFirst()
					.orElseThrow();
			assertThat(aRow.parentId()).isNull();

			FolderTreeRow aaRow = rows.stream()
					.filter(r -> r.folderId()
							.equals(child.getId()))
					.findFirst()
					.orElseThrow();
			assertThat(aaRow.parentId()).isEqualTo(rootA.getId());
		}
	}
}
