# 결론: 내 노트 검색 성능 개선 요약 (PostgreSQL, LIKE + pg_trgm, Page vs Slice)

## 한 줄 결론

- 현재 PostgreSQL notes 검색에서는 검색 방식 자체를 바꾸는 것보다, `Page` 응답이 요구하는 count query를 제거해 `Slice`로 전환하는 편이 더 직접적인 성능 개선 효과를 보였다.

---

## 비교 대상

- Baseline: `LIKE + pg_trgm + Page`
- Improved: `LIKE + pg_trgm + Slice`

공통 조건:
- 데이터: `notes 100,000건`
- keyword 분포: `spring 포함 10%`
- 대상 사용자: `perf@example.com` / `author_id=1`
- API: 첫 페이지(`page=0`), `size=10`
- k6: warmup `10s` + measure `30s`, `vus=10`, `sleep=1s`
- 반복: `3회(run1~run3)`

상세 문서:
- [Baseline: LIKE + Page](search_notes_pg_like_page.md)
- [Improved: LIKE + Slice](search_notes_pg_like_slice.md)

---

## 왜 Slice를 비교했나

- baseline 측정에서 content query는 매우 빨랐지만, `Page` 응답을 위한 count query가 상대적으로 훨씬 큰 비용을 보였다.
- 즉, 현재 구조의 병목 후보는 검색 방식 자체보다 `Page`가 강제하는 `COUNT(*)`에 더 가까웠다.
- 그래서 이번 비교는 검색 방식을 바꾸지 않고, 응답 구조를 `Page -> Slice`로 바꿨을 때의 차이를 확인하는 데 초점을 맞췄다.

---

## 실행 구조 차이

### Page

- content query + count query
- `totalElements`, `totalPages`, `last` 계산 필요

### Slice

- content query only
- `hasNext`만 판단

Repository 테스트 기준으로:
- `Page` 검색은 2개 쿼리(content + count)
- `Slice` 검색은 1개 쿼리(content only)

근거:
- [NoteRepositoryTest.java](../../../src/test/java/com/keepgoing/keepgoing/note/repository/NoteRepositoryTest.java)

---

## EXPLAIN 요약

### Baseline(Page)

- content query: `0.108 ms`
- count query: `19.749 ms`
- count query가 content query보다 약 `183배` 더 비쌌다.

### Slice

- content query: `0.183 ms`
- `hasNext` 판단을 위해 `LIMIT 11`로 1건 더 읽기 때문에 content query는 소폭 증가
- 그러나 count query가 제거되므로 전체 요청 경로는 더 단순해진다.

### 해석

- baseline에서는 검색 본문보다 `Page` 응답을 위한 count query가 더 큰 비용을 차지했다.
- Slice는 content query 비용이 약간 늘어도, count query 제거 효과가 훨씬 크게 나타날 수 있는 구조였다.

---

## k6 결과 (phase=measure 기준)

| 항목 | LIKE + Page | LIKE + Slice |
|---:|---:|---:|
| avg | 45.08 ms | 14.53 ms |
| p95 | 68.70 ms | 17.58 ms |
| req/s | 7.25 | 7.50 |
| fail | 0.00% | 0.00% |

### 개선 폭

- avg: `45.08 ms -> 14.53 ms`로 약 **3.1배 개선**
- p95: `68.70 ms -> 17.58 ms`로 약 **3.9배 개선**
- req/s: `7.25 -> 7.50`으로 소폭 증가
- fail: 두 경우 모두 `0.00%`

---

## 결과 캡처

<p align="center">
  <img src="images/k6_like_page.png" width="720" alt="k6 결과 (PostgreSQL, LIKE + pg_trgm + Page)">
</p>

<p align="center">
  <img src="images/k6_like_slice.png" width="720" alt="k6 결과 (PostgreSQL, LIKE + pg_trgm + Slice)">
</p>

---

## 결과 해석

- 현재 PostgreSQL notes 검색에서는 `LIKE + pg_trgm` 자체가 이미 충분히 빠른 편이다.
- 그러나 `Page` 응답은 전체 개수 계산을 위해 count query를 추가로 수행하기 때문에, 실제 API 응답시간에 더 큰 영향을 주고 있었다.
- `Slice`는 `hasNext` 판단을 위해 1건을 더 읽지만, count query를 제거할 수 있어 전체 응답시간을 더 크게 줄였다.
- 실제 측정에서도 avg는 `45.08 ms -> 14.53 ms`로 약 `3.1배`, p95는 `68.70 ms -> 17.58 ms`로 약 `3.9배` 개선됐다.
- 따라서 현재 조건에서는 검색 방식 변경보다 `Page -> Slice` 전환이 더 우선순위가 높은 개선안으로 보인다.

### 한 줄 정리

- 현재 PostgreSQL notes 검색에서는 검색 품질 개선보다 먼저, `Page`의 count query 제거(`Slice 전환`)가 더 큰 성능 개선 효과를 만들었다.

---

## 다음 단계

1. `/api/notes/search` 공개 검색에도 동일한 `Slice` 전략을 적용할지 검토
2. keyword 분포(희귀/없는 키워드) 변경 실험
3. 필요 시 PostgreSQL Full Text Search를 비교군으로 추가

---

## 참고(측정 스코프)

- 로컬(single-machine) 환경에서의 측정 결과이며, 배포 환경/네트워크 조건에 따라 절대값은 달라질 수 있다.
- 다만 동일 조건에서의 전/후 비교(개선 효과 확인)에는 충분히 유효하다.
