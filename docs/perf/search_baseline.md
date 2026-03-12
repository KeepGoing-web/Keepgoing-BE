# Baseline: 내 글 검색 성능 (MySQL, LIKE)

## 목적
- 개선 전/후 성능 비교를 위해 **베이스라인(latency, p95)**을 확보한다.
- “왜 느린지/어디서 비용이 드는지”를 **EXPLAIN**으로 근거화한다.

---

## 환경
- Runtime: Colima + Docker Compose
- DB: MySQL 8.0 (docker)
- App: Spring Boot (profile=local)
- Endpoint: `GET /api/posts/me/search?keyword=spring&page=0&size=10`
- Auth: Bearer JWT accessToken 사용
- k6: `vus=10`, `duration=30s`  
  (참고: 스크립트에 `sleep(1)` 포함 → iteration_duration ≈ 1s)

---

### 동일 조건 체크리스트(전/후 공정성)
- 데이터: posts=100,000 / keyword(spring) 포함=10% / author_id=1 / deleted_at IS NULL
- API: `GET /api/posts/me/search?keyword=spring&page=0&size=10`
- 정렬: 최신순(`created_at DESC`) + LIMIT 10
- 부하(k6): warmup 10s → measure 30s, vus=10, sleep=1s
- 실행: 각 시나리오 3회(run1~run3) 측정 후 평균(mean) 비교

### warm-up을 두는 이유
- warmup 구간은 DB buffer pool/OS page cache/JIT 워밍업 영향을 줄여, measure 구간의 편차를 낮추기 위함이다.
- 결과 비교는 measure(30s) 구간만 사용한다.

---

## 데이터 세팅
- 기준 사용자(author_id): **1**
  - 근거: `SELECT id, email FROM users ORDER BY id LIMIT 5;` 결과에서 `1 | test@example.com`
- posts: **100,000 rows**
- keyword 분포: **spring 10% 포함** (seed 규칙: `n % 10 == 0`일 때 spring 포함)

### 데이터 증거
- TRUNCATE 이후:
  - `SELECT COUNT(*) FROM posts;` → **0**
- seed 실행 후:
  - `SELECT COUNT(*) FROM posts;` → **100000**

<p align="center">
  <img src="images/user(id=1).png" width="720" alt="users(id=1)">
</p>
<p align="center">
  <img src="images/count=0.png" width="720" alt="posts COUNT=0">
</p>
<p align="center">
  <img src="images/count=100000.png" width="720" alt="posts COUNT=100000">
</p>

### keyword 분포 확인
```sql
SELECT
  COUNT(*) AS total,
  SUM((title LIKE '%spring%') OR (content LIKE '%spring%')) AS matched
FROM posts
WHERE author_id = 1 AND deleted_at IS NULL;
```
<p align="center">
  <img src="images/matched=10000.png" width="720" alt="keyword matched=10000">
</p>

---

## 실행 계획(베이스라인 근거)
### EXPLAIN (LIKE 기반)
쿼리(개념):
```sql
EXPLAIN
SELECT id
FROM posts
WHERE author_id = 1
  AND deleted_at IS NULL
  AND (title LIKE '%spring%' OR content LIKE '%spring%')
ORDER BY created_at DESC
LIMIT 10;
```
- type: ref
- key: idx_posts_author_deleted_created
- rows: 49499
- Extra: Using where; Backward index scan

#### 해석:
- author_id, deleted_at, created_at 기준으로는 인덱스를 타며 정렬도 인덱스 역방향 스캔으로 처리된다.
- 하지만 LIKE '%spring%'는 인덱스를 활용하기 어려워, 조건 필터링을 위해 많은 행(rows≈49k)을 스캔하는 비용이 남는다.
> LIMIT 10이 있어도 비용이 줄지 않을 수 있다. `'%spring%'`(contains) 조건은 인덱스에서 바로 걸러지지 않기 때문에, `author_id=1 AND deleted_at IS NULL`로 좁혀진 후보(≈49k) 내부에서 문자열 비교를 수행하며 “조건을 만족하는 10개”를 찾을 때까지 계속 검사한다. keyword 포함 비율이 낮아질수록(예: 1%) 이 검사 비용은 더 커질 수 있다.


<p align="center">
  <img src="images/explain_baseline.png" width="720" alt="EXPLAIN">
</p>

### EXPLAIN ANALYZE (측정)
```sql
EXPLAIN ANALYZE
SELECT id
FROM posts
WHERE author_id = 1
  AND deleted_at IS NULL
  AND (title LIKE '%spring%' OR content LIKE '%spring%')
ORDER BY created_at DESC
LIMIT 10;
```

#### EXPLAIN ANALYZE 원문 일부(캡처와 동일)
```text
-> Limit: 10 row(s) ... (actual time=0.0338..0.0546 rows=10 loops=1)
  -> Filter: ... (actual time=0.0331..0.0535 rows=10 loops=1)
    -> Index lookup on posts using idx_posts_author_deleted_created ... (actual time=0.03..0.049 rows=10 loops=1)
```

- Index lookup (reverse) on idx_posts_author_deleted_created (author_id=1, deleted_at=NULL)
- estimated rows: 49499
- actual time (index lookup 단계): ~0.03..0.049s, rows=10, loops=1
- actual time (LIMIT 10 전체): ~0.0338..0.0546s, rows=10, loops=1

<p align="center">
  <img src="images/explain_analyze_baseline.png" width="720" alt="EXPLAIN ANALYZE">
</p>

## k6 결과(warm-up 10s + measure 30s)
### 실행 조건
- scenarios: warmup(10s) -> measure(30s)
- vus: 10
- keyword: spring
- size: 10
- sleep: 1s

### 실행 커맨드
```bash
TOKEN="<ACCESS_TOKEN>" KEYWORD="spring" \
  k6 run --summary-export "docs/perf/results/baseline_spring_run1.json" perf/search_my_posts.js
```

### 측정 결과 (phase=measure) - run3
- http_req_duration avg: 15.72ms
- http_req_duration p(95): 18.89ms
- http_reqs: 400 (≈ 10.00 req/s)
- http_req_failed: 0.00%
- 근거: summary-export JSON (baseline_spring_run3.json)

<p align="center">
  <img src="images/k6_baseline.png" width="720" alt="k6 결과">
</p>

### k6 3회 측정 결과(phase=measure) 요약

- 기준: warm-up 10s + measure 30s, vus=10, keyword=spring, size=10, sleep=1s
- 근거: summary-export JSON (run1~run3)

|      run | http_req_duration avg (ms) | http_req_duration p95 (ms) | http_reqs (count) | req/s(대략) | http_req_failed |
|---------:|---------------------------:|---------------------------:|---:|---:|---:|
|        1 |                      16.52 |                      22.30 | 400 | 10.00 | 0.00% |
|        2 |                      14.94 |                      18.00 | 400 | 10.00 | 0.00% |
|        3 |                      15.72 |                      18.89 | 400 | 10.00 | 0.00% |
| **mean** |                  **15.73** |                  **19.73** | **400** | **10.00** | **0.00%** |

- p95≈19.7ms, avg≈15.7ms로 3회 편차가 작아 베이스라인으로 적합
- avg 범위(min~max): 14.94~16.52ms / p95 범위(min~max): 18.00~22.30ms

---

### 참고(측정 스코프)
- 로컬(단일 머신) 환경에서의 측정 결과이며, 배포 환경/네트워크 조건에 따라 수치는 달라질 수 있다.
- 다만 동일 조건에서의 전/후 비교(개선 효과 확인)에는 충분히 유효하다.
