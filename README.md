# Keepgoing-BE

KeepGoing은 개인 노트를 폴더 단위로 관리하고, 저장된 노트를 검색과 AI 질문 응답의 근거로 활용하는 백엔드 서비스입니다.

이 저장소는 KeepGoing 서비스의 API 서버, 이미지 처리 worker, 공통 이벤트 모듈, 배포/성능 측정 문서를 포함합니다.

## 서비스 링크

- 배포된 웹 서비스: https://keepgoingapp.vercel.app
- API 경로: `/api` (프론트 배포 도메인 기준)
- 로컬 Swagger UI: http://localhost:8080/swagger-ui.html

## 서비스 개요

KeepGoing은 사용자가 작성한 노트를 안전하게 저장하고, 필요할 때 빠르게 검색하며, AI 패널에서 관련 노트를 근거로 답변을 받을 수 있도록 설계되었습니다.

주요 목표:

- 폴더 기반 노트 관리
- 사용자별 접근 권한과 도메인 무결성 보장
- 검색 API 성능 개선과 측정 근거 문서화
- AI 답변의 근거가 되는 note chunk 인덱싱 및 citation 응답
- 이미지 업로드 요청을 Redis Stream 기반 worker로 비동기 처리
- Docker Compose 기반 로컬/단일 서버 운영 테스트 환경 제공

## 주요 기능

### 인증/사용자

- 이메일/비밀번호 회원가입 및 로그인
- Google OAuth 로그인
- JWT access/refresh token 발급
- cookie 기반 refresh/logout 처리
- 내 정보 조회, 수정, 비밀번호 변경

### 폴더

- 폴더 생성, 목록 조회, 트리 조회
- 폴더 이름 변경, 이동, 삭제
- 폴더 이동 시 순환 참조 검증
- 사용자별 폴더 접근 권한 검증
- 활성 노트가 있는 폴더 삭제 제한

### 노트

- 노트 생성, 상세 조회, 목록 조회, 수정, 삭제
- 내 노트 검색
- 공개 노트 검색
- 노트 폴더 이동
- 노트 제목 변경
- 노트 이미지 업로드 요청 및 처리 상태 관리

### AI 패널

- 현재 선택한 노트 context와 검색된 관련 노트를 함께 사용해 답변 생성
- AI 응답에 참고한 노트 citation 포함
- 접근 가능한 노트, 삭제되지 않은 노트, AI 수집 허용 노트, 인덱싱 완료 노트만 retrieval 대상에 포함
- 노트 변경 후 AI 검색용 chunk를 비동기로 재색인

### 활동 대시보드

- 사용자 활동 이벤트 기록
- 캘린더와 streak 기반 활동 요약 조회

## 아키텍처

```text
Client
  |
  v
Keepgoing API (Spring Boot)
  |
  +-- PostgreSQL
  |     +-- users / folders / notes
  |     +-- ai_note_indexes / ai_note_chunks
  |
  +-- Redis Stream
  |     +-- image processing request/result
  |
  +-- MinIO or S3-compatible storage
  |     +-- uploaded note images
  |
  +-- AI Provider
        +-- Spring AI / ZhipuAI
```

모듈 구조:

```text
Keepgoing-BE
├── keepgoing-api      # Spring Boot API 서버
├── keepgoing-worker   # Redis Stream 기반 이미지 처리 worker
├── keepgoing-common   # API/worker가 공유하는 이벤트와 공통 타입
├── docs               # 성능 측정, RAG 흐름, DB/배포 문서
├── perf               # k6 성능 측정 스크립트
└── infra              # nginx 설정, 배포/백업 스크립트
```

## 기술 스택

| 구분 | 기술 |
| --- | --- |
| Language | Java 21 |
| Framework | Spring Boot 3.5.7 |
| Persistence | Spring Data JPA, PostgreSQL 17, Flyway |
| Auth | Spring Security, OAuth2 Client, JWT |
| AI | Spring AI, ZhipuAI |
| Async/Event | Spring Event, `@TransactionalEventListener`, `@Async` |
| Queue | Redis Stream |
| Storage | S3-compatible object storage, MinIO |
| Test | JUnit 5, Spring Boot Test, Testcontainers |
| Infra | Docker, Docker Compose, Nginx |

## 주요 구현 포인트

