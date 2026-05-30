# SnapMind 팀원 A 인수인계 — Phase 1·2 완료

> 다음 세션의 팀원 A agent가 이어서 작업할 때 참고할 인수인계 문서.
> Phase 1 커밋: `f5350a6 Phase1: DB설계`
> Phase 2 작업: 본 문서 갱신 시점에서 stage 상태 (commit 대기)

---

## 작업 범위 (완료)

`docs/todos/team_todo.md`의 **Phase 1 — 팀원 A (DB 설계)** 3개 항목:

- [x] `#11` Room DB 스키마 설계 (MemoryItem, Tag, N:M 매핑)
- [x] `#12` DAO 구현 (insert / update / delete / query)
- [x] `#14` Room FTS 검색 인덱스 설정

근거 명세:
- `docs/specs/room-schema.md` — 테이블/컬럼/인덱스 명세 (이 문서를 100% 준수했음)
- `docs/architecture/database-design.md` — 엔티티 관계·FTS 설계 의도

---

## 주요 결정 사항

### 1. 패키지 구조: `data/local/`
Room 관련 코드는 모두 `app/src/main/java/com/example/snapmind/data/local/` 하위에 배치:
- `entity/` — Room 엔티티 (9개)
- `dao/` — DAO 인터페이스 (9개)
- `converter/` — Room TypeConverters
- `SnapMindDatabase.kt` — RoomDatabase

### 2. 기존 `InMemoryMemoryRepository` 보존
팀원 B의 UI가 `data/repository/InMemoryMemoryRepository`에 직접 의존 중이라 **건드리지 않았다**. Room 기반 Repository로의 교체는 **Phase 2 작업**(`#3` 이미지 저장 / `#13` 태그 N:M 매핑 로직)에서 진행해야 한다. 그때 `MemoryRepository` 인터페이스를 유지하면서 구현체만 갈아끼우면 팀원 B UI에 영향이 없다.

> ⚠️ 주의: `data/repository/`, `data/model/MemoryModels.kt`, 그리고 `feature/**`, `ui/**`는 팀원 B 영역. CLAUDE.md 규칙상 함부로 수정 금지.

### 3. Enum 타입 매핑
`room-schema.md`의 "Shared Enum Values"를 그대로 Kotlin enum으로 옮겼다 (`entity/ProcessingStatusEnums.kt`):
- `StandardProcessingStatus`: PENDING/RUNNING/SUCCESS/FAILED
- `OptionalRemoteProcessingStatus`: 위 + SKIPPED
- `GeminiMemoStatus`: PENDING/RUNNING/SUGGESTED/ACCEPTED/DISMISSED/FAILED/SKIPPED
- `TagAssignmentSource`: OCR/TFLITE/VISION/USER/SYSTEM (현재 미사용 — 추후 cross ref의 `sourceTypes` 직렬화에 사용)
- `TagAssignedBy`: AUTO/USER

`StatusConverters.kt`에서 String ↔ Enum 변환 등록. `MemoryTagCrossRef.sourceTypes`는 CSV String 그대로 저장 (예: `"OCR,TFLITE"`).

### 4. FTS4 가상 테이블
- `MemorySearchFts` (`@Fts4(notIndexed = ["memoryId"])`)
- `MemorySearchDao.search(query)` — `memory_search_fts MATCH :query` 조인으로 active memory 반환
- ⚠️ **FTS row 갱신은 OCR/메모/태그/분류/유튜브 메타 변경 시 동일 트랜잭션에서 수행해야 함**. 이건 Phase 2~5의 Repository 구현 시 책임 (도메인 로직 영역).

### 5. Database 설정
- `SnapMindDatabase.VERSION = 1`
- `exportSchema = true` (kapt `room.schemaLocation` 인자로 `app/schemas/` 경로 지정 — `app/build.gradle.kts` 참조)
- `app/schemas/com.example.snapmind.data.local.SnapMindDatabase/1.json` 커밋됨 (마이그레이션 검증용 — Phase 6 `#41` 마이그레이션 관리에서 사용)

### 6. Hilt 통합
`di/DatabaseModule.kt` — `@Provides`로 DB + 9개 DAO 모두 노출. `RepositoryModule.kt`는 아직 InMemoryMemoryRepository를 바인딩 중이라 그대로 둠.

---

## 추가/수정한 파일 (총 26)

### 수정 (3)
- `app/build.gradle.kts` — Room 의존성 + kapt schemaLocation
- `gradle/libs.versions.toml` — Room 2.6.1 버전/라이브러리 4개
- `docs/todos/team_todo.md` — Phase 1 팀원 A 체크 + 진행 표

