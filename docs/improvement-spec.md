# SnapMind 개선 명세서 (피드백 구체화)

> 작성일: 2026-06-02
> 목적: 현재 빌드에서 수집된 12개 사용자 피드백을 코드 근거와 함께 구체적 작업 단위로 재정의한다.
> 범례 — **영역**: 팀원 A(데이터/AI/저장) · 팀원 B(UI/외부 API). B 영역 코드 수정은 `docs/specs/*`와 어긋날 때 **사용자 승인 하에만** 진행한다.

---

## 한눈에 보기

| # | 요약 | 핵심 영역 | 주요 파일 | 비고 |
|---|------|-----------|-----------|------|
| 1 | PDF 추출 — 항목 선택 + 저장 옵션 | A·B | `PdfExportActivity`, `RoomMemoryRepository.exportToPdf` | 현재 전체만, 공유만 |
| 2 | 업로드 시 편집 화면 선행(태그·메모 수정 후 저장) | B(UI)·A(저장) | `ShareActivity`, FAB 흐름 | 현재 미리보기→즉시 저장 |
| 3 | Vision/Gemini/YouTube 3개 API 실제 연결 | B(API)·A(파이프라인) | `RemoteEnrichmentRepository`, 워커 | 구현됐으나 미호출 |
| 4 | 홈에서 롱프레스 다중 선택 → 삭제·PDF | B(UI) | `MemoryGridFragment/Adapter` | 선택 모드 없음 |
| 5 | 개발자 소개 내용 수정 | B(UI) | `DeveloperInfoActivity`, `activity_developer_info.xml` | "Team B" 더미 |
| 6 | Drawer 폰트 ↔ 홈 로고 폰트 통일 | B(UI) | `activity_main.xml`, `themes.xml` | 통일된 타이포 없음 |
| 7 | 휴지통 카드의 하트 아이콘 → 휴지통 아이콘 | B(UI) | `MemoryGridAdapter`, `TrashActivity` | 하트 재사용 중 |
| 8 | 휴지통: 탭=복구 제거, 롱프레스 선택 → 영구삭제·복원 | B(UI)·A(로직) | `TrashActivity` | 현재 탭=복구 |
| 9 | 검색 돋보기·휴지통 비우기 아이콘 검은색 | B(UI) | `ic_search`, `menu_trash` | 틴트 변경 |
| 10 | 상세 화면에 최근 수정 날짜 표시 | A·B | `DetailActivity`, `MemoryItem` | `updatedAt` 미노출 |
| 11 | `#Imported` 태그 정체/필요성 | A | `RoomMemoryRepository` | 시드 태그(설계 산물) |
| 12 | OCR 한글 깨짐 | A | `OcrExtractor` | Latin 인식기 사용 |

---

## 1. PDF 추출 — 원하는 항목만 선택 + 저장 기능

**현재 상태**
- `PdfExportActivity`(`feature/utility/PdfExportActivity.kt:31`)는 버튼 한 번에 **활성 메모리 전체**(`memoryRepository.activeMemories()`)를 대상으로 PDF를 만든다. 개별 선택 UI가 없다.
- 결과는 `ACTION_SEND` 공유 chooser로만 전달된다(`:43`). 기기에 **저장(다운로드/SAF)** 하는 경로가 없다.
- 백엔드 `RoomMemoryRepository.exportToPdf(memoryIds)`(`:293`)는 이미 ID 목록을 받도록 되어 있어 **선택 기능을 받을 준비는 되어 있다**(현재 호출부가 전체 ID만 넘김).

**요청 / 개선안**
1. **항목 선택 UI**: PDF 화면을 활성 메모리 썸네일 그리드 + 체크박스(또는 다중선택)로 구성. 선택된 ID만 `exportToPdf(selectedIds)`로 전달.
2. **저장 옵션 추가**: 기존 "공유"에 더해 "기기에 저장" 액션 제공.
   - 권장: `ACTION_CREATE_DOCUMENT`(SAF) 또는 `MediaStore`(Downloads)로 사용자가 위치 선택해 저장.
   - 진행 인디케이터 표시(100+ 항목 시 수 초 소요 — `handover_to_team_b.md` Phase 4 주의).
