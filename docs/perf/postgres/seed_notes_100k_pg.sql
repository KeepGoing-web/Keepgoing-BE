-- docs/postgres/perf/seed_notes_100k_pg.sql
-- perf@example.com 사용자 기준으로 notes 100,000건 생성
-- keyword 'spring' 10% 포함

-- 기존 perf 사용자 노트 정리
DELETE FROM notes
WHERE author_id = (
    SELECT id FROM users WHERE email = 'perf@example.com'
);

-- seed insert
INSERT INTO notes (
    author_id,
    folder_id,
    title,
    content,
    visibility,
    ai_collectable,
    deleted_at,
    created_at,
    updated_at
)
SELECT
    u.id,
    NULL,
    CASE
        WHEN gs % 10 = 0 THEN 'spring 테스트 제목 ' || gs
        ELSE '일반 제목 ' || gs
        END,
    CASE
        WHEN gs % 10 = 0 THEN 'spring 키워드가 포함된 내용 ' || gs || ' - pg baseline content'
        ELSE '일반 내용 ' || gs || ' - pg baseline content'
        END,
    'PRIVATE',
    FALSE,
    NULL,
    NOW() - ((gs % 100) || ' days')::interval,
    NOW() - ((gs % 100) || ' days')::interval
FROM users u
         CROSS JOIN generate_series(1, 100000) AS gs
WHERE u.email = 'perf@example.com';
