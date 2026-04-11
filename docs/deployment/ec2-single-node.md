# EC2 t4g.medium 단일 서버 운영 테스트 가이드

## 목적

- AWS에서 운영 테스트를 진행하되, 향후 자체 서버로 이전하기 쉬운 배포 구조를 유지한다.
- AWS 관리형 서비스 의존성을 최소화하고 `Nginx + Spring Boot + PostgreSQL`를 한 VM에서 Docker Compose로 운영한다.
- `t4g.medium`에서 먼저 병목을 드러내고, 필요 시 `t4g.large`로 상향하는 기준선을 마련한다.

## 구성

- EC2: `t4g.medium` (2 vCPU, 4GiB RAM)
- OS: Ubuntu 24.04 LTS 권장
- 컨테이너
  - `nginx`: 외부 80 포트 수신, app reverse proxy
  - `app`: Spring Boot API (Java 21)
  - `postgres`: PostgreSQL 17

## 파일 구성

- `Dockerfile` (Multi-stage build)
- `docker-compose.prod.yml`
- `docker-compose.prod.tls.yml`
- `.env.prod.example`
- `.env.app.example`
- `.env.db.example`
- `infra/nginx/keepgoing.conf`
- `infra/nginx/keepgoing.tls.conf.template`
- `infra/scripts/deploy-prod.sh`
- `infra/scripts/deploy-prod-tls.sh`
- `infra/scripts/backup-postgres.sh`

실제 운영에서는 `.env.prod`, `.env.app`, `.env.db`를 서버에만 두고 Git에는 커밋하지 않는다.

## 서버 초기 디렉터리 예시

```bash
/srv/keepgoing
├── .env.app
├── .env.db
├── .env.prod
├── docker-compose.prod.yml
├── docker-compose.prod.tls.yml
├── infra/
└── var/
    ├── certs/
    └── postgres-data/
```

## 배포 절차

```bash
cp .env.prod.example .env.prod
cp .env.app.example .env.app
cp .env.db.example .env.db
# 각 env 파일 값을 실제 운영값으로 수정

chmod +x infra/scripts/deploy-prod.sh infra/scripts/deploy-prod-tls.sh infra/scripts/backup-postgres.sh
./infra/scripts/deploy-prod.sh ./.env.prod
```

기본 실행 커맨드:

```bash
docker compose --env-file .env.prod -f docker-compose.prod.yml up -d --build
```

중지:

```bash
docker compose --env-file .env.prod -f docker-compose.prod.yml down
```

TLS 적용 (HTTPS):

```bash
./infra/scripts/deploy-prod-tls.sh ./.env.prod
```

## 환경변수 가이드

`.env.prod` 필수:
- `POSTGRES_DATA_DIR`
- `NGINX_HTTP_PORT`
- `NGINX_SERVER_NAME`
- `NGINX_CERT_NAME`

`.env.app` 필수:
- `JWT_SECRET_KEY`
- `OAUTH_GOOGLE_CLIENT_ID`
- `OAUTH_GOOGLE_CLIENT_SECRET`
- `OAUTH_REDIRECT_URI`
- `CORS_ALLOWED_ORIGINS`

`.env.db` 필수:
- `POSTGRES_DB`
- `POSTGRES_USER`
- `POSTGRES_PASSWORD`

`.env.app` 권장:
- `JAVA_OPTS=-Xms256m -Xmx1024m -XX:+UseG1GC -XX:MaxGCPauseMillis=200`
- `SPRING_JPA_HIBERNATE_DDL_AUTO=validate`

## DB 마이그레이션 전략 (Flyway)

- 모든 초기 스키마 구축은 `src/main/resources/db/migration/V1__init.sql`을 통해 관리된다.
- **최초 배포 시**: 빈 PostgreSQL 컨테이너가 기동되면 Flyway가 자동으로 `V1__init.sql`을 실행하여 전체 테이블 및 인덱스(pg_trgm 포함)를 생성한다.
- **운영 환경 설정**: `.env.app`에서 `SPRING_JPA_HIBERNATE_DDL_AUTO=validate`를 사용하여, Flyway가 생성한 스키마와 엔티티 간의 정합성을 검증하도록 설정한다.
- **주의**: 프로젝트 초기 단계이므로 `V1__init.sql` 하나에 모든 베이스라인이 통합되어 있다. 향후 변경 사항은 `V2`, `V3` 순으로 마이그레이션 파일을 추가하여 관리한다.

## 리소스 배분 권장치 (`t4g.medium`)

- OS + Docker + nginx: 약 0.5 ~ 0.8GiB
- PostgreSQL: 약 0.8 ~ 1.0GiB
- Spring Boot JVM heap: 최대 1.0GiB
- 나머지: page cache / 버퍼 / 순간 피크 여유

다음 상황이면 `t4g.large` 상향을 검토한다.
- swap 사용이 지속적으로 발생
- 검색 API p95가 자주 급등
- PostgreSQL cache hit ratio가 눈에 띄게 낮아짐

## 헬스체크

- nginx: `GET /healthz`
- app: `GET /actuator/health`

운영 배포를 위해 `SecurityConfig`에서 `/actuator/health`는 익명 접근을 허용한다.

## 아키텍처 / 빌드 주의사항

- 대상 서버는 `t4g.medium`이므로 **ARM64(Graviton)** 환경이다.
- 서버 안에서 직접 `docker compose up --build` 하므로 서버 환경에 맞는 ARM64 이미지가 자동으로 빌드된다.
- 향후 CI(GitHub Actions)에서 이미지를 미리 빌드해 배포하려면 `linux/arm64` 플랫폼을 명시해야 한다.

## TLS 적용

- 초안 파일: `docker-compose.prod.tls.yml`, `infra/nginx/keepgoing.tls.conf.template`
- 목적: 80(HTTP) 요청을 443(HTTPS)으로 리다이렉트하고, certbot sidecar를 통해 인증서를 자동 갱신한다.

### TLS 적용 전 준비
1. 도메인을 서버 IP로 연결
2. `.env.prod`에서 `NGINX_SERVER_NAME`과 `NGINX_CERT_NAME`을 실제 도메인으로 설정
3. `.env.app`에서 `AUTH_COOKIE_SECURE=true`, `OAUTH_REDIRECT_URI=https://<domain>/oauth/callback` 등으로 전환

## 백업

논리 백업 (PostgreSQL dump):
```bash
./infra/scripts/backup-postgres.sh ./.env.prod
```
산출물: `backups/postgres-<db>-<timestamp>.sql.gz`

## 관측 포인트

- API: `/api/notes/me/search`, `/api/notes/search` 응답 속도
- 인프라: CPU/메모리 사용률, 디스크 I/O wait
- DB: Slow query 발생 여부, Connection 수
- 앱: GC pause, Docker 로그 파일 크기 관리

## 자체 서버 이전 시 유지되는 것
- Dockerfile, docker-compose.prod.yml, nginx 설정, 배포/백업 스크립트, .env 운영 방식
- 즉, 클라우드 의존성을 최소화하여 VM 공급자만 바뀌어도 운영 방식은 그대로 유지된다.