3. (선택) 진입 경로 확장: #4의 홈 다중선택에서 바로 이 화면(선택 프리필)으로 진입.

**영향 파일 / 승인**
- `PdfExportActivity.kt`, `activity_pdf_export.xml` — B 영역 UI(단, Phase 4에서 A가 예외 수정한 이력 있음) → **선택/저장 UI 추가는 사용자 승인 필요**.
- `PdfExporter`(`core/pdf/PdfExporter.kt`) — 저장 경로 추가 시 A 영역.

---

## 2. 사진 업로드 시 편집 화면 선행 → 확인 시 저장 (태그·메모 수정 가능)

**현재 상태**
- `ShareActivity`(`feature/importimage/ShareActivity.kt`)는 공유 이미지의 **첫 장 썸네일과 개수만** 보여주고(`:43-52`), 저장 버튼을 누르면 곧바로 `importImage()`로 전부 저장한다(`:54-81`). 편집 단계가 없다.
- 저장 시 태그는 자동 시드 태그 `#Imported` 하나만 붙고(11번 참조), 메모는 기본 문구로 채워진다(`RoomMemoryRepository.importImage` `:170`, `:180`). 사용자가 저장 전 손댈 수 없다.
- FAB(갤러리 직접 선택) 경로도 동일하게 즉시 저장된다.

**요청 / 개선안**
1. **편집 화면 선행**: 업로드(공유/FAB) → 저장 전 **편집 화면**을 띄운다. 항목별로:
   - 분류 태그(자동 분류 결과) **확인·수정/추가/삭제**,
   - 메모 **입력·수정**,
   - 미리보기.
2. **확인 시에만 저장**: "확인" 시 입력값과 함께 영속화, "취소" 시 폐기.
3. 다중 업로드 시 항목별 편집 또는 일괄 편집 UX 결정 필요(**결정 필요**).

**기술적 고려 (중요)**
- 현재 분류/태그는 저장 **후** WorkManager 파이프라인에서 비동기로 채워진다(`RoomMemoryRepository.importImage` → `enqueueLocalProcessing` `:191`). 즉 **저장 전에는 자동 태그가 아직 없다.**
- 따라서 "편집 화면에서 분류된 태그 수정"을 하려면 둘 중 하나:
  - (a) 저장 전 동기적으로 임시 분류/OCR 1회 수행 후 편집 화면에 표시, 또는
  - (b) 임시 저장(드래프트) 후 파이프라인 결과를 편집 화면에 보여주고 확정.
- `MemoryRepository`에 "초안 import + 사후 확정/메모·태그 일괄 반영" 시그니처 추가가 필요할 수 있음 → **A·B 인터페이스 합의 필요**.

**영향 파일 / 승인**
- `ShareActivity.kt`(B 수집 UI) + 신규 편집 화면/레이아웃 → **사용자 승인 필요**.
- `MemoryRepository`/`RoomMemoryRepository`(A) — 초안/확정 흐름 추가 시.

---

## 3. 외부 API 3종(Vision · Gemini · YouTube) 실제 연결

**현재 상태 (핵심)**
- 3개 Retrofit 서비스와 DTO, 공통 호출부 `RemoteEnrichmentRepository`(`data/remote/common/RemoteEnrichmentRepository.kt`)는 **모두 구현되어 있다**: `labelImage`(Vision), `suggestMemo`(Gemini), `findYoutubeVideo`(YouTube).
- 그러나 **이 리포지토리를 호출하는 코드가 앱 어디에도 없다.** 가져오기 파이프라인은 `LocalMemoryProcessingWorker`(OCR + TFLite 분류) → `AutoTaggingWorker`로 끝나며, 원격 API를 부르는 워커가 없다. (`team_todo.md` Phase 3 B 항목이 `[/]` "파이프라인 연동 대기" 상태)
- **API 키 저장소가 없다.** `AppPreferences`에는 `vision/gemini/youtubeEnabled` **on/off 플래그만** 있고(`core/settings/AppPreferences.kt:27-37`) 키 입력/보관 필드가 없다. 설정 화면(`SettingsFragment`)도 스위치만 노출.
- `RemoteEnrichmentRepository.runRemote`는 `apiKey.isBlank()`면 `RemoteFeatureDisabled`를 반환(`:107`) — 즉 키가 공급되지 않으면 전부 비활성.

