INSERT INTO users (email, name, role, status, last_login_at, created_at, updated_at)
VALUES ('test@example.com',
        '테스트유저',
        'USER',
        'ACTIVE',
        NULL,
        CURRENT_TIMESTAMP,
        CURRENT_TIMESTAMP);

INSERT INTO users(email, name, role, status, created_at, updated_at)
VALUES ('other@example.com',
        '다른유저',
        'USER',
        'ACTIVE',
        CURRENT_TIMESTAMP,
        CURRENT_TIMESTAMP);

INSERT INTO posts(id, author_id, title, content, visibility, ai_collectable, created_at, updated_at)
VALUES (100,
        2,
        '다른 유저 글',
        '내용',
        'PRIVATE',
        true,
        CURRENT_TIMESTAMP,
        CURRENT_TIMESTAMP);