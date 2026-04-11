# EC2 t4g.medium 단일 서버 운영 테스트 초안

## 목적

- AWS에서 운영 테스트를 진행하되, 향후 자체 서버로 이전하기 쉬운 배포 구조를 유지한다.
- AWS 관리형 서비스 의존성을 최소화하고 `Nginx + Spring Boot + PostgreSQL`를 한 VM에서 Docker Compose로 운영한다.
- `t4g.medium`에서 먼저 병목을 드러내고, 필요 시 `t4g.large`로 상향하는 기준선을 마련한다.

## 구성

- EC2: `t4g.medium` (2 vCPU, 4GiB RAM)
- OS: Ubuntu 24.04 LTS 권장
- 컨테이너
  - `nginx`: 외부 80 포트 수신, app reverse proxy
  - `app`: Spring Boot API
  - `postgres`: PostgreSQL 17

## 파일 구성

- `Dockerfile`
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

TLS draft 적용:

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

주의:

- 현재 저장소의 Flyway 스크립트는 **MySQL 기반 레거시 스크립트**가 포함되어 있어 PostgreSQL 운영 테스트에 바로 활성화하지 않는다.
- 빈 PostgreSQL에 최초 부팅해야 할 때만 `.env.prod`에서 `SPRING_JPA_HIBERNATE_DDL_AUTO=update`를 **일시적으로** 사용하고, 스키마가 생성되면 다시 `validate`로 되돌린다.
- `deploy-prod.sh`는 `CHANGE_ME`, `app.example.com` 같은 placeholder 값이 남아 있으면 배포를 중단한다.
- app/db 설정은 `env_file`로 분리해 compose 본문 가독성을 유지한다. compose 레벨 포트/경로 값은 `.env.prod`에서 관리한다.
- HTTP(80)만 쓰는 동안에는 `AUTH_COOKIE_SECURE=false`, `AUTH_COOKIE_SAME_SITE=Lax`를 유지하고, HTTPS 적용 후 `secure=true`, `same-site=None`으로 전환한다.

## 리소스 배분 권장치 (`t4g.medium`)

- OS + Docker + nginx: 약 0.5 ~ 0.8GiB
- PostgreSQL: 약 0.8 ~ 1.0GiB
- Spring Boot JVM heap: 최대 1.0GiB
- 나머지: page cache / 버퍼 / 순간 피크 여유

다음 상황이면 `t4g.large` 상향을 검토한다.

- swap 사용이 지속적으로 발생
- 검색 API p95가 자주 급등
- RAG 적재/검색 작업 중 앱 latency가 흔들림
- PostgreSQL cache hit ratio가 눈에 띄게 낮아짐

## 헬스체크

- nginx: `GET /healthz`
- app: `GET /actuator/health`

운영 배포를 위해 `SecurityConfig`에서 `/actuator/health`는 익명 접근을 허용한다.

## 아키텍처 / 빌드 주의사항

- 대상 서버는 `t4g.medium`이므로 **ARM64(Graviton)** 환경이다.
- 현재 배포 스크립트는 **EC2 서버 안에서 직접 `docker compose up --build`** 하도록 설계되어 있어, 서버에서 빌드하면 별도 플랫폼 지정 없이 ARM64 이미지가 생성된다.
- 향후 CI에서 이미지를 미리 빌드해 배포하려면 `linux/arm64` 대상으로 빌드해야 한다.

## TLS draft

- 초안 파일: `docker-compose.prod.tls.yml`, `infra/nginx/keepgoing.tls.conf.template`
- 목적:
  - 80 포트는 ACME challenge 및 HTTPS redirect 처리
  - 443 포트는 `ssl_certificate`, `ssl_certificate_key`를 사용해 nginx에서 TLS 종료
  - certbot sidecar가 12시간마다 인증서 갱신을 시도
- 기본 mount:
  - `${TLS_CERTS_DIR}` → `/etc/letsencrypt`
  - `${TLS_ACME_CHALLENGE_DIR}` → `/var/www/certbot`

### TLS 적용 전 준비

1. 도메인을 서버로 연결
2. `${TLS_CERTS_DIR}`에 인증서 배치
3. `.env.prod`에서 아래 값을 실제 도메인으로 설정

```env
NGINX_SERVER_NAME=app.example.com
NGINX_CERT_NAME=app.example.com
```

4. `.env.app`에서 아래 값으로 전환

```env
AUTH_COOKIE_SECURE=true
AUTH_COOKIE_SAME_SITE=None
OAUTH_REDIRECT_URI=https://<your-domain>/oauth/callback
CORS_ALLOWED_ORIGINS=https://<your-domain>
```

### 직접 certbot을 쓸 경우 예시

아래 명령은 초안 예시이며, 실제 운영에서는 도메인/경로를 맞춰서 실행한다.

```bash
mkdir -p var/www/certbot var/certs

docker run --rm \
  -v "$(pwd)/var/www/certbot:/var/www/certbot" \
  -v "$(pwd)/var/certs:/etc/letsencrypt" \
  certbot/certbot certonly --webroot \
  -w /var/www/certbot \
  -d example.com -d www.example.com
```

이후 `deploy-prod-tls.sh`로 nginx + certbot sidecar를 함께 기동한다.

## 백업

논리 백업:

```bash
./infra/scripts/backup-postgres.sh ./.env.prod
```

산출물:

- `backups/postgres-<db>-<timestamp>.sql.gz`

복구 예시:

```bash
gunzip -c backups/postgres-keepgoing-20260410-120000.sql.gz \
  | docker compose --env-file .env.prod -f docker-compose.prod.yml exec -T postgres \
      psql -U "$POSTGRES_USER" "$POSTGRES_DB"
```

## 관측 포인트

운영 테스트 시 최소 확인 대상:

- API
  - `/api/notes/me`
  - `/api/notes/me/search`
  - `/api/notes/search`
- 인프라
  - CPU 사용률
  - 메모리 / swap
  - 디스크 사용량 / I/O wait
  - 컨테이너 재시작 여부
- DB
  - slow query
  - connection 수
  - search query latency
- 앱
  - GC pause
  - `/actuator/health`
  - Docker 로그 파일 크기

## 자체 서버 이전 시 그대로 가져갈 수 있는 것

- Dockerfile
- `docker-compose.prod.yml`
- nginx 설정
- 배포/백업 스크립트
- `.env.prod` 기반 운영 방식

## 자체 서버 이전 시 바뀌는 것

- EC2 / Security Group / Elastic IP
- DNS 연결 방식
- EBS 스냅샷 같은 AWS 전용 백업 수단

즉, 앱 운영 방식은 그대로 두고 VM 공급자만 바꾸는 것을 목표로 한다.