**요청 / 개선안**
1. **API 키 공급 경로 확정 (결정됨, 2026-06-16)**: `local.properties` → `BuildConfig` 내장 방식 채택. `app/build.gradle.kts`가 git에 커밋되지 않는 `local.properties`에서 `GEMINI/VISION/YOUTUBE_API_KEY`를 읽어 `BuildConfig`에 주입하고, `AppPreferences`의 키 getter가 이 값을 반환한다. 설정 화면의 키 입력란은 제거됨(스위치만 유지). 템플릿: `local.properties.template`.
2. **원격 보강 워커 신설**: 로컬 처리 완료 후 실행되는 `RemoteEnrichmentWorker`(가칭) 추가.
   - 게이팅: `AppPreferences.visionEnabled/geminiEnabled/youtubeEnabled` 플래그로 각 호출 on/off (`handover_to_team_b.md` Phase 6 안내가 이 연동을 전제로 작성됨).
   - Vision 라벨 → `VisionLabelEntity` 저장, 자동 태그 룰 엔진에 반영.
   - Gemini 추천 → `MemoEntity.geminiSuggestion` 저장(상세 화면 제안 칩이 이미 이 값을 소비함, `DetailActivity:135`).
   - YouTube → OCR/제목 기반 검색 → `YoutubeLinkEntity` 저장(상세 화면 영상 버튼이 소비, `DetailActivity:141`).
3. **이미지 페이로드**: Vision/Gemini는 base64 JPEG 필요 → `BitmapDecoder`로 다운샘플 후 인코딩(프라이버시 문서: 다운샘플 페이로드만 전송).
4. **에러/쿼터 처리**: `RemoteEnrichmentRepository`가 이미 `ApiTimeout/Unauthorized/QuotaExceeded` 매핑 제공 → 워커에서 상태 반영 및 재시도 정책 결정.

