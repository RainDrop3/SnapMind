# SnapMind

SnapMind는 왜 저장했는지 잊기 쉬운 스크린샷과 이미지를
OCR, 이미지 분류, 태그, 메모 추천, 링크 카드로 다시 찾기 쉽게 정리하는 Android 앱입니다.

유튜브 영상, 검색 결과, 영수증, 문서, 채팅, 여행 정보처럼 흩어진 스크린샷을 저장하고,
나중에 검색, 태그, 카테고리, 메모, PDF 내보내기로 다시 활용하는 흐름을 목표로 합니다.

---

## 주요 기능

### 1. 이미지 저장 및 로컬 분석

- Android 공유 Intent로 외부 앱이나 갤러리에서 이미지를 SnapMind에 저장합니다.
- 저장 직후 WorkManager가 OCR, 이미지 분류, 검색 인덱싱을 백그라운드로 처리합니다.
- 저장된 이미지는 Room Database와 로컬 파일/MediaStore URI를 기준으로 관리합니다.

### 2. OCR 텍스트 추출

- ML Kit Text Recognition과 Korean 모델로 이미지 안의 텍스트를 추출합니다.
- OCR 결과는 상세 화면의 OCR 텍스트 보기, 검색, 링크 자동 인식, 메모 추천에 활용됩니다.
- OCR 텍스트는 선택 가능하게 표시되어 링크가 자동 인식되지 않을 때 직접 복사할 수 있습니다.

### 3. OCR URL 링크 카드

OCR 결과에서 URL을 찾으면 상세 화면에 링크 카드를 표시합니다.

- 페이지 제목, 설명, 대표 이미지를 카드에 표시합니다.
- 일반 웹페이지는 HTML/OG 메타 태그를 가져와 프리뷰를 구성합니다.
- YouTube 링크는 YouTube Data API v3로 제목과 썸네일을 보강합니다.
- 카드 클릭 시 외부 브라우저나 YouTube 앱으로 이동합니다.
- URL 자동 인식 주의사항은 링크 카드의 주황색 정보 버튼에서 확인할 수 있습니다.

#### URL 인식 보강

OCR 특성상 URL 내부가 줄바꿈, 공백, 유사 문자로 깨지는 경우가 있어 다음 보정을 추가했습니다.

- URL 구조 문자(`/`, `?`, `=`, `&` 등) 주변 줄바꿈 복원
- `search. naver.com/search. naver?`처럼 URL 내부에 끼어든 공백 복원
- 한글 검색어가 `?` 또는 `query=` 다음 줄로 분리된 경우 복원
- 네이버 검색 URL이 조각난 경우 `query=` 중심으로 재구성
- YouTube 영상 ID의 `l`, `I`, `1` OCR 혼동 후보 조회

YouTube OCR 보정은 원본 영상 ID 조회가 실패했을 때만 실행합니다.
후보는 최대 24개까지 만들고 YouTube API에 1회만 추가 조회합니다.
후보 중 유효한 영상이 정확히 1개일 때만 자동 보정하고, 여러 개가 유효하면 자동 보정하지 않습니다.

### 4. 링크 안전 확인과 접근 실패 안내

- Google Safe Browsing API로 악성코드, 피싱, 원치 않는 소프트웨어 위험을 확인합니다.
- 위험 가능성이 있으면 빨간 경고 배지를 표시하고, 열기 전 확인 다이얼로그를 띄웁니다.
- Safe Browsing은 악성 여부 확인용이며, 페이지가 실제로 열리는지는 보장하지 않습니다.
- 페이지 프리뷰 HTTP 요청이 실패하면 주황색 `링크 접근 확인 실패` 배지를 표시합니다.
- 링크가 정상적으로 열리지 않으면 OCR 텍스트에서 직접 복사해 브라우저에 붙여넣을 수 있습니다.

### 5. Gemini 메모 추천

- 상세 화면에서 버튼을 눌러 Gemini API로 저장 이유 메모를 추천받습니다.
- 추천 요청은 자동 실행이 아니라 사용자가 직접 누를 때만 실행됩니다.
- 추천 중에는 메모 입력칸 내부에 회색 로딩 오버레이와 인디케이터를 표시합니다.
- Gemini 응답에서 마크다운, 따옴표, 글자 수 표기 같은 불필요한 포맷을 제거합니다.
- 추천은 사용자가 수락하기 전까지 메모 본문을 바로 덮어쓰지 않습니다.

### 6. 화질 업그레이드

- 상세 화면에서 Clipdrop API를 사용해 이미지를 업스케일할 수 있습니다.
- 실행 전 이미지가 온라인으로 업로드된다는 주의 문구와 동의를 받습니다.
- 업스케일 결과는 `Pictures/SnapMind`에 저장되어 갤러리에서도 확인할 수 있습니다.
- 업그레이드 결과 URI는 DB에 저장되어 앱 상세 화면에서도 다시 볼 수 있습니다.

### 7. 카테고리, 태그, 검색

- 이미지 분류 결과를 기반으로 카테고리를 부여합니다.
- 한 메모리에 카테고리는 최대 2개까지 지정할 수 있습니다.
- 카테고리가 2개가 되면 `+ 카테고리` 버튼이 사라지고, 삭제하면 다시 나타납니다.
- 이미 선택된 카테고리는 추가 목록에서 제외됩니다.
- 태그, 카테고리, OCR, 메모, 링크 정보로 검색할 수 있습니다.

### 8. 관리 기능

- 즐겨찾기
- 휴지통 이동 및 복원
- PDF 내보내기
- PDF 캐시 정리
- 태그 관리
- 개발자 소개 화면

