> **[Archive]** 이 문서는 MySQL 8.0 FULLTEXT 기반 벤치마크입니다. PostgreSQL 전환(#55) 이후 FULLTEXT 검색이 제거되어 현재 API와 일치하지 않습니다. 벤치마크 방법론은 추후 PostgreSQL 성능 측정 시 참고할 수 있습니다.

# 결론: 내 글 검색 성능 개선 요약 (LIKE vs FULLTEXT/SCORE vs FULLTEXT/NEWEST)

## 한 줄 결론
- `LIKE(contains)`에서 `FULLTEXT`로 전환하면서 API 레벨 평균/처리량이 크게 개선되었고, 정렬 정책은 **품질(SCORE) vs 성능/단순성(NEWEST)** 트레이드오프 형태로 선택할 수 있다.

---

## 실험 조건(고정)
- 데이터: posts=100,000 / `spring` 포함 10% / `author_id=3` / `deleted_at IS NULL`
- API: `page=0`, `size=10`
- k6: warmup 10s → measure 30s, `vus=10`, `sleep=1s`
- 측정: 3회(run1~run3) 반복 후 **mean(평균) + 범위(min~max)** 기록

---

## k6 결과 요약(phase=measure, mean)

| 항목 | LIKE baseline | FULLTEXT/SCORE | FULLTEXT/NEWEST |
|---:|--------------:|---:|---:|
| avg |      368.27ms | 80.35ms | 64.56ms |
| p95 |      494.40ms | 137.49ms | 146.05ms |
| req/s |          7.33 | 9.27 | 9.20 |
| fail |         0.00% | 0.00% | 0.00% |

### 편차 범위(min~max, phase=measure)
- LIKE: avg 338.23~407.87ms / p95 490.76~497.59ms
- FULLTEXT/SCORE: avg 73.11~85.57ms / p95 131.81~147.30ms
- FULLTEXT/NEWEST: avg 60.21~67.75ms / p95 144.57~148.83ms

### 해석
- **LIKE → FULLTEXT:**  평균 응답시간과 처리량이 개선되었고(요청 실패 0%), `contains` 문자열 비교 비용이 서비스 체감 성능에 영향을 주던 구간을 줄였다. 
    → avg 기준 약 **4.6x 개선**(368.27ms → 80.35ms), 처리량(req/s) 기준 약 **1.26x 개선**(7.33 → 9.27).
- **SCORE vs NEWEST:** NEWEST가 avg는 더 낮게 측정되었지만, p95는 SCORE 대비 비슷하거나 소폭 높았다.  
  이는 `created_at DESC` 정렬(filesort)과 Page의 countQuery, 로컬 환경 편차가 tail에 영향을 줄 수 있음을 시사한다.

---

## DB 근거 요약(EXPLAIN/EXPLAIN ANALYZE)
> 참고: EXPLAIN ANALYZE는 **DB에서 쿼리 1회 단독 실행 실측**이며(캐시/시점/동시성에 따라 초 단위로 크게 변동 가능),  
> k6는 warmup 이후 **API 전체 시간**(인증/컨트롤러/서비스/DB/직렬화/네트워크)을 반복 측정한 통계치다.  
> 본 프로젝트에서는 DB 결과는 **병목 가능성 설명**, 비교 결론은 **k6 지표 중심**으로 정리했다.

### LIKE baseline
- EXPLAIN: `key=idx_posts_author_deleted_created`, `rows≈49,499`, `Using where`/`Backward index scan`
- 의미: author_id/created_at 정렬은 인덱스로 처리 가능하지만, `'%spring%'`(contains) 조건을 인덱스로 처리하기 어려워 **후보 집합 내부 문자열 비교**가 남는다.

### FULLTEXT/SCORE
- EXPLAIN: `type=fulltext`, `key=ft_post_title_content`, `Using filesort`
- 의미: 역인덱스로 후보를 찾은 뒤 `ORDER BY MATCH(...) DESC, created_at DESC`로 정렬하며, **score+created_at 정렬(filesort)** 비용이 남을 수 있다.
- Page의 countQuery: 키워드가 흔한(10%) 워크로드에서 `COUNT(*)`가 rows=10000을 스캔/집계하며 **병목이 될 수 있음**(실측 근거 존재)

### FULLTEXT/NEWEST
- EXPLAIN: `type=fulltext`, `Ft_hints: no_ranking`, `Using filesort`
- 의미: `no_ranking`으로 **score(랭킹) 계산을 생략**하지만, `created_at DESC` 정렬은 여전히 필요할 수 있어 filesort가 남을 수 있다.
- Page의 countQuery는 동일하게 존재(운영 전략 필요).

---

## 운영 의사결정 가이드
### 정렬 정책 선택
- **검색 품질(관련도 랭킹) 우선**: `FULLTEXT/SCORE`
  - 장점: relevance 기반 정렬
  - 주의: score 정렬(filesort) + countQuery 비용이 커질 수 있음

- **성능/단순성/최신글 UX 우선**: `FULLTEXT/NEWEST`
  - 장점: `no_ranking`으로 score 계산 생략
  - 주의: 최신순 정렬(filesort)과 countQuery 영향으로 tail(p95)이 크게 줄지 않을 수 있음

### Page vs Slice
- `Page`는 요청 1회당 content query + countQuery를 실행한다.
- totalElements가 꼭 필요하지 않다면 `Slice`로 전환(=countQuery 제거)을 검토하면, FULLTEXT 워크로드에서 **가장 큰 병목 요인 중 하나(countQuery)**를 제거할 수 있다.
- `Slice`는 `COUNT(*)`를 호출하지 않으므로, 키워드가 흔한(예: 10%) 상황에서 countQuery로 인한 tail latency 리스크를 줄이는 데 효과적일 것 이라고 예상된다.

---

## 다음 단계(추후)
- 실제 트래픽 가정에 맞춰 vus 단계(10/30/50)로 확장 측정
- keyword 분포 다양화(흔함/희귀/없는 키워드)
- 운영 환경에서 DB CPU/slow query log/latency(p99)까지 함께 관측

---

## 상세 리포트
- LIKE baseline: [search_like.md](search_like.md)
- FULLTEXT/SCORE: [search_fulltext_score.md](search_fulltext_score.md)
- FULLTEXT/NEWEST: [search_fulltext_newest.md](search_fulltext_newest.md)
- k6 summary-export JSON은 변경이 잦아 기본적으로 커밋하지 않고, 본 문서는 추출한 수치(mean/min/max)와 캡처를 근거로 작성한다.
