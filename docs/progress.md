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

---

# Phase 3 — AI 파이프라인 (ML · OCR) 완료

## 작업 범위

`docs/todos/team_todo.md`의 **Phase 3 — 팀원 A (ML · OCR)** 4개 중 3개 완료, #5는 외부 진행 중:

- [/] `#5` TFLite CNN 모델 학습 (Colab — 외부 진행) — 스펙(9개 라벨) 확정, asset 도착 대기
- [x] `#4` ML Kit OCR 비동기 텍스트 추출
- [x] `#6` TFLite 앱 탑재 + 추론 결과 DB 업데이트 (스텁 구조)
- [x] `#9` 자동 태그 생성 룰 엔진

근거 명세:
- `docs/architecture/ai-pipeline.md` — 전체 파이프라인 흐름
- `docs/architecture/background-processing.md` — WorkManager 기반 오케스트레이션
- `docs/features/ocr-processing.md` — OCR 처리 명세
- `docs/features/ai-image-classification.md` — TFLite 분류 명세
- `docs/features/auto-tagging.md` — 자동 태그 룰

## 주요 결정 사항

### 1. 오케스트레이션: WorkManager 풀 인프라

`docs/architecture/background-processing.md`의 권장안 채택.

- `Application : Configuration.Provider` + `HiltWorkerFactory` 주입 → 커스텀 Worker DI 가능
- AndroidManifest에 `androidx.startup` 기반 WorkManager 자동 초기화 비활성 (Configuration.Provider 사용)
- 워커 입력: `memoryId` (Long). 출력: Room의 status 컬럼 (Worker.Result는 보조 신호)
- 체인: `LocalMemoryProcessingWorker` → 끝나면 `AutoTaggingWorker` enqueue
- Remote enrichment(Vision/Gemini/YouTube)는 팀원 B의 Phase 3 항목. 현재는 `SKIPPED` 상태로 둠 → AutoTaggingWorker가 OCR + TFLite만 보고 태그 생성. Vision 결과가 추후 DB에 들어오면 동일 룰 엔진이 자동 활용.

### 2. TFLite 스텁 Classifier

