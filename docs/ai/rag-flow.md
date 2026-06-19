# AI RAG MVP 흐름

## 목적

AI 패널은 사용자의 메시지에 답변할 때 단순히 선택된 note 하나만 참고하지 않고,
사용자의 메시지와 관련 있는 indexed note를 검색해 함께 참고한다.

이 문서의 목적은 KeepGoing의 AI RAG MVP가 다음을 어떻게 보장하는지 설명하는 것이다.

- 사용자가 접근 가능한 note만 retrieval 대상에 포함한다.
- AI 수집이 허용된 note만 retrieval 대상에 포함한다.
- 인덱싱이 완료된 note chunk만 검색한다.
- AI 응답에 참고한 note 정보를 citation으로 함께 반환한다.
- 근거가 부족할 때 모델이 과장된 답변을 하지 않도록 prompt 규칙을 둔다.

---

## 전체 흐름

```text
Note 생성/수정/삭제/이름 변경
        ↓
AiNoteIndexingRequestService
        ↓
AiNoteIndexRequestedEvent 발행
        ↓
@TransactionalEventListener(AFTER_COMMIT)
@Async("aiIndexingExecutor")
        ↓
AiNoteIndexingService
        ↓
AiNoteIndexingProcessor
        ↓
ai_note_indexes / ai_note_chunks 저장
        ↓
사용자 AI 패널 메시지 요청
        ↓
AiPanelService
        ↓
AiNoteRetrievalService
        ↓
AiNoteChunkRepository.searchRelevantChunks(...)
        ↓
Prompt assembly
        ↓
ChatClient 호출
        ↓
assistantMessage + citations[] 응답
```
---


## 1. 인덱싱 요청

note가 생성, 수정, 삭제, 이름 변경되면 AiNoteIndexingRequestService가 재색인을 요청한다.

AiNoteIndexingRequestService.requestReindex(noteId, authorId)

역할:

- ai_note_indexes에 note 단위 indexing 상태를 생성하거나 갱신한다.
- 상태를 PENDING으로 변경한다.
- AiNoteIndexRequestedEvent를 발행한다.

---

## 2. 비동기 인덱싱 처리

인덱싱 이벤트는 트랜잭션 commit 이후 비동기로 처리된다.

@TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
@Async("aiIndexingExecutor")


처리 흐름:

```text
AiNoteIndexingService
        ↓
AiNoteIndexingProcessor
```
AiNoteIndexingService는 wrapper 역할을 한다.

- processor 호출
- 예외 발생 시 실패 상태 기록
- 실패 로그 기록
- 예외 재전파

AiNoteIndexingProcessor는 실제 인덱싱 트랜잭션을 담당한다.

- ai_note_indexes row를 pessimistic lock으로 조회한다.
- source note를 조회한다.
- 삭제된 note 또는 aiCollectable=false note면 chunk를 제거하고 REMOVED로 마킹한다.
- 수집 가능한 note면 chunk를 재생성하고 COMPLETED로 마킹한다.

---

## 3. 실패 상태 기록

인덱싱 중 예외가 발생하면 실패 상태는 별도 트랜잭션으로 기록한다.

AiNoteIndexFailureRecorder.markFailed(...)

이 recorder는 REQUIRES_NEW 트랜잭션으로 동작한다.

목적:

- 인덱싱 트랜잭션이 rollback되어도 FAILED 상태가 남도록 한다.
- last_error, last_processed_at, attempt_count를 기록한다.

---

## 4. 저장 구조

### ai_note_indexes

note 단위의 인덱싱 상태를 관리한다.

주요 컬럼:

  | 컬럼 | 설명 |
  | --- | --- |
  | note_id | 인덱싱 대상 note id |
  | author_id | note 작성자 id |
  | status | PENDING, COMPLETED, FAILED, REMOVED |
  | attempt_count | 처리 시도 횟수 |
  | last_requested_at | 마지막 인덱싱 요청 시각 |
  | last_processed_at | 마지막 처리 시각 |
  | indexed_at | 인덱싱 완료 시각 |
  | last_error | 실패 메시지 |

