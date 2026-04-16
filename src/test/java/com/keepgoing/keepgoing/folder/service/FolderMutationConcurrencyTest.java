package com.keepgoing.keepgoing.folder.service;

import com.keepgoing.keepgoing.folder.domain.Folder;
import com.keepgoing.keepgoing.folder.repository.FolderRepository;
import com.keepgoing.keepgoing.folder.service.dto.FolderCreateCommand;
import com.keepgoing.keepgoing.folder.service.dto.FolderMoveCommand;
import com.keepgoing.keepgoing.folder.service.dto.FolderSummaryResult;
import com.keepgoing.keepgoing.note.domain.NoteVisibility;
import com.keepgoing.keepgoing.note.repository.NoteRepository;
import com.keepgoing.keepgoing.note.service.NoteService;
import com.keepgoing.keepgoing.note.service.dto.NoteCreateCommand;
import com.keepgoing.keepgoing.note.service.dto.NoteDetailResult;
import com.keepgoing.keepgoing.note.service.dto.NoteMoveCommand;
import com.keepgoing.keepgoing.user.domain.User;
import com.keepgoing.keepgoing.user.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(webEnvironment = WebEnvironment.NONE)
public class FolderMutationConcurrencyTest {

	@Autowired
	FolderService folderService;

	@Autowired
	NoteService noteService;

	@Autowired
	FolderRepository folderRepository;

	@Autowired
	NoteRepository noteRepository;

	@Autowired
	UserRepository userRepository;

	private User user;
	private Long userId;
	private static final long READY_TIMEOUT_SECONDS = 5L;
	private static final long DONE_TIMEOUT_SECONDS = 10L;
	private static final long TERMINATION_TIMEOUT_SECONDS = 5L;

	@BeforeEach
	void setUp() {
		noteRepository.deleteAll();
		folderRepository.deleteAll();
		userRepository.deleteAll();

		user = userRepository.save(User.create("test@test.com", "tester"));
		userId = user.getId();
	}

	@Test
	@DisplayName("deleteFolder와 createNote가 동시에 실행되면 둘 다 성공하지 않는다")
	void deleteFolder_and_createNote_areSerialized() throws Exception {
		Folder folder = folderRepository.save(Folder.create(user, null, "target"));
		Long folderId = folder.getId();

		ExecutorService executor = Executors.newFixedThreadPool(2);
		CountDownLatch readyLatch = new CountDownLatch(2);
		CountDownLatch startLatch = new CountDownLatch(1);
		CountDownLatch doneLatch = new CountDownLatch(2);

		AtomicInteger deleteSuccess = new AtomicInteger();
		AtomicInteger createSuccess = new AtomicInteger();
		AtomicInteger failureCount = new AtomicInteger();

		executor.submit(() -> {
			try {
				readyLatch.countDown();
				startLatch.await();
				folderService.deleteFolder(userId, folderId);

				deleteSuccess.incrementAndGet();
			} catch (Exception e) {
				failureCount.incrementAndGet();
			} finally {
				doneLatch.countDown();
			}
		});

		executor.submit(() -> {
			try {
				readyLatch.countDown();
				startLatch.await();
				noteService.createNote(NoteCreateCommand.builder()
						.userId(userId)
						.folderId(folderId)
						.title("title")
						.content("content")
						.visibility(NoteVisibility.PRIVATE)
						.aiCollectable(false)
						.build());
				createSuccess.incrementAndGet();
			} catch (Exception e) {
				failureCount.incrementAndGet();
			} finally {
				doneLatch.countDown();
			}
		});

		awaitConcurrentTasks(readyLatch, startLatch, doneLatch, executor);

		assertThat(deleteSuccess.get() + createSuccess.get()).isEqualTo(1);
		assertThat(failureCount.get()).isEqualTo(1);
	}

	@Test
	@DisplayName("deleteFolder와 createFolder가 동시에 실행되면 둘 다 성공하지 않는다")
	void deleteFolder_and_createFolder_areSerialized() throws Exception {
		Folder folder = folderRepository.save(Folder.create(user, null, "target"));
		Long folderId = folder.getId();

		ExecutorService executor = Executors.newFixedThreadPool(2);
		CountDownLatch readyLatch = new CountDownLatch(2);
		CountDownLatch startLatch = new CountDownLatch(1);
		CountDownLatch doneLatch = new CountDownLatch(2);

		AtomicInteger deleteSuccess = new AtomicInteger();
		AtomicInteger createSuccess = new AtomicInteger();
		AtomicInteger failureCount = new AtomicInteger();

		executor.submit(() -> {
			try {
				readyLatch.countDown();
				startLatch.await();
				folderService.deleteFolder(userId, folderId);
				deleteSuccess.incrementAndGet();
			} catch (Exception e) {
				failureCount.incrementAndGet();
			} finally {
				doneLatch.countDown();
			}
		});

		executor.submit(() -> {
			try {
				readyLatch.countDown();
				startLatch.await();
				folderService.createFolder(new FolderCreateCommand(userId, folderId, "child"));
				createSuccess.incrementAndGet();
			} catch (Exception e) {
				failureCount.incrementAndGet();
			} finally {
				doneLatch.countDown();
			}
		});

		awaitConcurrentTasks(readyLatch, startLatch, doneLatch, executor);

		assertThat(deleteSuccess.get() + createSuccess.get()).isEqualTo(1);
		assertThat(failureCount.get()).isEqualTo(1);
	}

