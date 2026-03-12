-- V3: posts 검색 성능 개선을 위한 FULLTEXT 인덱스 추가

ALTER TABLE posts
    ADD FULLTEXT INDEX ft_post_title_content (title, content);