`.tflite` 모델 학습(#5)이 외부 진행 중이라 asset이 없는 상태. **코드는 모델이 있다는 가정으로 완성**:

- 경로: `app/src/main/assets/image_classifier_v1_0_0.tflite` (ml-training-spec 규약)
- `ImageClassifier.obtainInterpreter()` — `context.assets.openFd(...)` 시도, `IOException` 발생 시 `ModelUnavailableException`으로 변환
- 워커는 이 예외를 잡아 `classificationStatus = FAILED`로 마킹
- **사용자가 모델 파일을 assets 폴더에 넣으면 다음 빌드부터 자동 동작 — 코드 수정 0**
- `build.gradle.kts`에 `androidResources.noCompress.add("tflite")` 추가 (asset 압축 방지)
- Interpreter는 lazy + 동기화된 단일 인스턴스 (재사용)
- 전처리: EXIF 회전 보정 → 224x224 리사이즈 → RGB float 정규화 (0~1)
- 후처리: top-3 + confidence threshold 0.65 미만이면 top-1을 `unknown`으로 마킹

### 3. ML Kit OCR

- `OcrExtractor` — `TextRecognition.getClient(LATIN)` 사용 (라틴 + 한글/영문/숫자 모두 인식. 한국어 한정 모델은 별도 의존성 필요하여 일단 LATIN으로 시작)
- ML Kit의 콜백 API를 `suspendCancellableCoroutine`으로 감싸 suspend 함수화
- 정규화: 라인별 trim + 빈 라인 압축 + 트레일링 공백 제거. `fullText`(정규화) + `rawText`(원본) 모두 저장 가능 (`rawText`는 정규화 결과와 다를 때만 저장)

### 4. 자동 태그 룰 엔진

`AutoTagRuleEngine.buildAssignments(ocrText, classifications, visionLabels)`:

- **TFLite 라벨**: confidence ≥ 0.5 + `unknown` 제외 → `TagAssignmentSource.TFLITE`
- **Vision API 라벨**: score ≥ 0.80 (auto-tagging.md 기준) → `TagAssignmentSource.VISION`
- **OCR 추출**:
  - URL 정규식 → host(`www.` 제거, port 제거) → 태그
  - 이메일 패턴 발견 시 `email` 태그
  - 한글(`가-힣`) 등장 시 `korean` 태그
- 같은 정규화 키로 중복되는 태그는 **소스만 합집합** (`linkedHashMap<key, Set<source>>`)
- 최대 20개 cap (auto-tagging.md)
- 출력은 `TagAssigner.assignAll`로 그대로 전달 — `removedAt` 보존/USER 우선 등 정책은 Phase 2 TagAssigner가 처리

### 5. 공용 헬퍼 추출

Worker와 Repository가 동일하게 aggregate 빌드 + FTS 갱신을 수행하므로 분리:
- `MemoryAggregateBuilder` (`@Singleton @Inject`) — DAO 6개 의존, `build(entity)` 메서드 노출
- `refreshFtsRow(memoryId, ...)` 자유 함수 — `MemoryItemDao + AggregateBuilder + MemorySearchDao` 받아 한 번에 처리
- `RoomMemoryRepository.refreshFts`/`buildAggregate`는 기존 private 그대로 유지 (점진 리팩터링 가능)

### 6. 파이프라인 트리거

`RoomMemoryRepository.importImage` 마지막에 `enqueueLocalProcessing(memoryId)` 호출. WorkManager Hilt 인젝션은 Worker 측 `@HiltWorker @AssistedInject`로 처리됨.

## 추가/수정한 파일 (총 11)

### 수정 (5)
- `gradle/libs.versions.toml` — ML Kit Text Recognition / TFLite + Support / WorkManager / Hilt-Work / ExifInterface (6개 라이브러리)
- `app/build.gradle.kts` — 위 라이브러리 implementation + `noCompress("tflite")`
- `app/src/main/AndroidManifest.xml` — WorkManagerInitializer 자동 초기화 비활성 provider 추가
- `app/src/main/java/com/example/snapmind/SnapMindApplication.kt` — `Configuration.Provider` 구현 + `HiltWorkerFactory` 주입
- `app/src/main/java/com/example/snapmind/data/repository/RoomMemoryRepository.kt` — `enqueueLocalProcessing` + `@ApplicationContext` 추가
- `docs/todos/team_todo.md`, `docs/progress.md` — 진행 상태/인수인계

### 신규 (6)
- `core/ai/OcrExtractor.kt` (#4) — ML Kit 래퍼
- `core/ai/ImageClassifier.kt` (#6) — TFLite 스텁 + 전/후처리
- `core/ai/AutoTagRuleEngine.kt` (#9) — 룰 엔진
- `data/work/LocalMemoryProcessingWorker.kt` — OCR + Classification + FTS refresh + AutoTaggingWorker enqueue
- `data/work/AutoTaggingWorker.kt` — 룰 엔진 호출 + TagAssigner 호출 + FTS refresh
- `data/repository/MemoryPersistenceHelpers.kt` — 공용 `MemoryAggregateBuilder` + `refreshFtsRow`

## 다음 Phase에서 해야 할 일 (팀원 A)

### Phase 3 잔여
- `#5` TFLite 모델 학습 (Colab):
  - 출력 라벨 9개: `chat`, `receipt`, `code`, `shopping`, `travel`, `food`, `document`, `youtube`, `unknown` (소문자, ml-training-spec.md)
  - 입력 224x224 RGB float [0,1]
  - 학습 완료 후 `.tflite` 파일을 `app/src/main/assets/image_classifier_v1_0_0.tflite` 경로에 배치 → 다음 빌드부터 자동 활성. 코드 수정 불필요
  - labels.txt(`image_classifier_labels_v1_0_0.txt`)도 동일 폴더 권장 (현재 코드는 사용 안 함, 향후 동적 라벨로 전환 가능)

### Phase 4 (검색 백엔드)
- `MemorySearchDao.search()`를 Flow로 감싸 ViewModel에서 사용. 현재 RoomMemoryRepository의 `searchMemories`는 in-memory 필터링이라 FTS 활용 안 됨. 인터페이스 suspend화 또는 ViewModel→DAO 직접 호출 검토 필요
- 휴지통 복구(#32) / PDF 추출(#33)

### Phase 5 / 6
- 상세 화면 데이터 로직(#25/#26/#28) — 도메인 매핑은 이미 반영됨, ViewModel만 필요
- Coroutine 예외 처리, OOM 방지, URI 권한 검증 — 기존 Phase 6 가이드 유지

### 알아둘 점
- 워커는 `Result.success()`를 반환하더라도 **DB의 status가 진짜 결과**. WorkManager Result는 보조 신호로만 사용
- 모델 부재 시 classificationStatus만 FAILED. OCR/Tagging은 정상 동작 → 부분 처리 결과 표시 가능
- 재시도는 Phase 4/5의 상세 화면에서 워커 재 enqueue로 구현 예정 (`#37` 안정성에서 다시 검토)

## Phase 3 마무리 (모델 도착 + 분류 정상화)

`.tflite` 모델 도착 후 발견·수정한 3가지:

1. **LiteRT 마이그레이션** — `org.tensorflow:tensorflow-lite:2.14/2.16`은 모두 Colab(TF 2.17+)에서 export한 `FULLY_CONNECTED v12` op 미지원. `com.google.ai.edge.litert:litert:1.0.1`로 전환 (TF 2.17 기준 빌드). 패키지명(`org.tensorflow.lite.*`)은 그대로 유지되어 import 변경 불필요. `tensorflow-lite-support`는 미사용이라 제거.
2. **labels.txt 동적 로딩** — 학습 스크립트(`snapmind_trainmodel.py`)가 `image_dataset_from_directory`를 쓰면서 폴더명 알파벳 순서로 클래스가 정렬됨(`chat, code, document, food, receipt, shopping, travel, unknown, youtube`). 코드의 하드코딩 순서와 달라서 카테고리/태그가 잘못 매핑되던 문제. `assets/labels.txt`를 인터프리터 로드 시 함께 읽어 학습 순서를 단일 진실 소스로 사용. 폴백은 알파벳 정렬된 9개 라벨.
3. **EfficientNet 입력 정규화 정렬** — 학습 코드 주석에 "EfficientNet은 내부에 정규화가 포함되어 있어 입력을 0~255 그대로 넣어야 한다"고 명시. 기존 `pixel / 255f`로 [0,1]로 줄이던 코드가 이중 정규화를 유발하던 문제. raw 0~255 float로 변경.

진단 보강: classify 실패 시 `Log.e("ImageClassifier", ...)`로 예외 출력 → Logcat에서 즉시 원인 파악 가능.

asset 폴더 구성:
- `app/src/main/assets/image_classifier_v1_0_0.tflite` — 모델 파일 (~16MB)
- `app/src/main/assets/labels.txt` — 클래스명 9줄 (학습 순서)
- `app/src/main/assets/snapmind_trainmodel.py` — Colab 학습 스크립트 (재학습 시 참고)

**재학습 시 주의:**
- `image_dataset_from_directory`의 알파벳 정렬 규칙 때문에 클래스 폴더명을 바꾸면 `class_names` 순서가 바뀜 → 반드시 새로 생성된 `labels.txt`도 함께 교체. 모델/라벨 한쪽만 교체하면 분류 결과 깨짐

---

# Phase 4 — 검색 백엔드 · 휴지통 · PDF 완료

## 작업 범위

`docs/todos/team_todo.md`의 **Phase 4 — 팀원 A** 4개 항목 모두 완료:

- [x] `#19` Room FTS 통합 검색 쿼리
- [x] `#20` 검색 결과 Flow 스트림 + ViewModel
- [x] `#32` 휴지통 복구 + 영구 삭제 로직
- [x] `#33` PDF 추출 기능

근거 명세:
- `docs/features/search-filter.md` — FTS + debounce + ViewModel
- `docs/features/memo-management.md` — 영구 삭제 시 cascade 규칙
- `docs/architecture/privacy-security.md` — 파일 cleanup
- `docs/specs/screen-specs.md` — 휴지통/PDF 화면 요구

## 주요 결정 사항

### 1. B Activity 3개 수정 (사용자 명시 승인 예외)

옵션 1(backend-only)과 옵션 2(UI까지 연결) 중 **옵션 2** 선택. SearchActivity / TrashActivity / PdfExportActivity 모두 수정. 신규 ViewModel/메서드 활용 + Phase 4 명세 100% 충족.

### 2. MemoryRepository 인터페이스 확장 (3개 메서드)

- `suspend fun searchFts(query: String): List<MemoryItem>` — FTS 기반 검색. 빈 query면 active 전체 반환
- `suspend fun permanentDelete(memoryId: Long): AppResult<Unit>` — FTS row 삭제 + memory_items DELETE(CASCADE로 OCR/메모/태그/분류/유튜브 모두 삭제) + 이미지 파일 cleanup
- `suspend fun exportToPdf(memoryIds: List<Long>): AppResult<Uri>` — 빈 리스트면 활성 전체 export

`InMemoryMemoryRepository`(여전히 dead code)에도 stub 추가하여 컴파일 유지.

### 3. FTS 쿼리 escape 전략

`RoomMemoryRepository.escapeFtsQuery`:
```kotlin
raw.split(\\s+).joinToString(" ") { "\"${it.replace("\"","")}\"*" }
```
- 토큰별로 큰따옴표로 감싸고 prefix wildcard `*` 부착
- 모든 토큰 AND (FTS4 기본 동작)
- 결과: "react bug" → `"react"* "bug"*` — react로 시작하는 단어 AND bug로 시작하는 단어

### 4. SearchViewModel 설계

- `_query: MutableStateFlow<String>` + `.debounce(250ms).distinctUntilChanged()`
- 4개 StateFlow combine: `repository.memories`(DB 변경 신호) + debouncedQuery + tagName + category
- `mapLatest`로 최신 입력만 처리 (오래된 검색 자동 취소)
- 결과는 `searchFts(query)`로 FTS 후보 추출 → in-memory에서 tag/category 필터
- `SearchUiState` 단일 객체로 노출 (results + tags + categories + 선택 상태)

### 5. 영구 삭제 + 파일 cleanup

- `memorySearchDao.deleteIndex(memoryId)` 먼저 (FTS는 FK CASCADE 안 됨, virtual table 한계)
- `memoryItemDao.deleteById(memoryId)` — FK CASCADE로 ocr/memo/classifications/vision_labels/youtube_links/memory_tag_cross_refs 자동 삭제. tags 자체는 보존(타 메모리에서 쓸 수 있음)
- 이미지 파일: `file://` URI에서 path 디코딩 → `context.filesDir` 하위인지 canonical path로 검증(traversal 방지) → 삭제
- 실패해도 DB는 이미 삭제된 상태 → Best-effort cleanup
- TrashActivity에서 개별(하트 버튼 길게 누름이 아니라 하트 탭 시 영구 삭제 확인 다이얼로그) + 전체("휴지통 비우기" 툴바 메뉴) 모두 지원

### 6. PDF 출력 전략

`PdfExporter` 서비스:
- Android 내장 `PdfDocument` API 사용 (외부 라이브러리 없음)
- A4 595x842pt, 메모리당 1페이지
- 페이지 레이아웃: 헤더(타임스탬프, 페이지 N/M) + 카테고리 + 태그 + 이미지(최대 300pt 높이, 비율 유지) + 메모 + OCR(최대 800자) + 유튜브 URL
- 이미지 디코드는 `inSampleSize=2 + RGB_565`로 OOM 회피
- 출력 위치: `context.cacheDir/exports/snapmind_export_{timestamp}.pdf`
- `FileProvider`(`${applicationId}.fileprovider`)로 content:// URI 변환 → `Intent.ACTION_SEND` 공유 가능
- `res/xml/file_paths.xml`에 `<cache-path name="exports" path="exports/" />` 선언

### 7. B Activity 수정 요지

- **SearchActivity**: `MemoryRepository` 직접 주입 제거 → `SearchViewModel by viewModels()`. 텍스트 입력은 `viewModel.onQueryChanged`로 위임 → ViewModel이 debounce 처리. `repeatOnLifecycle(STARTED)`로 결과 구독.
- **TrashActivity**: 툴바에 `menu_trash.xml`(휴지통 비우기) 추가. 하트 버튼 = 개별 영구삭제 확인 다이얼로그(복구는 카드 탭). 휴지통 비우기 = 전체 영구삭제 확인 → 루프 호출.
- **PdfExportActivity**: 버튼 → `exportToPdf(activeIds)` → `ACTION_SEND chooser`. 진행 중에는 버튼 비활성 + "PDF 생성 중…" 표시.

## 추가/수정한 파일 (총 12)

### 수정 (7)
- `data/local/dao/MemorySearchDao.kt` — `searchIds` + `observeSearch(Flow)` 추가
- `data/repository/MemoryRepository.kt` — 3개 메서드 추가
- `data/repository/InMemoryMemoryRepository.kt` — stub 추가
- `data/repository/RoomMemoryRepository.kt` — 신규 메서드 구현 + `PdfExporter` 주입 + `escapeFtsQuery`
- `feature/search/SearchActivity.kt` — ViewModel 기반 재작성 (B 예외)
- `feature/utility/TrashActivity.kt` — 영구 삭제 추가 (B 예외)
- `feature/utility/PdfExportActivity.kt` — 실제 PDF 생성 + 공유 (B 예외)
- `app/src/main/AndroidManifest.xml` — FileProvider 등록

### 신규 (4)
- `core/pdf/PdfExporter.kt` — PDF 생성 서비스
- `feature/search/SearchViewModel.kt` — debounce + Flow 기반 검색 VM
- `res/xml/file_paths.xml` — FileProvider 경로 정의
- `res/menu/menu_trash.xml` — 휴지통 비우기 메뉴

## 다음 Phase에서 해야 할 일 (팀원 A)

### Phase 5 (상세 화면 데이터 로직)
- `#25` DetailActivity 이미지 원본 + OCR 텍스트 표시 — 도메인 매핑은 이미 반영. ViewModel만 작성
- `#26` 분류 카테고리 + 태그 표시 — 동일
- `#28` 메모 작성/수정 + DB 연동 — `updateMemo` 이미 구현됨. ViewModel + 양방향 바인딩

### Phase 6 (안정성)
- `#37` Coroutine 예외 처리 — RoomMemoryRepository `scope`에 `CoroutineExceptionHandler`
- `#38` 대용량 이미지 OOM — `ImageImporter`/`PdfExporter` 디코드 시 inSampleSize 적용 (PdfExporter는 이미 적용됨)
- `#39` URI 권한 만료 — import 시점 즉시 복사하므로 영향 없음
- `#36` 설정 화면 — 설정 항목(예: 자동 OCR 활성화, 캐시 정리) 정의 필요

### 알아둘 점
- 영구 삭제는 비가역. UI 확인 다이얼로그 필수
- PDF 캐시 파일은 자동 정리 안 됨 (Android의 cache 자동 정리에 의존). Phase 6에서 명시적 cleanup 워커 추가 고려
- SearchActivity는 이제 ViewModel을 사용하므로 `memoryRepository.tags()` / `categoryCounts()` 호출 횟수가 줄어듬 (StateFlow 캐시 1회)
- FTS 검색은 한 번에 매칭 ID만 받아오고 나머지는 in-memory snapshot에서 lookup — 5,000개 메모리 가정에서도 즉답 가능
- TF 버전 차이로 op version이 또 올라가면 LiteRT 1.0.1 → 1.1.x/1.2.x로 한 줄만 올리면 됨

---

# Phase 5 — 상세 화면 데이터 로직 완료

## 작업 범위

`docs/todos/team_todo.md`의 **Phase 5 — 팀원 A** 3개 항목 모두 완료:

- [x] `#25` DetailActivity — 이미지 원본 + OCR 텍스트 표시
- [x] `#26` 분류 카테고리 + 태그 표시
- [x] `#28` 메모 작성 / 수정 로직 + DB 연동

근거 명세:
- `docs/features/memo-management.md` — 메모 ViewModel + StateFlow, config change 생존, Gemini 추천 흐름
- `docs/specs/screen-specs.md` — 상세 화면 요구

## 주요 결정 사항

### 1. B Activity 1개 수정 (사용자 명시 승인 예외)

`DetailActivity` 수정 허용. 명세 "Memo edits must survive configuration changes" + "ViewModel + StateFlow"를 충족하려면 ViewModel 도입이 필수. 사용자 승인 받아 진행.

### 2. DetailViewModel — SavedStateHandle 기반 상태 보존

`DetailViewModel`이 두 가지 키를 `SavedStateHandle`에 저장:
- `detail.memoryId` (Long) — 현재 보고 있는 메모리 ID. 프로세스 사망 후 복구 가능
- `detail.memoDraft` (String?) — 저장 안 된 메모 편집 내용. `null`이면 "저장된 메모와 동일", non-null이면 미저장 변경분

`bind(memoryId)`로 초기화 — Activity onCreate에서 호출. 동일 ID 재바인딩은 no-op (config change 시 draft 보존).

### 3. DetailUiState 단일 상태 노출

`combine(repository.memories, memoryIdFlow, memoDraftFlow)`로 합성:
- 메모리 없음/삭제됨 → `gone = true` (Activity가 finish())
- 메모리 존재 → `memoDraft = draft ?: memory.memo`, `hasUnsavedMemo = draft != null && draft != memory.memo`

`hasUnsavedMemo`가 `false`일 때 저장 버튼 비활성 → 저장 후 자동 비활성.

### 4. EditText ↔ ViewModel 양방향 안전 바인딩

`suppressMemoTextWatcher` 플래그로 피드백 루프 방지:
- ViewModel 상태 변경 → `syncMemoEditText(target)`: 현재 텍스트와 다를 때만 `setText` + 커서 끝으로
- 사용자 타이핑 → `doOnTextChanged` → `viewModel.onMemoDraftChanged(text)` (suppress 플래그로 ViewModel 트리거 시점은 무시)
- 결과: 회전/재구성에도 입력 위치 유지, 외부 메모리 변경(다른 화면에서 수정)도 즉시 반영

### 5. 액션 위임

모든 상호작용을 ViewModel 메서드로 위임:
- `saveMemo()` — draft가 있을 때만 동작, 저장 후 draft clear → 다음 emission에서 hasUnsavedMemo=false
- `toggleFavorite()`, `softDelete()`, `acceptGeminiSuggestion()`, `dismissGeminiSuggestion()`
- Gemini 수락 시 draft 자동 clear (메모 본문이 suggestion으로 덮어쓰임)

### 6. #25/#26 처리

이미 RoomMemoryRepository(Phase 2~3)가 OCR/분류/태그 데이터를 도메인 모델에 매핑해서 노출 중이라 별도 데이터 작업 없음. DetailActivity의 기존 표시 로직(`renderPreview`, `renderChips`, `renderSuggestion`, `renderYoutube`) 유지하고 데이터 소스만 ViewModel로 전환.

## 추가/수정한 파일 (총 4)

### 수정 (3)
- `feature/memorydetail/DetailActivity.kt` — ViewModel 기반 재작성 (B 예외)
- `docs/todos/team_todo.md` — Phase 5 체크
- `docs/handover_to_team_b.md` — B 영향 알림

### 신규 (1)
- `feature/memorydetail/DetailViewModel.kt` — SavedStateHandle + Flow 합성

## 다음 Phase에서 해야 할 일 (팀원 A)

### Phase 6 (안정성 · 설정)
- `#37` Coroutine 전역 예외 처리 — `RoomMemoryRepository.scope`에 `CoroutineExceptionHandler` 부착 + WorkManager Worker 실패 정책 통일
- `#38` 대용량 이미지 OOM 방지 — `ImageImporter`/`ImageClassifier`에 `BitmapFactory.Options.inSampleSize` 적용 (PdfExporter는 이미 inSampleSize=2 + RGB_565 적용됨)
- `#39` URI 권한 만료 — Phase 2에서 import 시점에 즉시 복사하므로 영향 없음. 검증만 필요 (예: 외부 URI grant 만료 시 fallback 경로 확인)
- `#36` 설정 화면 — 설정 항목 정의 + DataStore/SharedPreferences 도입 + 설정 적용 흐름

### 알아둘 점
- DetailActivity는 이제 회전/프로세스 사망 후에도 편집 중이던 메모를 잃지 않음
- 저장 버튼은 미저장 변경분이 있을 때만 활성. UI에 명시적 "수정 중" 표시는 없음 (Phase 6/추가 UI 작업에서 보강 가능)
- ViewModel의 `bind()`는 Activity onCreate마다 호출되지만 동일 ID면 idempotent — 따라서 다른 Detail로 navigate는 새 Activity 인스턴스가 새 VM 생성