### 인증과 사용자 경계

- Spring Security 기반 API 보호
- 이메일/비밀번호 로그인과 Google OAuth 로그인 지원
- JWT access/refresh token 발급과 refresh/logout cookie 처리
- 사용자별 폴더, 노트, 활동 데이터 접근 권한 검증

### 폴더/노트 도메인 규칙

- 폴더 트리 구조와 parent 이동 지원
- 폴더 이동 시 순환 참조 방지
- 활성 노트가 있는 폴더 삭제 제한
- 노트 공개 범위, soft delete, 폴더 이동, 제목 변경 정책 분리

### 비동기 이미지 처리 파이프라인

- API 서버는 이미지 업로드 요청을 저장하고 Redis Stream에 처리 요청을 발행
- worker 모듈은 요청 stream을 소비해 이미지 처리 결과를 result stream으로 발행
- API 서버는 처리 결과를 받아 노트 이미지 상태를 갱신
- MinIO/S3-compatible storage를 사용해 quarantine/secure bucket을 분리

### 검색과 AI 응답 기반

- 내 노트 검색과 공개 노트 검색 API 제공
- 검색 화면에 필요한 응답에는 `Slice` 기반 조회를 제공해 count query 비용을 줄임
- AI 패널은 현재 노트 context와 retrieval 결과를 함께 사용해 citation 포함 응답을 생성
- 노트 변경 후 AI 검색용 index/chunk를 비동기로 재색인

관련 문서:

- [PostgreSQL 검색 성능 개선 요약](docs/perf/postgres/search_notes_pg_summary.md)
- [AI RAG MVP 흐름](docs/ai/rag-flow.md)

### 테스트와 운영 문서

- controller, service, domain 단위 테스트로 주요 정책 검증
- PostgreSQL, Redis, MinIO 연동은 Testcontainers 기반 통합 테스트로 검증
- Flyway migration으로 DB schema 변경 이력 관리
- Docker Compose 기반 로컬 실행, 운영 테스트, 백업 스크립트 문서화

## API 요약

| 기능 | Method | Endpoint |
| --- | --- | --- |
| 회원가입 | `POST` | `/api/auth/signup` |
| 로그인 | `POST` | `/api/auth/login` |
| 토큰 재발급 | `POST` | `/api/auth/refresh` |
| 로그아웃 | `POST` | `/api/auth/logout` |
| 내 정보 조회 | `GET` | `/api/users/me` |
| 폴더 생성 | `POST` | `/api/folders` |
| 폴더 목록 조회 | `GET` | `/api/folders` |
| 폴더 트리 조회 | `GET` | `/api/folders/tree` |
| 폴더 이름 변경 | `PATCH` | `/api/folders/{folderId}` |
| 폴더 이동 | `PATCH` | `/api/folders/{folderId}/parent` |
| 폴더 삭제 | `DELETE` | `/api/folders/{folderId}` |
| 노트 생성 | `POST` | `/api/notes` |
| 노트 상세 조회 | `GET` | `/api/notes/{noteId}` |
| 내 노트 목록 | `GET` | `/api/notes/me` |
| 내 노트 검색(Page) | `GET` | `/api/notes/me/search` |
| 내 노트 검색(Slice) | `GET` | `/api/notes/me/search-slice` |
| 공개 노트 검색 | `GET` | `/api/notes/search` |
| 노트 수정 | `PUT` | `/api/notes/{noteId}` |
| 노트 삭제 | `DELETE` | `/api/notes/{noteId}` |
| 노트 폴더 이동 | `PATCH` | `/api/notes/{noteId}/folder` |
| 노트 제목 변경 | `PATCH` | `/api/notes/{noteId}/title` |
| 노트 이미지 업로드 | `POST` | `/api/notes/{noteId}/images` |
| AI 패널 메시지 | `POST` | `/api/ai/panel/messages` |
| 활동 대시보드 | `GET` | `/api/activities/me/dashboard?from={yyyy-MM-dd}&to={yyyy-MM-dd}` |

Swagger UI:

```text
http://localhost:8080/swagger-ui.html
```

## 로컬 실행

### 1. 요구사항

- Java 21
- Docker / Docker Compose
- direnv 사용 권장

### 2. 환경변수 준비

direnv를 처음 사용하는 경우 shell hook을 먼저 설정합니다.