### ai_note_chunks

retrieval에 사용할 note chunk를 저장한다.

주요 컬럼:

  | 컬럼 | 설명 |
  | --- | --- |
  | id | chunk id |
  | note_id | 원본 note id |
  | author_id | note 작성자 id |
  | chunk_order | note 내 chunk 순서 |
  | title | note 제목 |
  | content_chunk | 검색 및 prompt에 사용할 본문 일부 |
  | source_updated_at | 원본 note 수정 시각 |
  | indexed_at | chunk 인덱싱 시각 |

---

## 5. Retrieval 조건

AI 패널 메시지가 들어오면 AiNoteRetrievalService가 사용자 메시지를 기준으로 관련 note chunk를 조회한다.

AiNoteRetrievalService.retrieve(userId, message, contextNoteId)

실제 DB 조회는 AiNoteChunkRepository.searchRelevantChunks(...)가 담당한다.


retrieval 대상 조건:

```text
c.author_id = userId
i.author_id = userId
n.author_id = userId
i.status = COMPLETED
n.deleted_at IS NULL
n.ai_collectable = true
contextNoteId는 제외
title 또는 content_chunk가 keyword와 match
```
이 조건을 통해 다음을 보장한다.

- 다른 사용자의 note는 retrieval되지 않는다.
- 삭제된 note는 retrieval되지 않는다.
- AI 수집이 허용되지 않은 note는 retrieval되지 않는다.
- 인덱싱이 완료되지 않은 note는 retrieval되지 않는다.
- 사용자가 직접 선택한 context note는 retrieved note와 중복되지 않는다.

---

## 6. 중복 제거

DB query는 chunk 단위로 후보를 반환한다.
같은 note의 여러 chunk가 검색될 수 있으므로 service 레벨에서 noteId 기준으로 중복 제거한다.

```text
AiNoteRetrievalService
  - candidate chunk 최대 20개 조회
  - noteId 기준 중복 제거
  - 최종 retrieved note 최대 5개 반환
```

중복 제거는 조회 순서를 유지한다.
즉, query 정렬상 더 관련 있다고 판단된 chunk가 해당 note의 대표 excerpt가 된다.

---

## 7. Prompt assembly

AiPanelService는 다음 정보를 조합해 user prompt를 만든다.

1. 현재 선택된 note context
2. retrieval된 관련 note
3. 답변 규칙
4. 사용자 메시지


prompt 구조:
```text
[현재 노트 문맥]
- noteId: ...
- title: ...
- content:
...

[검색된 관련 노트]
1. noteId: ...
   title: ...
   excerpt:
   ...

[답변 규칙]
- 제공된 현재 노트 문맥과 검색된 관련 노트 문맥만 근거로 답변한다.
- 근거가 부족하면 추측하지 말고 "노트에서 확인되지 않습니다"라고 말한다.
- 실제 저장/수정/삭제가 완료된 것처럼 말하지 않는다.
- 참고한 노트가 있다면 답변 내용이 해당 노트 문맥과 연결되도록 답한다.

[사용자 메시지]
...
```
---

## 8. Citation 응답

AI 패널 응답은 assistant message와 함께 citation 정보를 반환한다.

응답 예시:
```json
{
  "assistantMessage": "배포 일정은 금요일로 보입니다.",
  "contextNoteId": 10,
  "contextAttached": true,
  "citations": [
    {
      "noteId": 10,
      "title": "현재 회의록",
      "excerpt": "오늘 논의한 내용...",
      "sourceType": "CONTEXT_NOTE"
    },
    {
      "noteId": 20,
      "title": "배포 회의",
      "excerpt": "금요일 배포 결정",
      "sourceType": "RETRIEVED_NOTE"
    }
  ]
}
```
citation 필드:

  | 필드 | 설명 |
  | --- | --- |
  | noteId | 참고한 note id |
  | title | 참고한 note 제목 |
  | excerpt | 참고한 note 본문 일부 |
  | sourceType | CONTEXT_NOTE 또는 RETRIEVED_NOTE |

