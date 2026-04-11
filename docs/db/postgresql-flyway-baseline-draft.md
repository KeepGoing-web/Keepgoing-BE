# PostgreSQL DB Migration Guide (Flyway)

## 개요
본 프로젝트는 **PostgreSQL 16+**를 주 데이터베이스로 사용하며, 모든 초기 스키마는 **Flyway**의 `V1__init.sql`을 통해 한 번에 구축됩니다.

## Flyway 마이그레이션 구조
마이그레이션 파일은 `src/main/resources/db/migration/` 경로에 위치합니다.

### 초기 베이스라인 (V1__init.sql)
프로젝트 시작 시 필요한 모든 테이블과 인덱스가 통합되어 있습니다.
- **핵심 테이블**: `users`, `folders`, `notes` (기존 posts에서 명칭 변경)
- **인증 테이블**: `user_oauth_accounts`, `user_password_credentials`
- **검색 최적화**: `pg_trgm` 확장을 활용한 GIN 인덱스 (`title`, `content` 대상)
- **쿼리 최적화**: `deleted_at` 필드를 포함한 부분 인덱스(Partial Index) 적용

## 운영 및 변경 정책
1. **스키마 변경**: 향후 추가되는 모든 변경 사항(테이블 추가, 컬럼 수정 등)은 `V2`, `V3`와 같이 새로운 버전 파일을 생성하여 관리합니다.
2. **ddl-auto 설정**:
   - 운영 환경: `validate` (Flyway가 스키마를 생성하고 Hibernate가 검증)
   - 개발 환경: `update` 또는 `none` (Flyway 자동 적용 활용)

## 주의 사항
- `V1__init.sql`은 초기 구축용 파일이므로, 이미 운영 중인 DB에서는 이 파일을 직접 수정해서는 안 됩니다.
- 모든 스키마 변경은 반드시 새로운 마이그레이션 파일을 통해서만 수행합니다.