```bash
brew install direnv
echo 'eval "$(direnv hook zsh)"' >> ~/.zshrc
source ~/.zshrc
```

```bash
cp .envrc.example .envrc
direnv allow
```

`direnv`를 사용하지 않는 경우 `.envrc.example`의 값을 shell 환경변수로 직접 설정하거나 `source .envrc`로 적용합니다.

AI 패널을 실제 호출하려면 `ZHIPUAI_API_KEY` 값을 설정해야 합니다.

### 3. 로컬 인프라 실행

```bash
docker compose up -d postgres redis minio minio-init
```

기본 포트:

| 서비스 | 포트 |
| --- | --- |
| PostgreSQL | `5432` |
| Redis | `6379` |
| MinIO API | `9000` |
| MinIO Console | `9001` |

### 4. API 서버 실행

```bash
./gradlew :keepgoing-api:bootRun
```

### 5. Worker 실행

이미지 처리 worker를 함께 확인하려면 API와 worker 모두에서 Redis Stream listener를 켠 뒤 별도 터미널에서 실행합니다.

```bash
export IMAGE_PROCESSING_STREAMS_ENABLED=true
./gradlew :keepgoing-worker:bootRun
```

### 6. 종료

```bash
docker compose down
```

## 테스트

전체 테스트:

```bash
./gradlew test
```

Colima를 Docker 런타임으로 사용하는 경우 Testcontainers가 Docker socket을 찾을 수 있도록 명시합니다.

```bash
env DOCKER_HOST=unix://${HOME}/.colima/default/docker.sock \
  TESTCONTAINERS_DOCKER_SOCKET_OVERRIDE=/var/run/docker.sock \
  ./gradlew test
```

API 모듈 테스트:

```bash
./gradlew :keepgoing-api:test
```

Worker 모듈 테스트:

```bash
./gradlew :keepgoing-worker:test
```

특정 테스트 실행 예시:

```bash
./gradlew :keepgoing-api:test --tests "com.keepgoing.keepgoing.ai.service.AiNoteIndexRecoverySchedulerTest"
```

## 성능 측정

성능 측정 스크립트는 `perf/` 아래에 있습니다.

```bash
k6 run perf/search_my_notes_pg_like_page.js
k6 run perf/search_my_notes_pg_like_slice.js
```

측정 조건과 결과는 [docs/perf/postgres](docs/perf/postgres)에 정리되어 있습니다.

## 배포

운영 테스트는 단일 VM에서 `Nginx + Spring Boot + PostgreSQL`을 Docker Compose로 실행하는 구조를 기준으로 정리했습니다.

배포 관련 파일:

- [Dockerfile](Dockerfile)
- [docker-compose.prod.yml](docker-compose.prod.yml)
- [docker-compose.prod.tls.yml](docker-compose.prod.tls.yml)
- [infra/nginx/keepgoing.conf](infra/nginx/keepgoing.conf)
- [infra/scripts/deploy-prod.sh](infra/scripts/deploy-prod.sh)
- [infra/scripts/deploy-prod-tls.sh](infra/scripts/deploy-prod-tls.sh)
- [infra/scripts/backup-postgres.sh](infra/scripts/backup-postgres.sh)

운영 테스트 가이드:

- [EC2 단일 서버 운영 테스트 가이드](docs/deployment/ec2-single-node.md)

## 문서

- [AI RAG MVP 흐름](docs/ai/rag-flow.md)
- [PostgreSQL 검색 성능 개선 요약](docs/perf/postgres/search_notes_pg_summary.md)
- [PostgreSQL Flyway baseline 가이드](docs/db/postgresql-flyway-baseline-draft.md)
- [EC2 단일 서버 운영 테스트 가이드](docs/deployment/ec2-single-node.md)

## 현재 한계와 개선 방향

- AI retrieval은 현재 LIKE 기반 MVP이며, 의미 기반 유사도 판단에는 한계가 있습니다.
- RAG 답변 품질을 정량 평가할 테스트셋과 지표가 아직 부족합니다.
- AI 인덱싱 실패율, 재처리 횟수, 처리 지연 시간 같은 운영 지표를 더 세분화할 필요가 있습니다.
- 향후 embedding 기반 vector search, rerank, retrieval score, source alignment 검증을 도입할 계획입니다.
