USE keepgoing;

SET SESSION cte_max_recursion_depth = 100000;

-- 1) 숫자 시퀀스 테이블 생성 (CTE는 SELECT 구문 안에서만 사용)
DROP TEMPORARY TABLE IF EXISTS seq_100000;
CREATE TEMPORARY TABLE seq_100000 AS
WITH RECURSIVE seq AS (
    SELECT 1 AS n
    UNION ALL
    SELECT n + 1 FROM seq WHERE n < 100000
)
SELECT n FROM seq;

-- 2) 시퀀스를 이용해 posts 100,000건 삽입
INSERT INTO posts (
    author_id,
    title,
    content,
    visibility,
    ai_collectable,
    created_at,
    updated_at,
    deleted_at
)
SELECT
    1 AS author_id,
    CASE
        WHEN (n % 10 = 0) THEN CONCAT('spring 테스트 제목 ', n)
        ELSE CONCAT('일반 제목 ', n)
        END AS title,
    CASE
        WHEN (n % 10 = 0) THEN CONCAT('spring 키워드가 포함된 내용 ', n, ' - mysql like baseline')
        ELSE CONCAT('일반 내용 ', n, ' - baseline content')
        END AS content,
    'PUBLIC' AS visibility,
    0 AS ai_collectable,
    DATE_SUB(NOW(6), INTERVAL (n % 100) DAY) AS created_at,
    DATE_SUB(NOW(6), INTERVAL (n % 100) DAY) AS updated_at,
    NULL AS deleted_at
FROM seq_100000;

-- (선택) 임시 테이블 정리
DROP TEMPORARY TABLE IF EXISTS seq_100000;