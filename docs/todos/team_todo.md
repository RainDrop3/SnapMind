# SnapMind — 기능별 역할 분담 TODO

> 체크박스를 완료 처리하며 진행 상황을 추적하세요.  
> 번호(#N)는 기능 리스트 원본 번호와 일치합니다.

---

## Phase 1 — 기반 세팅 `1주차`

> 🤝 **공동** : 앱 패키지 구조 및 아키텍처 합의 / ViewModel · Repository 인터페이스 공동 정의

### 👤 팀원 A — DB 설계

- [x] `#11` Room DB 스키마 설계 (MemoryItem, Tag, N:M 매핑) `DB`
- [x] `#12` DAO 구현 (insert / update / delete / query) `DB`
- [x] `#14` Room FTS 검색 인덱스 설정 `DB`

### 👤 팀원 B — 앱 셸

- [x] Hilt DI 의존성 주입 설정 `CORE`
- [x] `#15` RecyclerView Grid 갤러리 기본 레이아웃 `UI`
- [x] ViewPager2 + BottomNavigationView 앱 셸 구현 `UI`

---

## Phase 2 — 이미지 수집 `2주차`

### 👤 팀원 A — 저장 로직

- [x] `#3` 수신 이미지 앱 내부 저장소 복사 및 URI 관리 `CORE`
- [x] `#13` 태그 저장 및 N:M 매핑 로직 구현 `DB`

### 👤 팀원 B — 수집 UI

- [x] `#1` ACTION_SEND 외부 공유 수신 (ShareActivity 구현) `CORE`
- [x] `#2` 업로드 FAB — 갤러리 직접 선택 `UI`
- [x] `#16` 처리 상태 뱃지 UI (처리중 / 완료 / 에러) `UI`

---

## Phase 3 — AI 파이프라인 `3~4주차 (병렬)`

> 🤝 **공동** : Coroutine 파이프라인 흐름 합의 (순서 및 에러 전파 방식)

### 👤 팀원 A — ML · OCR

- [ ] `#5` TFLite CNN 모델 학습 (Colab — 병렬 진행) `ML`
- [ ] `#4` ML Kit OCR 비동기 텍스트 추출 구현 `CORE`
- [ ] `#6` TFLite 앱 탑재 및 추론 결과 DB 업데이트 `ML`
- [ ] `#9` 자동 태그 생성 룰 엔진 (OCR + Vision 결과 기반) `CORE`

### 👤 팀원 B — 외부 API

- [/] `#7` Google Cloud Vision API 이미지 라벨링 연동 `API` — Retrofit service / DTO 구현, 파이프라인 연동 대기
- [/] `#8` Gemini API 메모 자동 추천 연동 `API` — Retrofit service / DTO 구현, 파이프라인 연동 대기
- [/] `#10` YouTube API 영상 제목 검색 → videoId → 딥링크 `API` — Retrofit service / DTO 구현, OCR 후보 연동 대기
- [x] Retrofit 공통 클라이언트 모듈화 및 에러 처리 `CORE`

---

## Phase 4 — 화면 구현 `4~5주차`

### 👤 팀원 A — 검색 백엔드 · Drawer 로직

- [ ] `#19` Room FTS 통합 검색 쿼리 작성 `DB`
- [ ] `#20` 검색 결과 Flow 스트림 및 ViewModel 구성 `CORE`
- [ ] `#32` 휴지통 — 삭제 이미지 복구 로직 `CORE`
- [ ] `#33` PDF 추출 기능 구현 `CORE`

### 👤 팀원 B — 탭 화면 UI

- [x] `#17` 홈 화면 우상단 검색 버튼 및 검색 UI `UI`
- [x] `#18` 카테고리/태그 필터 및 유틸리티 UI (DrawerLayout 좌측 메뉴) `UI`
- [x] `#21` 즐겨찾기 추가 / 해제 토글 UI `UI`
- [x] `#22` 즐겨찾기 탭 — 즐겨찾기 이미지 갤러리 화면 `UI`
- [x] `#23` 태그별 사진 탭 — 태그 목록 화면 `UI`
- [x] `#24` 태그 선택 시 이미지 필터링 연동 `UI`

---

## Phase 5 — 상세 화면 & Drawer `5~6주차`

> 🤝 **공동** : DetailActivity ↔ MainActivity Intent 양방향 흐름 합의

### 👤 팀원 A — 상세 데이터 로직

- [ ] `#25` DetailActivity — 이미지 원본 및 OCR 텍스트 표시 `UI`
- [ ] `#26` 분류 카테고리 및 태그 표시 `UI`
- [ ] `#28` 메모 작성 / 수정 로직 및 DB 연동 `DB`

### 👤 팀원 B — 상세 · Drawer UI

- [x] `#27` 메모 작성 / 수정 UI `UI`
- [x] `#29` Gemini 메모 추천 결과 표시 및 수락 UI `UI`
- [x] `#30` YouTube 딥링크 버튼 UI (유튜브 스크린샷 한정) `UI`
- [x] `#31` 즐겨찾기 토글 UI `UI`
- [x] `#34` 개발자 소개 화면 `UI`
- [x] `#35` 인기 태그 TOP 3 바로가기 (Drawer 연동) `UI`

---

## Phase 6 — 안정성 & 마무리 `6~7주차`

> 🤝 **공동** : 통합 테스트 및 데모 시연 준비

### 👤 팀원 A — 안정성 · 설정

- [ ] `#37` Coroutine 전역 예외 처리 (CoroutineExceptionHandler) `안정성`
- [ ] `#38` 대용량 이미지 OOM 방지 (BitmapFactory.Options) `안정성`
- [ ] `#39` URI 권한 만료 대응 검증 `안정성`
- [ ] `#36` 설정 화면 구현 `UI`

### 👤 팀원 B — 안정성 · UI 마무리

- [x] `#40` DrawerLayout ↔ ViewPager2 제스처 충돌 해결 `안정성`
- [ ] `#41` Room DB 마이그레이션 관리 `안정성`
- [x] Glide 썸네일 로딩 최적화 `UI`
- [/] 전체 UI 폴리싱 및 애니메이션 정리 `UI` — 기본 UI polish 완료, 추가 애니메이션 정리 가능

---

## 진행 현황

| Phase | 팀원 A | 팀원 B | 공동 |
|---|---|---|---|
| Phase 1 — 기반 세팅 | 3 / 3 | 3 / 3 | 0 / 2 |
| Phase 2 — 이미지 수집 | 2 / 2 | 3 / 3 | — |
| Phase 3 — AI 파이프라인 | 0 / 4 | 1 / 4 | 0 / 1 |
| Phase 4 — 화면 구현 | 0 / 4 | 6 / 6 | — |
| Phase 5 — 상세 화면 & Drawer | 0 / 3 | 6 / 6 | 0 / 1 |
| Phase 6 — 안정성 & 마무리 | 0 / 4 | 2 / 4 | 0 / 1 |
| **합계** | **5 / 20** | **21 / 26** | **0 / 5** |