개발자 소개 화면은 역할 중심 문구를 제거하고, 현재 앱 기능을 설명하는 프로젝트 소개로 업데이트했습니다.
프로젝트 소개 박스는 긴 문구를 안정적으로 감싸도록 전용 배경과 작은 radius를 사용합니다.

---

## 설정 화면

설정 화면에서는 원격 보강 기능을 켜고 끌 수 있습니다.

- `OCR URL 링크 카드`
- `YouTube 링크 보강`
- `악성 링크 경고`
- `화질 업그레이드 API`
- `Gemini 메모 추천`

`YouTube 링크 보강`과 `악성 링크 경고`는 `OCR URL 링크 카드`의 하위 옵션입니다.
상위 옵션을 끄면 하위 옵션도 함께 비활성화됩니다.

각 기능 이름 옆의 정보 버튼에서 기능 설명을 확인할 수 있습니다.
설정 화면에는 API 키 보유 여부를 표시하지 않습니다.

---

## 사용 중인 API와 역할

| API/기술 | 역할 |
| --- | --- |
| ML Kit Text Recognition | 이미지 내부 OCR 텍스트 추출 |
| TensorFlow Lite | 이미지 카테고리 분류 |
| YouTube Data API v3 | YouTube 영상 제목, 설명, 썸네일 보강 |
| Google Safe Browsing API | 악성/피싱 링크 위험 확인 |
| Gemini API | 저장 이유 메모 추천 |
| Clipdrop API | 이미지 화질 업그레이드 |
| OkHttp + Jsoup | 일반 웹페이지 HTML/OG 링크 프리뷰 수집 |

---

## API 키 설정

API 키는 앱 설정 화면에서 입력하지 않고, 빌드 시 `local.properties`에서 읽어 `BuildConfig`에 주입합니다.
`local.properties`는 git에 커밋되지 않습니다.

1. `local.properties.template`를 복사해 프로젝트 루트의 `local.properties`로 만듭니다.
2. 필요한 키 값을 채웁니다.

```properties
GEMINI_API_KEY=
YOUTUBE_API_KEY=
SAFE_BROWSING_API_KEY=
CLIPDROP_API_KEY=
```

키가 비어 있으면 해당 원격 기능은 실행되지 않거나 fallback 동작으로 처리됩니다.

---

## 기술 스택

| 분야 | 기술 |
| --- | --- |
| Language | Kotlin |
| Architecture | Activity, Fragment, ViewModel |
| Async | Coroutine, Flow |
| Background Work | WorkManager, Hilt Worker |
| Database | Room |
| OCR | ML Kit Text Recognition |
| Machine Learning | TensorFlow Lite |
| Networking | Retrofit, OkHttp, Jsoup |
| Image Loading | Glide |
| Dependency Injection | Hilt |
| UI | Material Components, RecyclerView, ViewPager2, DrawerLayout |
| External App Integration | Android Intent |

---

## 주요 화면

### Home

- 최근 저장 목록
- 카테고리/태그/즐겨찾기 탐색
- 이미지 업로드 FAB
- Drawer 메뉴 진입

### Detail

- 이미지 미리보기
- 카테고리 최대 2개 편집
- 태그 편집
- 메모 수정 및 Gemini 추천 수락
- 링크 카드 표시
- OCR 텍스트 보기
- 화질 업그레이드
- 즐겨찾기/삭제/저장

### Settings

- 원격 보강 기능 on/off
- 하위 옵션 비활성화 상태 표시
- 기능별 정보 다이얼로그
- PDF 캐시 정리
- 저장된 메모리/태그 상태 요약

### Utility

- 검색
- 태그 관리
- 휴지통
- PDF 내보내기
- 개발자 소개

---

## 처리 흐름

```text
이미지 공유/선택
    ↓
Room에 메모리 생성
    ↓
LocalMemoryProcessingWorker
    ├─ OCR 추출
    ├─ 이미지 분류
    └─ 원격 보강 작업 예약
    ↓
RemoteEnrichmentWorker
    ├─ OCR URL 추출 및 보정
    ├─ Safe Browsing 검사
    ├─ YouTube/HTML 링크 프리뷰 수집
    └─ 검색 인덱스 갱신
    ↓
상세 화면에서 링크 카드, OCR, 메모, 태그 확인
```

Gemini 메모 추천과 Clipdrop 화질 업그레이드는 자동 실행되지 않고, 상세 화면에서 사용자가 버튼을 누를 때만 실행됩니다.

---

## 테스트

전체 디버그 빌드와 단위 테스트:

```bash
./gradlew :app:assembleDebug :app:testDebugUnitTest
```

최근 보강된 테스트 범위:

- URL 줄바꿈 복원
- URL 내부 OCR 공백 복원
- 네이버 검색 URL 조각 복원
- YouTube URL 파싱
- YouTube `l/I/1` OCR 혼동 후보 생성
- Gemini 메모 추천 응답 정리
- Clipdrop 업스케일 이미지 크기 처리

---

## 주의 사항

- OCR 기반 URL 자동 인식은 이미지 품질과 OCR 결과에 따라 실패할 수 있습니다.
- 링크가 정상적으로 열리지 않으면 OCR 텍스트에서 직접 복사해 브라우저에 붙여넣어 이동하세요.
- Safe Browsing은 악성 여부 확인용이며, 페이지 접근 가능 여부를 보장하지 않습니다.
- 화질 업그레이드는 사용자가 동의한 경우에만 이미지를 외부 API 서버로 업로드합니다.
- API 키와 개인 로컬 설정은 git에 커밋하지 않습니다.
