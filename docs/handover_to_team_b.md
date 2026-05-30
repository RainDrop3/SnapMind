# 팀원 A → 팀원 B 변경 사항 안내

> 팀원 A 작업 중 **팀원 B의 코드·로직·동작에 영향을 주는 변경**만 추려서 정리.
> 전반적 작업 내역은 `docs/progress.md` 참고.

---

## Phase 1 — DB 설계

**B 코드 직접 변경: 없음.**

알아둘 점:
- Room DB(`SnapMindDatabase`) + 9개 DAO + Hilt `DatabaseModule`이 추가됨. 단, `RepositoryModule`은 여전히 `InMemoryMemoryRepository`를 바인딩 → **이 시점까지 B의 UI 동작은 동일** (시드 데이터 6개 그대로 노출).
- B가 새 DAO를 직접 주입받아 쓰고 싶다면 `DatabaseModule`의 `@Provides`로 모두 노출되어 있음. 단, 동일 데이터를 A의 `MemoryRepository`에서 제공하므로 중복 주입은 피할 것.
- `MemoryRepository` 인터페이스 시그니처 변경 0개 → B 파일 컴파일 영향 0.

---

## Phase 2 — 이미지 수집 (저장 로직)

**B 코드 직접 변경 (3개 파일, 사용자 승인 하 예외):**

1. `data/model/MemoryModels.kt` — `MemoryCategory` enum 재정의 (8 → 9개).
   - 제거: `MAP`
   - 추가: `TRAVEL("Travel","TRIP")`, `FOOD("Food","FOOD")`
   - 사유: `docs/specs/ml-training-spec.md`의 9개 라벨과 정렬 → 매핑 테이블 없이 `valueOf` 직결.
2. `feature/home/MemoryGridAdapter.kt`, `feature/memorydetail/DetailActivity.kt` — `thumbnailBackground()` when 분기에서 `MAP` 삭제, `TRAVEL`/`FOOD`는 임시로 `bg_thumbnail_receipt` 재사용. **Phase 4 UI 폴리싱 시 전용 drawable 2종(`bg_thumbnail_travel`, `bg_thumbnail_food`) 추가 권장**.
3. `res/drawable/bg_thumbnail_map.xml` — 미사용 상태로 보존. B가 정리 가능.

**동작·데이터 흐름 변경 (B 코드는 그대로지만 UX 영향):**

- `RepositoryModule` 바인딩이 `InMemoryMemoryRepository` → `RoomMemoryRepository`로 전환. **`MemoryRepository` 인터페이스 시그니처는 동일**하므로 B의 ViewModel·Activity·Fragment 코드 수정 불필요.
- 시드 데이터 6개 제거 → **첫 실행 시 홈/즐겨찾기/태그 Drawer 모두 빈 화면**. 이미지를 import해야 채워짐. ShareActivity/FAB import 흐름은 그대로 동작.
- `importImage()`는 이제 영속화됨 (앱 종료 후에도 유지). 중복 import는 SHA-256 해시로 감지하여 같은 메모리 객체 반환.
- 자동 태그는 Phase 3 AI 파이프라인 완료 전까지 `#Imported` 한 개만 붙음.

---

## Phase 3 — AI 파이프라인 (ML · OCR)

**B 코드 직접 변경: 없음.**

알아둘 점 (B의 UI 동작에 영향):

- 이제 `importImage()` 직후 WorkManager가 OCR + TFLite 분류 + 자동 태그 파이프라인을 자동 실행. UI는 `MemoryRepository.memories` Flow를 그대로 구독하면 처리 진행에 따라 status·tags·ocrText·category가 점진적으로 채워짐.
- `MemoryItem.processingStatus` 합성 규칙은 그대로 (Phase 2 결정 유지). 처음엔 `PROCESSING` → 모든 단계 완료 시 `DONE` → 단계 중 하나라도 실패하면 `ERROR`.
- **TFLite 모델 도착·정상 동작 확인 (2026-05-30)** → `classificationStatus=SUCCESS`, `processingStatus=DONE`로 정상 마킹됨. 카테고리 Drawer / 자동 태그도 채워짐. 단, **모델 도착 전 import한 기존 메모리**는 `classificationStatus=FAILED`가 영구 박혀있어 ERROR로 표시됨 → 자동 재시도 로직은 Phase 6에서 추가 예정. 현재는 앱 데이터 삭제 또는 신규 import만 정상 표시.
- `MemoryCategory`는 학습된 9개 클래스(`chat, code, document, food, receipt, shopping, travel, unknown, youtube`) 기반. TFLite 결과의 confidence가 0.65 미만이면 `UNKNOWN`. 라벨 순서는 `assets/labels.txt`에서 런타임 로드.