	@Test
	@DisplayName("deleteFolder와 moveNote(target folder)가 동시에 실행되면 둘 다 성공하지 않는다")
	void deleteFolder_and_moveNoteToTargetFolder_areSerialized() throws Exception {
		Folder sourceFolder = folderRepository.save(Folder.create(user, null, "source"));
		Folder targetFolder = folderRepository.save(Folder.create(user, null, "target"));

		Long sourceFolderId = sourceFolder.getId();
		Long targetFolderId = targetFolder.getId();
		Long noteId = noteService.createNote(NoteCreateCommand.builder()
				.userId(userId)
				.folderId(sourceFolderId)
				.title("title")
				.content("content")
				.visibility(NoteVisibility.PRIVATE)
				.aiCollectable(false)
				.build())
				.noteId();

		ExecutorService executor = Executors.newFixedThreadPool(2);
		CountDownLatch readyLatch = new CountDownLatch(2);
		CountDownLatch startLatch = new CountDownLatch(1);
		CountDownLatch doneLatch = new CountDownLatch(2);

		AtomicInteger deleteSuccess = new AtomicInteger();
		AtomicInteger moveSuccess = new AtomicInteger();
		AtomicInteger failureCount = new AtomicInteger();

		executor.submit(() -> {
			try {
				readyLatch.countDown();
				startLatch.await();
				folderService.deleteFolder(userId, targetFolderId);
				deleteSuccess.incrementAndGet();
			} catch (Exception e) {
				failureCount.incrementAndGet();
			} finally {
				doneLatch.countDown();
			}
		});

		executor.submit(() -> {
			try {
				readyLatch.countDown();
				startLatch.await();
				noteService.moveNote(new NoteMoveCommand(noteId, userId, targetFolderId));
				moveSuccess.incrementAndGet();
			} catch (Exception e) {
				failureCount.incrementAndGet();
			} finally {
				doneLatch.countDown();
			}
		});

		awaitConcurrentTasks(readyLatch, startLatch, doneLatch, executor);

		assertThat(deleteSuccess.get() + moveSuccess.get()).isEqualTo(1);
		assertThat(failureCount.get()).isEqualTo(1);
	}

	@Test
	@DisplayName("deleteFolder와 moveFolder(target parent)가 동시에 실행되면 둘 다 성공하지 않는다")
	void deleteFolder_and_moveFolderToTargetParent_areSerialized() throws Exception {
		Folder movingFolder = folderRepository.save(Folder.create(user, null, "moving"));
		Folder targetParent = folderRepository.save(Folder.create(user, null, "target"));

		Long movingFolderId = movingFolder.getId();
		Long targetParentId = targetParent.getId();

		ExecutorService executor = Executors.newFixedThreadPool(2);
		CountDownLatch readyLatch = new CountDownLatch(2);
		CountDownLatch startLatch = new CountDownLatch(1);
		CountDownLatch doneLatch = new CountDownLatch(2);

		AtomicInteger deleteSuccess = new AtomicInteger();
		AtomicInteger moveSuccess = new AtomicInteger();
		AtomicInteger failureCount = new AtomicInteger();

		executor.submit(() -> {
			try {
				readyLatch.countDown();
				startLatch.await();
				folderService.deleteFolder(userId, targetParentId);
				deleteSuccess.incrementAndGet();
			} catch (Exception e) {
				failureCount.incrementAndGet();
			} finally {
				doneLatch.countDown();
			}
		});

		executor.submit(() -> {
			try {
				readyLatch.countDown();
				startLatch.await();
				folderService.moveFolder(new FolderMoveCommand(userId, movingFolderId, targetParentId));
				moveSuccess.incrementAndGet();
			} catch (Exception e) {
				failureCount.incrementAndGet();
			} finally {
				doneLatch.countDown();
			}
		});

		awaitConcurrentTasks(readyLatch, startLatch, doneLatch, executor);

		assertThat(deleteSuccess.get() + moveSuccess.get()).isEqualTo(1);
		assertThat(failureCount.get()).isEqualTo(1);
	}

