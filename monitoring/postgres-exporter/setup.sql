-- postgres_exporter 전용 계정 생성 (운영 서버 DB에서 한 번 실행)
-- 사용법: psql -U keepgoing -d keepgoing -f setup.sql

CREATE USER postgres_exporter WITH PASSWORD 'CHANGE_ME';
GRANT CONNECT ON DATABASE keepgoing TO postgres_exporter;
GRANT pg_monitor TO postgres_exporter;
