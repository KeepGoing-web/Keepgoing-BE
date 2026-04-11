# Keepgoing-BE

## 로컬 실행 (direnv & Docker Compose)

> 본 프로젝트는 **PostgreSQL**과 **Java 21**을 사용합니다.  
> 로컬 환경 설정은 `.envrc.example`을 복사하여 개인화된 `.envrc`를 사용합니다.

### 1) direnv 설치 (macOS)

```bash
brew install direnv
echo 'eval "$(direnv hook zsh)"' >> ~/.zshrc
source ~/.zshrc
```

### 2) .envrc 생성 및 적용

```bash
cp .envrc.example .envrc
direnv allow
```

### 3) 로컬 DB 실행 (PostgreSQL)

로컬 개발 환경에서는 Docker Compose를 통해 PostgreSQL을 실행합니다.

```bash
docker compose up -d postgres
```

- **DB 접속 정보**: `localhost:5432` (기본 설정은 `application-local.yml` 참조)
- **Flyway 마이그레이션**: 애플리케이션 기동 시 자동으로 `V1__init.sql`이 실행되어 스키마가 구성됩니다.

### 4) 애플리케이션 실행

```bash
./gradlew bootRun
```

- Swagger UI: `http://localhost:8080/swagger-ui.html`
- API 검색 예시:
```bash
curl -i -H "Authorization: Bearer <ACCESS_TOKEN>" "http://localhost:8080/api/notes/me/search?keyword=spring&page=0&size=10"
```

### 5) 종료 / 정리
- 애플리케이션 종료: 실행 중인 터미널에서 `Ctrl + C`
- DB 컨테이너 종료:
```bash
docker compose down
```

### 6) 기타 가이드
- 운영 배포 가이드: [docs/deployment/ec2-single-node.md](docs/deployment/ec2-single-node.md)
- DB 마이그레이션 가이드: [docs/db/postgresql-flyway-baseline-draft.md](docs/db/postgresql-flyway-baseline-draft.md)