	@Test
	@DisplayName("source 폴더에서 note를 빼는 move와 delete가 함께 와도 source invariant를 유지한다")
	void moveNoteOutOfSourceFolder_and_deleteSourceFolder_preserveInvariant() throws Exception {
		Folder sourceFolder = folderRepository.save(Folder.create(user, null, "source"));
		Long sourceFolderId = sourceFolder.getId();

		Long noteId = noteService.createNote(NoteCreateCommand.builder()
				.userId(userId)
				.folderId(sourceFolderId)
				.title("title")
				.content("content")
				.visibility(NoteVisibility.PRIVATE)
				.aiCollectable(false)
				.build())
				.noteId();

		ExecutorService executor = Executors.newFixedThreadPool(2);
		CountDownLatch readyLatch = new CountDownLatch(2);
		CountDownLatch startLatch = new CountDownLatch(1);
		CountDownLatch doneLatch = new CountDownLatch(2);

		AtomicInteger deleteSuccess = new AtomicInteger();
		AtomicInteger deleteFailure = new AtomicInteger();
		AtomicInteger moveSuccess = new AtomicInteger();
		AtomicInteger moveFailure = new AtomicInteger();

		executor.submit(() -> {
			try {
				readyLatch.countDown();
				startLatch.await();
				folderService.deleteFolder(userId, sourceFolderId);
				deleteSuccess.incrementAndGet();
			} catch (Exception e) {
				deleteFailure.incrementAndGet();
			} finally {
				doneLatch.countDown();
			}
		});

		executor.submit(() -> {
			try {
				readyLatch.countDown();
				startLatch.await();
				noteService.moveNote(new NoteMoveCommand(noteId, userId, null));
				moveSuccess.incrementAndGet();
			} catch (Exception e) {
				moveFailure.incrementAndGet();
			} finally {
				doneLatch.countDown();
			}
		});

		awaitConcurrentTasks(readyLatch, startLatch, doneLatch, executor);

		NoteDetailResult note = noteService.getNote(userId, noteId);
		boolean sourceDeleted = folderRepository.findById(sourceFolderId)
				.orElseThrow()
				.isDeleted();

		assertThat(moveSuccess.get()).isEqualTo(1);
		assertThat(moveFailure.get()).isZero();
		assertThat(deleteSuccess.get() + deleteFailure.get()).isEqualTo(1);
		assertThat(note.folderId()).isNull();
		assertThat(sourceDeleted).isEqualTo(deleteSuccess.get() == 1);
	}

	@Test
	@DisplayName("source 부모에서 child를 빼는 move와 delete가 함께 와도 source invariant를 유지한다")
	void moveFolderOutOfSourceParent_and_deleteSourceParent_preserveInvariant() throws Exception {
		Folder sourceParent = folderRepository.save(Folder.create(user, null, "source-parent"));
		Long sourceParentId = sourceParent.getId();

		Folder child = folderRepository.save(Folder.create(user, sourceParent, "child"));
		Long childFolderId = child.getId();

		ExecutorService executor = Executors.newFixedThreadPool(2);
		CountDownLatch readyLatch = new CountDownLatch(2);
		CountDownLatch startLatch = new CountDownLatch(1);
		CountDownLatch doneLatch = new CountDownLatch(2);

		AtomicInteger deleteSuccess = new AtomicInteger();
		AtomicInteger deleteFailure = new AtomicInteger();
		AtomicInteger moveSuccess = new AtomicInteger();
		AtomicInteger moveFailure = new AtomicInteger();

		executor.submit(() -> {
			try {
				readyLatch.countDown();
				startLatch.await();
				folderService.deleteFolder(userId, sourceParentId);
				deleteSuccess.incrementAndGet();
			} catch (Exception e) {
				deleteFailure.incrementAndGet();
			} finally {
				doneLatch.countDown();
			}
		});

		executor.submit(() -> {
			try {
				readyLatch.countDown();
				startLatch.await();
				folderService.moveFolder(new FolderMoveCommand(userId, childFolderId, null));
				moveSuccess.incrementAndGet();
			} catch (Exception e) {
				moveFailure.incrementAndGet();
			} finally {
				doneLatch.countDown();
			}
		});

		awaitConcurrentTasks(readyLatch, startLatch, doneLatch, executor);

		List<FolderSummaryResult> rootFolders = folderService.getFolders(userId, null);
		boolean sourceDeleted = folderRepository.findById(sourceParentId)
				.orElseThrow()
				.isDeleted();

		assertThat(moveSuccess.get()).isEqualTo(1);
		assertThat(moveFailure.get()).isZero();
		assertThat(deleteSuccess.get() + deleteFailure.get()).isEqualTo(1);
		assertThat(rootFolders).anySatisfy(folder ->
				assertThat(folder.folderId()).isEqualTo(childFolderId)
		);
		assertThat(sourceDeleted).isEqualTo(deleteSuccess.get() == 1);
	}

	private void awaitConcurrentTasks(
			CountDownLatch readyLatch,
			CountDownLatch startLatch,
			CountDownLatch doneLatch,
			ExecutorService executor
	) throws InterruptedException {
		try {
			assertThat(readyLatch.await(READY_TIMEOUT_SECONDS, TimeUnit.SECONDS))
					.as("worker threads did not become ready in time")
					.isTrue();

			startLatch.countDown();

			assertThat(doneLatch.await(DONE_TIMEOUT_SECONDS, TimeUnit.SECONDS))
					.as("concurrent tasks did not finish in time")
					.isTrue();
		} finally {
			executor.shutdownNow();
			assertThat(executor.awaitTermination(TERMINATION_TIMEOUT_SECONDS, TimeUnit.SECONDS))
					.as("executor did not terminate in time")
					.isTrue();
		}
	}
}