**영향 파일 / 승인**
- 신규 워커 + DI(A 파이프라인 영역), 키 입력 UI는 B 설정 영역.
- B의 Phase 3 미완 항목(#7/#8/#10) 완료에 해당 → **A·B 공동 진행 + 사용자 승인** 권장.

---

## 4. 홈 — 롱프레스 다중 선택 → 일괄 삭제 / PDF 추출

**현재 상태**
- 홈 그리드(`MemoryGridFragment` + `MemoryGridAdapter`)는 **단일 탭만** 처리한다: 카드 탭 → 상세 화면(`MemoryGridFragment.kt:35`), 하트 → 즐겨찾기 토글(`:36`). 롱프레스/선택 모드가 없다.
- 삭제는 상세 화면에서 개별 soft-delete만 가능(`DetailActivity:51`).

**요청 / 개선안**
1. **선택 모드**: 카드 롱프레스로 선택 모드 진입, 추가 탭으로 다중 선택/해제, 선택 카드 시각 표시(체크/오버레이).
2. **컨텍슈얼 액션 바**: 선택 모드에서 상단(또는 하단)에 액션 노출:
   - **삭제**(휴지통으로 soft-delete, 선택 N개 일괄),
   - **PDF로 추출**(선택 ID → #1의 PDF 화면 또는 직접 `exportToPdf(selectedIds)`).
3. 빈 선택/전체 선택/선택 해제 UX 및 뒤로가기로 선택 모드 종료.

**기술 메모**
- `MemoryRepository.softDelete(id)`는 단건이라 루프 호출 또는 일괄 메서드 추가 고려.
- 즐겨찾기 탭(`FavoritesFragment`)도 동일 `MemoryGridFragment` 파생이면 동작 일관성 확인.

**영향 파일 / 승인**
- `MemoryGridFragment.kt`, `MemoryGridAdapter.kt`, 관련 레이아웃 — **B 영역, 사용자 승인 필요**.

---

## 5. 개발자 소개 화면 수정

**현재 상태**
- `DeveloperInfoActivity`(`feature/utility/DeveloperInfoActivity.kt`)는 레이아웃만 띄우는 빈 화면이며, `activity_developer_info.xml:41`에 더미 텍스트 **"SnapMind Team B"** 가 들어 있다.

**요청 / 개선안**
- 실제 개발자/팀 소개 내용으로 교체: 팀명·구성원·역할·연락/링크·앱 소개 등(**최종 문구 사용자 제공 필요**).
- 레이아웃 정리(프로필/버전/링크 섹션) 필요 시 함께.

**영향 파일 / 승인**
- `DeveloperInfoActivity.kt`, `activity_developer_info.xml` — B 영역 UI. 문구 확정 후 수정.

---

## 6. Drawer 폰트 ↔ 홈 로고 폰트 통일

**현재 상태**
- 홈 "로고": 별도 이미지 로고 없이 툴바 타이틀 텍스트 `app:title="SnapMind"`(`activity_main.xml:31`). 테마 기본 폰트 사용.
- Drawer 헤더: `TextView` `android:text="SnapMind"`, `textSize=24sp`, `textStyle=bold`(`activity_main.xml:90-96`).
- 테마는 `android:fontFamily="sans"`만 지정(`values/themes.xml:4`, `values-night/themes.xml:4`). 전용 브랜드 서체/`TextAppearance`가 없어 위치마다 렌더가 달라 보임.

**요청 / 개선안**
1. 브랜드 워드마크용 **공통 `TextAppearance`(또는 커스텀 폰트)** 정의 → 홈 툴바 타이틀과 Drawer 헤더가 **동일 서체/웨이트** 사용.
2. (선택) 커스텀 폰트 도입 시 `res/font/` 추가(현재 없음) 후 테마/스타일에서 참조.

**영향 파일 / 승인**
- `activity_main.xml`, `values/themes.xml`, (신규) `res/font/`, `styles` — B 영역 UI. 시각 디자인 결정 사항.

---

## 7. 휴지통 — 카드의 하트 아이콘 → 휴지통 아이콘으로 변경

**현재 상태**
- 휴지통은 홈과 **같은 `MemoryGridAdapter`** 를 재사용한다(`TrashActivity.kt:32`). 카드의 하트 버튼(`favoriteButton`)을 **영구 삭제 트리거로 재활용**(`onFavoriteClick = { confirmPermanentDelete(it.id) }` `:37`)하지만, 아이콘 자체는 여전히 **하트**다(`MemoryGridAdapter.bind`에서 하트 틴트만 설정, `:56-62`).
- 결과적으로 "휴지통인데 하트를 누르면 영구삭제"라 직관과 어긋남.

**요청 / 개선안**
- 휴지통 컨텍스트에서 카드 우상단 액션 아이콘을 **휴지통(삭제) 아이콘**(`ic_trash`)으로 표시.
- 구현 옵션:
  - (a) `MemoryGridAdapter`에 표시 모드 파라미터 추가(`favorite` vs `delete`)해 아이콘/틴트 분기, 또는
  - (b) 휴지통 전용 어댑터/뷰홀더 분리.
- #8의 선택 모드 도입과 함께 설계하면 일관성↑.

**영향 파일 / 승인**
- `MemoryGridAdapter.kt`(공유 컴포넌트), `TrashActivity.kt` — **B 영역, 사용자 승인 필요**(어댑터는 홈과 공유되므로 회귀 주의).

---

## 8. 휴지통 — 탭=복구 제거, 롱프레스 선택 → 영구삭제 / 복원

**현재 상태**
- 현재 동작: 카드 **탭 = 즉시 복구**(`TrashActivity.kt:33-35`), 하트 = 개별 영구삭제 확인(`:37`), 툴바 "휴지통 비우기" = 전체 영구삭제(`:55`).
- 실수로 탭하면 바로 복구되는 등 의도와 다른 동작.

**요청 / 개선안**
1. **탭=복구 제거.** 단일 탭은 (예: 상세/미리보기) 또는 무동작으로 변경(**결정 필요**).
2. **롱프레스 다중 선택 모드** 도입(#4와 동일 패턴):
   - 선택 후 **영구 삭제**(`permanentDelete`, 확인 다이얼로그 유지 — 비가역),
   - 선택 후 **복원**(`restore`).
3. 기존 "휴지통 비우기"(전체 영구삭제)는 유지 가능.

**기술 메모**
- 백엔드 메서드는 이미 존재: `restore(id)`, `permanentDelete(id)`(suspend). 다건 처리는 루프 또는 일괄 메서드.

**영향 파일 / 승인**
- `TrashActivity.kt`(Phase 4에서 A가 예외 수정한 파일), 어댑터 선택 모드 — **사용자 승인 필요**.

---

## 9. 검색 돋보기 / 휴지통 비우기 아이콘 색상 → 검은색

**현재 상태**
- 검색 아이콘: `menu_main_toolbar.xml`의 `action_search`가 `@drawable/ic_search` 사용(`:7`). 색은 툴바/테마 틴트를 따름(메인 툴바 `navigationIconTint=@color/snap_text`지만 메뉴 아이콘 틴트는 별도 미지정).
- 휴지통 비우기: `menu_trash.xml`의 `action_empty_trash`는 **아이콘 없이 텍스트만**(`showAsAction=ifRoom`). "버튼"이 검은색이 되려면 아이콘/오버플로 텍스트 색 지정 필요.

**요청 / 개선안**
1. 검색 돋보기 아이콘 틴트를 **검정(또는 `snap_text`)** 으로 명시 — 메뉴 아이템 틴트 또는 툴바 `app:iconTint` 지정.
2. 휴지통 비우기: 아이콘(예: 휴지통/비우기)을 검정 틴트로 추가하거나, 텍스트 색을 검정으로.
3. 다크 모드 대비 확인(검정 고정 시 야간 가독성 — `values-night` 대응 **결정 필요**).

**영향 파일 / 승인**
- `menu_main_toolbar.xml`, `menu_trash.xml`, 관련 툴바 레이아웃 — B 영역 UI.

---

## 10. 상세 화면 — 최근 수정 날짜 표시

**현재 상태**
- 상세 화면(`DetailActivity.render`)은 카테고리/메모/OCR/태그/제안/유튜브만 렌더하고 **날짜 정보를 표시하지 않는다**.
- 도메인 모델 `MemoryItem`에는 `createdAtMillis`, `deletedAtMillis`만 있고 **`updatedAt`(수정 시각)이 노출되어 있지 않다**(`data/model/MemoryModels.kt:21-38`). DB 엔티티(`MemoryItemEntity`)에는 `updatedAt`이 있으나 도메인까지 매핑되지 않음.

**요청 / 개선안**
1. **`MemoryItem`에 `updatedAtMillis` 노출**: `EntityMappers`(`toDomain`)에서 엔티티 `updatedAt` 매핑.
2. 상세 화면에 **"최근 수정: yyyy.MM.dd HH:mm"**(또는 상대 시간) 표시 TextView 추가.
3. "수정 시각" 정의 확정(**결정 필요**): 메모 편집·즐겨찾기·태그 변경 등 어떤 변경을 수정으로 볼지. 현재 DAO들이 `updatedAt`을 갱신하는 지점 점검 필요.

**영향 파일 / 승인**
- `MemoryModels.kt`, `EntityMappers.kt`(A 영역) + `DetailActivity.kt`/`activity_memory_detail.xml`(B UI). 모델 변경은 B 컴파일에 영향 적음(필드 추가).

---

## 11. `#Imported` 태그 — 정체와 존재 이유

**조사 결과 (사실)**
- `#Imported`는 **가져오기 시 자동으로 붙는 시드(seed) 태그**다. `RoomMemoryRepository.importImage`가 저장 직후 `SEED_IMPORT_TAG = "Imported"`를 `AUTO`/`SYSTEM` 소스로 부여한다(`RoomMemoryRepository.kt:180-188`, `:350`).
- 설계 의도: `handover_to_team_b.md` Phase 2 — *"자동 태그는 Phase 3 AI 파이프라인 완료 전까지 `#Imported` 한 개만 붙음."* 즉 AI 태깅 전 **모든 항목이 최소 1개의 태그를 갖도록** 하는 임시/플레이스홀더 태그.
- 현재는 AI 분류·자동 태깅이 동작하므로, `#Imported`는 **"아직 의미 있는 태그가 없는 항목"의 잔여 표식**처럼 남는다.

**요청 / 개선안 (결정 필요)**
- 셋 중 택1:
  - (a) **유지하되 UI에서 숨김**: 내부 추적용으로 두되 카드/상세/태그 Drawer 노출에서 필터링.
  - (b) **AI 태그 생성 후 자동 제거**: 의미 태그가 1개 이상 붙으면 `#Imported` 정리.
  - (c) **명칭/용도 재정의**: "미분류" 등 사용자 친화적 태그로 전환.
- 검색/필터(`filterByTag`, 태그 카운트)에서의 영향도 함께 점검.

**영향 파일 / 승인**
- `RoomMemoryRepository.kt`, `TagAssigner` 등 — A 영역.

---

## 12. OCR 한글 깨짐 — 원인과 해결

**원인 (확정)**
- `OcrExtractor`가 ML Kit **라틴 전용 인식기**를 사용한다: `TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)`(`core/ai/OcrExtractor.kt:8`, `:28`). `DEFAULT_OPTIONS`는 라틴 스크립트 모델이라 **한글을 제대로 인식하지 못해 깨진/누락된 결과**가 나온다. 인코딩 문제가 아니라 **모델 선택 문제**.

**해결안**
1. **한국어 인식 모델로 교체/추가**: ML Kit Korean text recognition 사용.
   - 의존성: `com.google.mlkit:text-recognition-korean`,
   - 옵션: `KoreanTextRecognizerOptions.Builder().build()`로 클라이언트 생성.
   - 참고: 한국어 모델은 라틴/숫자도 함께 인식하므로 단일 교체로 한글+영문 혼용 처리 가능. (다국어 동시 필요 시 스크립트별 인식기 다중 운용 검토)
2. 교체 후, **기존에 라틴 모델로 잘못 추출된 메모리는 OCR 재처리** 필요(현재는 import 시 1회만 실행 — 재처리/재시도 경로는 `handover` Phase 3/6에서 예정).
3. APK 크기/모델 번들 방식(앱 번들 vs Play 서비스 다운로드) 결정.

**영향 파일 / 승인**
- `OcrExtractor.kt`, `app/build.gradle`(의존성) — A 영역(AI). 재처리 트리거는 워커/리포지토리.

---

## 공통 결정 필요 항목 (사용자 확인 요청)

1. **#2 다중 업로드** 편집 UX: 항목별 vs 일괄.
2. ~~**#3 API 키 공급 방식**~~: 결정됨 — `local.properties` → `BuildConfig` 내장(설정 입력란 제거). (2026-06-16)
3. **#8 휴지통 단일 탭** 동작: 미리보기 vs 무동작.
4. **#9 다크모드** 아이콘 색 고정 여부.
5. **#10 "수정 시각" 정의** 범위.
6. **#11 `#Imported`** 처리 방향(숨김/제거/재정의).
7. **#6 폰트** 커스텀 서체 도입 여부.

> B 영역(UI) 수정이 다수 포함됨 → 각 항목 착수 전 `docs/specs/*` 정합성 확인 및 **사용자 승인** 후 진행.
