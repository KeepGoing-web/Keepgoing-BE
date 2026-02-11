-- src/main/resources/db/migration/V2__create_user_auth_tables.sql
-- User / UserOAuthAccount / UserPasswordCredential 엔티티 기반 (MySQL 8.x)

-- 1) users
CREATE TABLE users (
                       id            BIGINT NOT NULL AUTO_INCREMENT,
                       email         VARCHAR(320) NOT NULL,
                       name          VARCHAR(100) NOT NULL,

                       role          VARCHAR(10)  NOT NULL DEFAULT 'USER',
                       status        VARCHAR(10)  NOT NULL DEFAULT 'ACTIVE',

                       last_login_at DATETIME(6) NULL,

                       created_at    DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
                       updated_at    DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),

                       PRIMARY KEY (id),

                       CONSTRAINT uq_users_email UNIQUE (email)
) ENGINE=InnoDB
  DEFAULT CHARSET=utf8mb4
  COLLATE=utf8mb4_0900_ai_ci;

-- 2) user_oauth_accounts
CREATE TABLE user_oauth_accounts (
                                     id               BIGINT NOT NULL AUTO_INCREMENT,
                                     user_id          BIGINT NOT NULL,

                                     provider         VARCHAR(20)  NOT NULL,
                                     provider_user_id VARCHAR(190) NOT NULL,

                                     email            VARCHAR(320) NULL,
                                     avatar_url       VARCHAR(500) NULL,

                                     created_at       DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
                                     updated_at       DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),

                                     PRIMARY KEY (id),

                                     CONSTRAINT uq_user_oauth_provider_user UNIQUE (provider, provider_user_id),

                                     CONSTRAINT fk_user_oauth_accounts_user
                                         FOREIGN KEY (user_id) REFERENCES users(id)
                                             ON DELETE CASCADE
) ENGINE=InnoDB
  DEFAULT CHARSET=utf8mb4
  COLLATE=utf8mb4_0900_ai_ci;

CREATE INDEX idx_user_oauth_accounts_user_id
    ON user_oauth_accounts (user_id);

-- 3) user_password_credentials (Shared Primary Key: PK = FK)
CREATE TABLE user_password_credentials (
                                           user_id             BIGINT NOT NULL,

                                           login_id            VARCHAR(320) NOT NULL,
                                           password_hash       VARCHAR(255) NOT NULL,
                                           password_changed_at DATETIME(6) NOT NULL,

                                           PRIMARY KEY (user_id),

                                           CONSTRAINT uq_user_password_login_id UNIQUE (login_id),

                                           CONSTRAINT fk_user_password_credentials_user
                                               FOREIGN KEY (user_id) REFERENCES users(id)
                                                   ON DELETE CASCADE
) ENGINE=InnoDB
  DEFAULT CHARSET=utf8mb4
  COLLATE=utf8mb4_0900_ai_ci;