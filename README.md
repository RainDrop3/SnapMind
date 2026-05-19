# SnapMind

왜 저장했는지 잊어버리는 스크린샷과 사진을  
AI 기반으로 자동 분류하고 기록해주는 Android 앱입니다.
유튜브, 강의자료, 코드 에러, 쇼핑 정보, 지도 등  
우리는 정말 많은 스크린샷을 저장합니다.
하지만 시간이 지나면:

- 왜 저장했는지
- 어디에 쓰려고 했는지
- 어떤 내용이었는지
  기억하지 못하는 경우가 많습니다.
  SnapMind는 이런 문제를 해결하기 위해 만든 앱입니다.

---

# 주요 기능

## 1. 스크린샷 공유 저장

사용자가 갤러리나 스크린샷 화면에서  
공유 버튼을 눌러 SnapMind로 이미지를 저장할 수 있습니다.

### 사용 기술

- Android Intent
- ACTION_SEND
- 외부 앱 연동

---

## 2. AI 기반 이미지 자동 분류

저장된 이미지를 머신러닝 모델로 분석하여 자동 분류합니다.

### 분류 카테고리

- video
- code
- shopping
- text
- map
- chat
- meme

### 사용 기술

- TensorFlow Lite
- CNN 기반 이미지 분류 모델

---

## 3. OCR 텍스트 추출

이미지 내부의 텍스트를 자동으로 추출합니다.
추출된 텍스트는:

- 제목 생성
- 검색 기능
- 태그 추천
  등에 활용됩니다.

### 예시

- 유튜브 영상 제목 추출
- 코드 에러 메시지 추출
- 링크 및 키워드 추출

### 사용 기술

- ML Kit Text Recognition

---

## 4. 자동 태그 추천

OCR 결과와 이미지 분류 결과를 기반으로  
태그를 자동 추천합니다.

### 예시

- #과제
- #공부
- #버그
- #구매예정
- #영상

### 사용 기술

- 키워드 분석
- OpenAI API

---

## 5. 메모 및 저장 목적 기록

사용자가 직접 저장 이유를 기록할 수 있습니다.

### 예시

- "나중에 알고리즘 공부"
- "리액트 에러 해결 참고"
- "다음에 살 물건"

---

## 6. 검색 및 필터링

저장된 기록을 빠르게 검색할 수 있습니다.

### 검색 기준

- 태그
- 제목
- 날짜
- 카테고리

### 사용 기술

- RecyclerView
- SearchView
- Room Database

---

# 앱 구조

## HomeActivity

- 최근 저장 목록
- 카테고리별 보기

## AddScreenshotActivity

- 공유 Intent 수신
- OCR 수행
- 이미지 분석
- 태그 추천

## DetailActivity

- 메모 수정
- 태그 수정
- 상세 이미지 보기

## SearchActivity

- 검색 및 필터링

---

# 머신러닝 구성

## 목표

직접 학습한 이미지 분류 모델을 Android 앱에 적용

## 데이터셋

직접 수집한 스크린샷 데이터 사용

### 카테고리

- video
- code
- shopping
- text
- map
- meme

## 학습 환경

- Python
- TensorFlow / Keras

## 앱 적용

## TensorFlow Lite로 변환 후 Android 앱에 적용

# 기술 스택

| 분야                     | 기술                    |
| ------------------------ | ----------------------- |
| Language                 | Kotlin                  |
| Async                    | Coroutine               |
| Networking               | Retrofit                |
| Database                 | Room                    |
| OCR                      | ML Kit                  |
| Machine Learning         | TensorFlow Lite         |
| Image Loading            | Glide                   |
| UI                       | RecyclerView / Fragment |
| External App Integration | Intent                  |

---

# Jetpack Library 활용

- RecyclerView
- Fragment
- ViewModel
- Navigation Component

---

# Coroutine 활용

다음 작업들을 비동기로 처리합니다.

- OCR 처리
- DB 저장
- API 요청
- 이미지 분석

---

# API 활용 계획

## OpenAI API

- 자동 설명 생성
- 태그 추천
- 저장 목적 추정

## YouTube API

- 영상 제목 및 링크 저장

## Notion API (Optional)

- 메모 Export 기능

---

# 기대 효과

- 스크린샷 관리 효율 향상
- 저장 목적 기억 보조
- 빠른 검색 및 분류 제공
- 실사용 가능한 AI 기반 기록 앱 구현

---

# 향후 개선 예정

- 클라우드 동기화
- 유사 이미지 추천
- AI 기반 내용 요약
- 자동 일정 생성
- 개인화 추천 시스템

---

# 개발 환경

- Android Studio
- Kotlin
- Gradle
- TensorFlow Lite

---

# 실행 흐름

```text
스크린샷 저장
    ↓
SnapMind로 공유
    ↓
OCR + AI 분석
    ↓
자동 태그 생성
    ↓
메모 저장
    ↓
검색 및 기록 관리

```
