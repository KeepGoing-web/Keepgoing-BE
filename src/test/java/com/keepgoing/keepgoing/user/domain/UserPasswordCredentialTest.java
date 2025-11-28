package com.keepgoing.keepgoing.user.domain;

import static org.assertj.core.api.Assertions.assertThat;

import com.keepgoing.keepgoing.global.config.JpaAuditingConfig;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.context.annotation.Import;

/*
    엔티티 매핑 테스트 겸 JPA 동작 방식 학습을 위한 테스트
 */
@Slf4j
@DataJpaTest
@DisplayName("UserPasswordCredential 엔티티 매핑 테스트")
@Import({JpaAuditingConfig.class})
class UserPasswordCredentialTest {

    @Autowired
    TestEntityManager entityManager;

    @Test
    @DisplayName("@MapsId를 사용한 Shared Primary key 매핑이 올바르게 동작한다.")
    void mapsId_mapping() {
        // given

        /*
            1) User 엔티티 영속화 & IDENTITY 전략

            - User는 @GeneratedValue(strategy = IDENTITY)를 사용한다.
            - IDENTITY 전략에서는 PK를 DB가 생성하므로, 실제 insert가 나가야 id 값을 알 수 있다.
            - Hibernate 구현에서는 persist() 시점에 insert 쿼리를 실행해서 곧바로 id를 채워 넣는 동작을 볼 수 있다.
         */
        User user = User.create("test@example.com", "test");
        log.info("User create userId:{}", user.getId()); // user.id = null
        entityManager.persist(user);
        log.info("JPA persist userId:{}", user.getId()); // user.id = 1
        entityManager.flush();

        /*
            2) @MapsId를 사용하는 UserPasswordCredential 생성

            - UserPasswordCredential는 Shared Primary Key 구조를 사용한다.
            - @MapsId 덕분에, 이 엔티티의 PK는 연관된 User의 id를 그대로 따라가야 한다.
         */
        UserPasswordCredential credential = UserPasswordCredential.create(user, user.getEmail(), "HASH");
        /*
            - credential을 persist() 하면, 영속성 컨텍스트에 엔티티가 등록되는 과정에서
              JPA 구현체는 @MapsId 메타데이터를 보고 user.id 값을 credential의 PK로 복사한다.
            - 이 시점에서 credential.id == user.id 여야 한다.
            - persistAndFlush()는 엔티티를 영속화한 뒤, 즉시 flush까지 수행한다.
         */

        // when
        entityManager.persistAndFlush(credential);
        entityManager.clear();

        // then
        UserPasswordCredential found = entityManager.find(
                UserPasswordCredential.class,
                user.getId()
        );
        // @MapsId로 ID가 User의 ID와 같은지 확인.
        assertThat(found.getUserId()).isEqualTo(user.getId());
        assertThat(found.getUser().getId()).isEqualTo(user.getId());
    }
}