---

  ## 9. Source type

  ### CONTEXT_NOTE

  사용자가 명시적으로 선택한 현재 note 문맥이다.

  특징:

  - contextNoteId로 전달된다.
  - 기존 NoteService.getNote(...) 권한 검사를 통과해야 한다.
  - citation 목록에서 retrieved note보다 먼저 반환된다.

  ### RETRIEVED_NOTE

  사용자 메시지를 기준으로 검색된 관련 note다.

  특징:

  - indexed chunk 기반으로 검색된다.
  - 작성자, 삭제 여부, AI 수집 허용 여부, 인덱싱 완료 상태를 모두 만족해야 한다.
  - context note와 중복되지 않는다.

---

  ## 10. 검증 범위

  현재 테스트는 다음 흐름을 검증한다.

  ### 인덱싱

  - AiNoteIndexingServiceTest
      - processor 성공 시 실패 상태를 기록하지 않음
      - processor 실패 시 실패 상태 기록 후 예외 재전파
  - AiNoteIndexingProcessorTest
      - 수집 가능한 note는 chunk 저장 후 COMPLETED
      - 삭제된 note는 chunk 제거 후 REMOVED
      - aiCollectable=false note는 chunk 제거 후 REMOVED
      - index가 없으면 예외 발생
      - processor는 실패 상태를 직접 마킹하지 않고 예외 전파
  - AiNoteIndexFailureRecorderTest
      - 실패 발생 시 FAILED, last_error, attempt_count 기록

  ### Retrieval

  - AiNoteChunkRepositoryTest
      - 내 note + COMPLETED + aiCollectable=true + not deleted note 검색
      - 다른 사용자 note 제외
      - aiCollectable=false note 제외
      - COMPLETED가 아닌 index 제외
      - 삭제된 note 제외
      - excludeNoteId 제외
      - title match가 content match보다 우선 정렬
  - AiNoteRetrievalServiceTest
      - blank message면 검색하지 않음
      - 후보 chunk를 noteId 기준으로 중복 제거
      - 최종 결과 최대 5개 제한
      - 긴 excerpt truncate

  ### AI Panel

  - AiPanelServiceTest
      - context note prompt 포함
      - retrieved note prompt 포함
      - 답변 규칙 prompt 포함
      - context/retrieved citation 생성
      - 접근 불가능한 context note면 모델 호출 전 예외
  - AiPanelControllerTest
      - citation 응답 JSON 형식 검증
      - validation 오류 검증
      - AI 예외 응답 검증
  - AiPanelIntegrationTest
      - context note citation 응답 검증
      - retrieved note prompt/citation 반영 검증
      - 다른 사용자 note가 retrieval prompt/citation에 포함되지 않음

---

  ## 11. MVP 한계

  현재 retrieval은 MVP 범위로 keyword LIKE 검색 기반이다.

  현재 방식의 한계:

  - embedding/vector search가 아니다.
  - 의미 기반 검색 품질은 제한적이다.
  - 한국어 형태소 분석을 수행하지 않는다.
  - LOWER(...) LIKE '%keyword%' 형태라 데이터가 커지면 성능 한계가 생길 수 있다.
  - 같은 note의 여러 chunk 중 대표 chunk 선택은 query 정렬과 service 중복 제거에 의존한다.

  향후 개선 방향:

  - PostgreSQL pg_trgm 기반 유사도 검색
  - PostgreSQL full-text search
  - embedding 생성 및 vector search
  - 사용자 메시지 keyword extraction
  - retrieved context score 반환
  - citation과 실제 모델 답변의 source alignment 강화