---

## Phase 4 — 검색 백엔드 · 휴지통 · PDF

**B 코드 직접 변경 (사용자 승인 하 예외, 3개 Activity):**

1. `feature/search/SearchActivity.kt` — `MemoryRepository` 직접 호출 제거, 신규 `SearchViewModel`로 위임. 텍스트 입력 250ms debounce, 결과는 Room FTS + Flow 기반. 외부 동작/UI 동일.
2. `feature/utility/TrashActivity.kt` — 툴바 메뉴 "휴지통 비우기"(전체 영구 삭제) 추가, 카드 하트 버튼 = 개별 영구 삭제 확인 다이얼로그(기존 restore에서 변경). 카드 탭 = 복구(유지).
3. `feature/utility/PdfExportActivity.kt` — 버튼 누르면 실제 PDF 생성 후 시스템 공유 chooser 띄움. 이전 placeholder Toast 제거.

**알아둘 점:**

- `MemoryRepository` 인터페이스에 3개 메서드 추가 (`searchFts`, `permanentDelete`, `exportToPdf`). 모두 suspend. B 코드는 기존 동기 메서드도 그대로 사용 가능.
- `AndroidManifest.xml`에 `FileProvider`(`${applicationId}.fileprovider`) provider 추가. PDF 공유용. 다른 곳에서 동일 authority 쓰지 말 것.
- `res/xml/file_paths.xml`, `res/menu/menu_trash.xml` 신규. 다른 곳에서 같은 이름 쓰지 말 것.
- 영구 삭제는 비가역 — UI에서 확인 다이얼로그 항상 띄움. B가 다른 진입점에서 호출할 경우 동일 패턴 권장.
- PDF 생성은 활성 메모리 전체 대상. 선택 기능은 미구현(Phase 5/6에서 B 또는 A가 추가 가능). 큰 메모리 갯수(100+)에서는 수 초 걸릴 수 있음 — 진행 인디케이터 권장.

---

## Phase 5 — 상세 화면 데이터 로직

**B 코드 직접 변경 (사용자 승인 하 예외, 1개 Activity):**

1. `feature/memorydetail/DetailActivity.kt` — `MemoryRepository` 직접 호출 제거, 신규 `DetailViewModel`로 위임. `SavedStateHandle` 기반으로 회전/프로세스 사망 후에도 메모 입력 내용 유지. EditText는 양방향 안전 바인딩(피드백 루프 방지). 외부 UI/레이아웃 변경 없음.

**알아둘 점:**

- 저장 버튼(`saveMemoButton`)은 미저장 변경분이 있을 때만 enable. 이전엔 항상 enable였음. 명시적 "수정 중" 라벨은 미추가 — B가 UI 폴리싱에서 hint text나 아이콘 색상 변경으로 보강 가능.
- 다른 화면(예: SearchActivity)에서 메모리를 수정하거나 삭제하면 DetailActivity가 열려 있을 때도 즉시 반영됨 (Flow 구독). 삭제 시 자동으로 finish().
- Gemini 추천을 수락하면 메모 본문이 추천 텍스트로 덮어쓰이고 미저장 draft도 자동 clear.
- `DetailViewModel`은 `@HiltViewModel` + `SavedStateHandle`만 사용 (Intent 직접 접근 안 함). 다른 진입점(예: 알림 탭) 만들 때 `DetailActivity.createIntent(context, id)` 그대로 사용 가능.
- Application 클래스가 `Configuration.Provider`로 변경됨 + AndroidManifest에 `WorkManagerInitializer` 자동 초기화 제거 provider 추가됨. **B가 별도로 WorkManager 초기화 코드를 추가하면 안 됨** — 자동으로 `HiltWorkerFactory` 기반 설정 사용됨.
- 실패한 처리에 대한 retry 액션 UI(상세 화면 등)는 Phase 5/6에서 함께 설계 예정. 현재는 import 시점 1회 실행만 됨.
