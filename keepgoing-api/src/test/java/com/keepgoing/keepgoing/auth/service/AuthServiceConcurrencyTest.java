package com.keepgoing.keepgoing.auth.service;

import static org.assertj.core.api.Assertions.assertThat;

import com.keepgoing.keepgoing.auth.service.dto.SignupCommand;
import com.keepgoing.keepgoing.support.DatabaseCleaner;
import com.keepgoing.keepgoing.support.PostgreSqlTestContainerSupport;
import com.keepgoing.keepgoing.user.repository.UserRepository;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment;

@SpringBootTest(webEnvironment = WebEnvironment.NONE)
@Slf4j
public class AuthServiceConcurrencyTest extends PostgreSqlTestContainerSupport {

	@Autowired
	AuthService authService;

	@Autowired
	UserRepository userRepository;

	@Autowired
	DatabaseCleaner databaseCleaner;

	@BeforeEach
	void setup() {
		databaseCleaner.clean();
	}

	@AfterEach
	void cleanup() {
		databaseCleaner.clean();
	}

	@Test
	@DisplayName("동시에 같은 이메일로 회원가입 시 하나만 성공해야 한다.")
	void concurrentSignupStartTogether() throws InterruptedException {
		String email = "test@example.com";

		int threadCount = 10;

		ExecutorService executor = Executors.newFixedThreadPool(threadCount);
		CountDownLatch readyLatch = new CountDownLatch(threadCount);  // 준비 완료
		CountDownLatch startLatch = new CountDownLatch(1);            // 시작 신호
		CountDownLatch doneLatch = new CountDownLatch(threadCount);   // 완료 대기

		AtomicInteger successCount = new AtomicInteger(0);
		AtomicInteger failCount = new AtomicInteger(0);

		for (int i = 0; i < threadCount; i++) {
			executor.submit(() -> {
				try {
					readyLatch.countDown();      // 준비 완료 신호
					startLatch.await();          // 시작 신호 대기 (모두 여기서 대기)

					authService.signup(new SignupCommand(email, "User", "P@ssw0rd!"));
					successCount.incrementAndGet();
				} catch (Exception e) {
					log.error("예외", e);
					failCount.incrementAndGet();
				} finally {
					doneLatch.countDown();
				}
			});
		}

		assertThat(readyLatch.await(5, TimeUnit.SECONDS))           // 모든 스레드 준비 완료 대기
				.as("signup worker threads did not become ready in time")
				.isTrue();
		startLatch.countDown();       // 🚀 동시 시작!
		assertThat(doneLatch.await(10, TimeUnit.SECONDS))
				.as("concurrent signup tasks did not finish in time")
				.isTrue();
		executor.shutdown();

		assertThat(successCount.get()).isEqualTo(1);
		assertThat(failCount.get()).isEqualTo(threadCount - 1);
		assertThat(userRepository.countByEmail(email)).isEqualTo(1);
	}
}