### 신규 (23) — 모두 `app/src/main/java/com/example/snapmind/` 하위
**entity/** (10): MemoryItemEntity, OcrTextEntity, ClassificationEntity, VisionLabelEntity, TagEntity, MemoryTagCrossRef, MemoEntity, YoutubeLinkEntity, MemorySearchFts, ProcessingStatusEnums

**dao/** (9): MemoryItemDao, OcrTextDao, ClassificationDao, VisionLabelDao, TagDao, MemoryTagDao, MemoDao, YoutubeLinkDao, MemorySearchDao

**기타** (3): `data/local/SnapMindDatabase.kt`, `data/local/converter/StatusConverters.kt`, `di/DatabaseModule.kt`

**자동 생성** (1): `app/schemas/com.example.snapmind.data.local.SnapMindDatabase/1.json`

---

## DAO 인터페이스 요약 (다음 Phase에서 호출할 API)

| DAO | 주요 메서드 |
|---|---|
| `MemoryItemDao` | `insert/update/delete/getById/observeById/observeActive/observeFavorites/observeTrashed/setFavorite/setDeletedAt/set*Status` |
| `OcrTextDao` | `upsert/getByMemoryId/deleteByMemoryId` |
| `ClassificationDao` | `insertAll/getByMemoryId/getTopByMemoryId/categoryCounts` (drawer용) |
| `VisionLabelDao` | `insertAll/getByMemoryId/deleteByMemoryId` |
| `TagDao` | `insert/update/findByName/findById/observeActive/setArchived/popularTags(limit)/allTagCounts` |
| `MemoryTagDao` | `upsert/upsertAll/activeTagsForMemory/setRemovedAt/memoriesForTag(tagId)` |
| `MemoDao` | `upsert/getByMemoryId/observeByMemoryId/updateBody/updateGeminiSuggestion` |
| `YoutubeLinkDao` | `upsert/getByMemoryId/deleteByMemoryId` |
| `MemorySearchDao` | `upsertIndex/deleteIndex/search(query)` |

Drawer 쿼리 2개는 `room-schema.md`의 SQL을 그대로 옮긴 것 (`categoryCounts`, `popularTags`).

---

---

# Phase 2 — 이미지 수집 (저장 로직) 완료

## 작업 범위 (완료)

`docs/todos/team_todo.md`의 **Phase 2 — 팀원 A (저장 로직)** 2개 항목:

- [x] `#3` 수신 이미지 앱 내부 저장소 복사 및 URI 관리
- [x] `#13` 태그 저장 및 N:M 매핑 로직 구현

근거 명세:
- `docs/specs/permission-storage-spec.md` — 저장 경로 / MIME 검증 / 파일명 규칙
- `docs/features/auto-tagging.md` — `sourceTypes` CSV / `removedAt` 보존 정책
- `docs/specs/ml-training-spec.md` — `MemoryCategory` 라벨 정렬 기준 (9개)

## 주요 결정 사항

### 1. 사용자 승인 하의 팀원 B 영역 예외 수정

ML 스펙(9개 카테고리: chat/receipt/code/shopping/travel/food/document/youtube/unknown)과 팀원 B의 `MemoryCategory` enum(MAP 포함 8개)이 어긋남. 사용자 명시 승인 하에 **ML 스펙 기준으로 B의 enum 정렬**:

- `data/model/MemoryModels.kt` — `MAP` 제거, `TRAVEL`/`FOOD` 추가
- `feature/home/MemoryGridAdapter.kt`, `feature/memorydetail/DetailActivity.kt` — when 분기 갱신 (TRAVEL/FOOD는 기존 `bg_thumbnail_receipt`로 fallback, Phase 4에서 B가 전용 drawable 추가 가능)
- `res/drawable/bg_thumbnail_map.xml` — **미사용 상태로 보존 (삭제 금지)**

> ⚠️ 이 예외는 docs/specs/*와 어긋날 때만 사용자 승인 받아 적용. 자의적으로 B 코드 수정 금지.

### 2. 다중 status → 단일 `ProcessingStatus` 매핑 규칙

`MemoryItemEntity`의 6개 status 컬럼 → 도메인 `ProcessingStatus` 합성 (`EntityMappers.kt:composeProcessingStatus`):

- **하나라도 FAILED → ERROR**
- 모두 "완료 상태"(SUCCESS / SKIPPED / Gemini의 SUGGESTED·ACCEPTED·DISMISSED) → **DONE**
- 그 외 (PENDING / RUNNING 포함) → **PROCESSING**

### 3. 분류 라벨 → `MemoryCategory` 매핑

`docs/specs/ml-training-spec.md`가 TFLite 출력을 `MemoryCategory.name`의 lowercase로 정렬하기로 합의되었기에 **매핑 테이블 없이 safe `valueOf` 호출만** (`EntityMappers.kt:toMemoryCategory`):
```kotlin
runCatching { MemoryCategory.valueOf(label.uppercase()) }.getOrDefault(UNKNOWN)
```
대소문자 정규화 + 미지/null 라벨 → UNKNOWN fallback.

### 4. 시드 데이터 정책

`InMemoryMemoryRepository.seedMemories()` 본문은 **주석 처리만 (코드 보존, CLAUDE.md 규칙 준수)** + `return emptyList()`. 빈 DB로 시작 → 이미지 import 시점부터 점진적으로 채워짐. 이로 인해 첫 실행 시 갤러리/즐겨찾기/태그 Drawer는 모두 빈 화면. 정상 동작.

### 5. ImageImporter 설계 (`#3`)

`core/image/ImageImporter.kt` — Repository와 독립된 standalone 서비스.

- 저장 경로: `context.filesDir/snapmind/images/` (`permission-storage-spec.md` 준수)
- 파일명: `memory_{timestamp}_{8자_UUID}.{ext}` (UUID로 랜덤 suffix → 충돌 방지)
- MIME 검증: `image/jpeg`, `image/png`, `image/webp`, `image/heic`, `image/heif` 허용리스트. 외 모두 `UnsupportedImageType` 반환
- **SHA-256 contentHash 계산**: 복사 스트림과 동시에 digest 누적 (single-pass) → 중복 import 감지용
- 빈 파일/스트림 실패 시 부분 저장된 파일 cleanup

### 6. TagAssigner 설계 (`#13`)

`data/repository/TagAssigner.kt` — Tag/CrossRef upsert 책임 분리.

- **정규화**: `#` 제거 + trim + lowercase + 공백 → `_`. `displayName`은 사용자 원본 유지
- **`TagDao.insert(IGNORE)` 기반 upsert**: race 시 `findByName` 폴백
- **`sourceTypes` CSV merge**: 기존 cross ref와 동일 태그가 다른 source로 다시 들어오면 sources 합집합 후 갱신 (예: `"OCR" + "VISION" → "OCR,VISION"`)
- **`removedAt` 보존 정책**: AUTO 재할당 시 사용자가 명시적으로 제거(`removedAt` non-null)한 태그는 **재추가하지 않음** (auto-tagging.md 명세)
- **USER 우선**: USER 할당이 들어오면 `removedAt` 클리어 + assignedBy를 USER로 승격
- `assigner.assign(memoryId, request, now)` 호출 시 `now` 인자를 외부에서 받음 → 트랜잭션 일관성 확보 가능

### 7. Repository 교체 (`RoomMemoryRepository`)

`data/repository/RoomMemoryRepository.kt` — `MemoryRepository` 인터페이스 그대로 구현 (팀원 B UI 영향 0).

- 내부 `CoroutineScope(SupervisorJob + io)` 보유 — non-suspend 메서드를 fire-and-forget으로 실행
- `MemoryItemDao.observeActive()` + `observeTrashed()` combine → 각 entity별 N+1 쿼리로 `MemoryAggregate` 빌드 → 도메인 매핑 → 캐시 갱신 (`@Volatile snapshot`)
- 동기 스냅샷 메서드(`getMemory`, `activeMemories`, `topTags` 등)는 캐시 반환
- `searchMemories`/`filterByTag`/`filterByCategory`는 **여전히 in-memory 필터링** (인터페이스가 non-suspend이므로). Phase 4 `#19`/`#20`에서 `MemorySearchDao.search()` 기반 Flow 쿼리로 교체 예정
- `RepositoryModule` 바인딩 `InMemoryMemoryRepository` → `RoomMemoryRepository` 전환

### 8. FTS row 갱신 트랜잭션 규율

`RoomMemoryRepository.refreshFts(memoryId)` private 헬퍼 — 항상 현재 DB 상태에서 `MemoryAggregate`를 재빌드하여 FTS row를 upsert. 호출 시점:
- `importImage` 직후 (초기 빈 값)
- `updateMemo` 직후 (memoBody 변경)
- `acceptGeminiSuggestion` 직후 (memo body 변경)

Phase 3에서 OCR/분류/태그/유튜브 메타 변경마다 동일하게 `refreshFts` 호출 필요.

### 9. 중복 import 처리

`importImage` 진입 시 `imageImporter`로 파일 복사 + 해시 계산 → 기존 `contentHash` 일치하는 `memory_items`이 있으면 **새 row 만들지 않고 기존 도메인 객체 반환**. 단, 이 경우 새로 복사된 파일은 디스크에 남음 (중복 파일은 cleanup 미구현 — Phase 6 안정성에서 처리 가능).

## 추가/수정한 파일 (총 11)

### 수정 (7)
- `app/src/main/java/com/example/snapmind/data/model/MemoryModels.kt` — `MemoryCategory` 9개 재정의
- `app/src/main/java/com/example/snapmind/feature/home/MemoryGridAdapter.kt` — when 분기
- `app/src/main/java/com/example/snapmind/feature/memorydetail/DetailActivity.kt` — when 분기
- `app/src/main/java/com/example/snapmind/data/repository/InMemoryMemoryRepository.kt` — `seedMemories()` 본문 주석화
- `app/src/main/java/com/example/snapmind/di/RepositoryModule.kt` — 바인딩 전환
- `docs/todos/team_todo.md` — Phase 2 팀원 A 체크 + 진행 표
- `docs/progress.md` — 본 섹션 추가

### 신규 (4)
- `app/src/main/java/com/example/snapmind/core/image/ImageImporter.kt` — #3 핵심
- `app/src/main/java/com/example/snapmind/data/repository/TagAssigner.kt` — #13 핵심
- `app/src/main/java/com/example/snapmind/data/repository/EntityMappers.kt` — Room↔도메인 매핑 + status/category 합성
- `app/src/main/java/com/example/snapmind/data/repository/RoomMemoryRepository.kt` — MemoryRepository 신규 구현

## 다음 Phase에서 해야 할 일 (팀원 A)

### Phase 3 (AI 파이프라인)
- `#5` TFLite 학습 (Colab): 라벨 출력은 `MemoryCategory.name`의 lowercase 9개로 통일. ML 스펙(`docs/specs/ml-training-spec.md`)을 추가로 정렬 필요 (현재 `travel`/`food` 있으나 `map` 잔존 → spec 수정)
- `#4` ML Kit OCR → `OcrTextDao.upsert` + `memoryItemDao.setOcrStatus(SUCCESS)` + `refreshFts(memoryId)`
- `#6` TFLite 추론 → `ClassificationDao.insertAll` + `setClassificationStatus(SUCCESS)` + `refreshFts`
- `#9` 자동 태그 룰 엔진: OCR + Classification + Vision 결과를 받아 `TagAssigner.assignAll(memoryId, requests, now)`. requests의 `sources`/`assignedBy`/정규화는 모두 `TagAssigner`가 처리. `removedAt` 보존 정책 자동 적용됨

### Phase 4 (검색 백엔드)
- `#19`/`#20`: `MemorySearchDao.search()`를 Flow로 감싸 ViewModel에서 사용. 현재 `RoomMemoryRepository.searchMemories`는 in-memory 필터링이므로 **인터페이스를 suspend/Flow로 바꾸거나** ViewModel에서 직접 DAO 호출하도록 우회 필요 (인터페이스 변경 시 팀원 B 의존 8개 파일 영향 검토 필수)
- `#32` 휴지통 복구: `restore(memoryId)`는 이미 동작. 영구 삭제 시 `imageUri` 파일 정리 추가 필요

### Phase 5 (상세 화면 데이터 로직)
- `#25`/`#26`/`#28`: 메모 작성은 `updateMemo` → 내부에서 FTS refresh 처리. OCR/카테고리/태그 표시는 도메인 매핑이 이미 반영하므로 ViewModel만 작성하면 됨

### Phase 6 (안정성 · 설정)
- `#37` Coroutine 예외 처리: `RoomMemoryRepository`의 내부 `scope`에 `CoroutineExceptionHandler` 부착
- `#38` OOM 방지: `ImageImporter`에 이미지 디코드 단계 추가 시 `BitmapFactory.Options.inSampleSize` 적용
- `#39` URI 권한 만료: import 시점에 즉시 복사하므로 만료 영향 없음. 외부 URI 누출 검증만 필요
- 중복 import로 인한 디스크 누수 cleanup
- FTS index 손상 복구 (rebuild 명령)