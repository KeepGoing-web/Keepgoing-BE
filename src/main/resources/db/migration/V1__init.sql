CREATE TABLE posts (
                       id             BIGINT NOT NULL AUTO_INCREMENT,
                       author_id      BIGINT NOT NULL,

                       title          VARCHAR(200) NOT NULL,
                       content        MEDIUMTEXT NOT NULL,

                       visibility     VARCHAR(20) NOT NULL DEFAULT 'PRIVATE',
                       ai_collectable TINYINT(1) NOT NULL DEFAULT 0,

                       created_at     DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
                       updated_at     DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),

                       deleted_at     DATETIME(6) NULL,

                       PRIMARY KEY (id),

                       INDEX idx_posts_author_deleted_created (author_id, deleted_at, created_at),
                       INDEX idx_posts_visibility_deleted_created (visibility, deleted_at, created_at)
) ENGINE=InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_0900_ai_ci;