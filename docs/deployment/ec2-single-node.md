# EC2 t4g.medium 단일 서버 운영 테스트 가이드

## 목적

- AWS에서 운영 테스트를 진행하되, 향후 자체 서버로 이전하기 쉬운 배포 구조를 유지한다.
- AWS 관리형 서비스 의존성을 최소화하고 `Nginx + Spring Boot API + PostgreSQL`를 한 VM에서 Docker Compose로 운영한다.
- `t4g.medium`에서 먼저 병목을 드러내고, 필요 시 `t4g.large`로 상향하는 기준선을 마련한다.

## 구성

- EC2: `t4g.medium` (2 vCPU, 4GiB RAM)
- OS: Ubuntu 24.04 LTS 권장
- 컨테이너
  - `nginx`: 외부 80 포트 수신, app reverse proxy
  - `app`: Spring Boot API 모듈 (Java 21)
  - `postgres`: PostgreSQL 17
  - `worker`: 현재 미완성 단계이므로 이 배포 예시에서는 제외

## 파일 구성

- `Dockerfile` (GitHub Actions 이미지 빌드용, 서버에는 복사하지 않음)
- `.github/workflows/deploy-oci.yml`
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

실제 운영에서는 `.env.prod`, `.env.app`, `.env.db`를 서버에만 두고 Git에는 커밋하지 않는다. 최초 서버 구성, TLS 인증서 발급, GHCR 로그인, compose/nginx 파일 배치는 수동으로 끝낸 뒤 CD를 켠다.

## 서버 초기 디렉터리 예시

```bash
/srv/keepgoing
├── .env.app
├── .env.db
├── .env.prod
├── docker-compose.prod.yml
├── docker-compose.prod.tls.yml
├── infra/
│   └── nginx/
└── var/
    ├── certs/
    └── postgres-data/
```

## 최초 수동 배포 절차

```bash
cp .env.prod.example .env.prod
cp .env.app.example .env.app
cp .env.db.example .env.db
# 각 env 파일 값을 실제 운영값으로 수정

# 서버에는 소스 전체가 아니라 compose/nginx/env 파일만 둔다.
# GHCR 로그인과 env 설정을 마친 뒤 최초 1회 수동 배포한다.
chmod +x infra/scripts/deploy-prod.sh infra/scripts/deploy-prod-tls.sh infra/scripts/backup-postgres.sh
./infra/scripts/deploy-prod.sh ./.env.prod
```

기본 실행 커맨드:

```bash
docker compose --env-file .env.prod -f docker-compose.prod.yml pull app
docker compose --env-file .env.prod -f docker-compose.prod.yml up -d
```

중지:

```bash
docker compose --env-file .env.prod -f docker-compose.prod.yml down
```

TLS 적용 (HTTPS):

```bash
./infra/scripts/deploy-prod-tls.sh ./.env.prod
```

## GitHub Actions CD 전제

`.github/workflows/deploy-oci.yml`은 최초 서버 부트스트랩을 대신하지 않는다. 다음 항목은 서버에서 먼저 수동으로 준비되어 있어야 한다.

- `/srv/keepgoing/.env.prod`, `.env.app`, `.env.db`
- `/srv/keepgoing/docker-compose.prod.yml`
- TLS 운영 시 `/srv/keepgoing/docker-compose.prod.tls.yml`과 `infra/nginx/keepgoing.tls.conf.template`
- GHCR private package pull을 위한 `docker login ghcr.io`
- 필요 시 최초 TLS 인증서 발급

이후 `develop` 브랜치에 merge되면 CD가 새 API 이미지를 `linux/arm64`로 빌드/push하고, 서버의 `.env.prod`에 `APP_IMAGE`, `APP_IMAGE_TAG`를 갱신한 뒤 `docker compose pull app && docker compose up -d`를 실행한다.

TLS compose까지 CD에서 사용하려면 최초 TLS 전환 후 서버의 `.env.prod`에 다음 값을 둔다.

```env
DEPLOY_TLS_ENABLED=true
```

`false`이면 CD는 `docker-compose.prod.yml`만 사용한다.

## 환경변수 가이드

`.env.prod` 필수:
- `APP_IMAGE`
- `APP_IMAGE_TAG`
- `DEPLOY_TLS_ENABLED`
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
- `STORAGE_ENDPOINT`
- `STORAGE_ACCESS_KEY`
- `STORAGE_SECRET_KEY`
- `STORAGE_REGION`
- `STORAGE_BUCKET_QUARANTINE`
- `STORAGE_BUCKET_SECURE`

`.env.db` 필수:
- `POSTGRES_DB`
- `POSTGRES_USER`
- `POSTGRES_PASSWORD`

`.env.app` 권장:
- `JAVA_OPTS=-Xms256m -Xmx1024m -XX:+UseG1GC -XX:MaxGCPauseMillis=200`
- `SPRING_JPA_HIBERNATE_DDL_AUTO=validate`
- `IMAGE_PROCESSING_STREAMS_ENABLED=false` (API-only 배포 기준. worker/Redis Streams 운영 시 true로 전환)

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
- 서버에서는 소스를 빌드하지 않고 GHCR 이미지를 pull해서 실행한다.
- GitHub Actions에서 이미지를 빌드하므로 `linux/arm64` 플랫폼을 명시해야 한다.

## TLS 적용

- 초안 파일: `docker-compose.prod.tls.yml`, `infra/nginx/keepgoing.tls.conf.template`
- 목적: 80(HTTP) 요청을 443(HTTPS)으로 리다이렉트하고, certbot sidecar를 통해 인증서를 자동 갱신한다.

### TLS 적용 전 준비
1. 도메인을 서버 IP로 연결
2. `.env.prod`에서 `NGINX_SERVER_NAME`과 `NGINX_CERT_NAME`을 실제 도메인으로 설정
3. `.env.app`에서 `AUTH_COOKIE_SECURE=true`, `AUTH_COOKIE_SAME_SITE=None`, `OAUTH_REDIRECT_URI=https://<domain>/oauth/callback` 등으로 전환

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
- Dockerfile, GitHub Actions CD, docker-compose.prod.yml, nginx 설정, 배포/백업 스크립트, .env 운영 방식
- 즉, 클라우드 의존성을 최소화하여 VM 공급자만 바뀌어도 운영 방식은 그대로 유지된다.
