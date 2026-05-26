# Retrofit Specification

## Overview

SnapMind uses Retrofit for three external APIs that are core features:

| API | Purpose | Scoring |
| --- | --- | --- |
| Google Cloud Vision API | Image label detection for auto-tagging | API 1/3 |
| Gemini API | Memo sentence recommendation | API 2/3 |
| YouTube Data API v3 | Video search by OCR-extracted title | API 3/3 |

## Base Rules

- Core on-device features (OCR, TFLite) do not require network; API features degrade gracefully offline.
- Retrofit calls run on the IO dispatcher.
- DTOs are mapped to domain models before reaching ViewModels.
- All errors are converted to `AppResult.Error`.
- API keys are stored in `local.properties` and injected via Hilt; never committed to version control.

## Client Configuration

| Setting | Value |
| --- | --- |
| Connect timeout | 10 seconds |
| Read timeout | 15 seconds |
| Write timeout | 15 seconds |
| Serialization | Kotlinx Serialization or Moshi |
| Logging | Debug builds only |

---

## 1. Google Cloud Vision API

### Endpoint

```http
POST https://vision.googleapis.com/v1/images:annotate?key={API_KEY}
```

### Request

```json
{
  "requests": [
    {
      "image": { "content": "{BASE64_IMAGE}" },
      "features": [
        { "type": "LABEL_DETECTION", "maxResults": 10 },
        { "type": "TEXT_DETECTION" }
      ]
    }
  ]
}
```

### Response (label annotations)

```json
{
  "responses": [
    {
      "labelAnnotations": [
        { "description": "Food", "score": 0.97 },
        { "description": "Receipt", "score": 0.91 }
      ]
    }
  ]
}
```

### Usage

- Called after TFLite classification in the AI pipeline.
- Labels with `score >= 0.80` are converted to tags.
- Tags are merged with OCR-derived and TFLite-derived tags; duplicates are de-duplicated.
- On failure: log error, set `visionLabelStatus = FAILED`, continue without Vision tags.

---

## 2. Gemini API — Memo Recommendation

### Endpoint

```http
POST https://generativelanguage.googleapis.com/v1beta/models/gemini-pro-vision:generateContent?key={API_KEY}
```

### Request

```json
{
  "contents": [
    {
      "parts": [
        { "text": "이 이미지를 저장한 이유를 한 문장(50자 이내 한국어)으로 추천해 주세요." },
        { "inlineData": { "mimeType": "image/jpeg", "data": "{BASE64_IMAGE}" } }
      ]
    }
  ]
}
```

### Response

```json
{
  "candidates": [
    {
      "content": {
        "parts": [
          { "text": "카페 메뉴 가격을 나중에 참고하기 위해 저장했습니다." }
        ]
      }
    }
  ]
}
```

### Usage

- Called after Vision API labeling.
- The recommended sentence is shown in detail screen as a suggestion chip.
- User can accept (tap chip), edit, or dismiss.
- `geminiMemoStatus` transitions: `PENDING` → `RUNNING` → `SUGGESTED` → `ACCEPTED` / `DISMISSED`.
- On failure: hide suggestion UI, set status `FAILED`, user writes memo manually.

---

## 3. YouTube Data API v3

### Endpoint

```http
GET https://www.googleapis.com/youtube/v3/search?part=snippet&q={TITLE}&type=video&maxResults=1&key={API_KEY}
```

### Response

```json
{
  "items": [
    {
      "id": { "videoId": "dQw4w9WgXcQ" },
      "snippet": {
        "title": "Video Title",
        "thumbnails": { "default": { "url": "..." } }
      }
    }
  ]
}
```

### Usage

- Triggered only when TFLite category is `youtube`.
- OCR-extracted text is used as the `q` query parameter.
- First result's `videoId` generates the deep-link: `https://www.youtube.com/watch?v={videoId}`.
- Deep-link button "▶ 영상 바로 이동" is displayed in detail screen.
- On failure or no result: hide deep-link button; show optional retry.

---

## Error Response Format (internal)

```kotlin
sealed interface AppResult<out T> {
    data class Success<T>(val data: T) : AppResult<T>
    data class Error(val error: AppError) : AppResult<Nothing>
}
```

Common API errors:

| Error | Cause |
| --- | --- |
| `NetworkUnavailable` | No internet connection |
| `ApiQuotaExceeded` | Daily quota limit reached |
| `ApiUnauthorized` | Invalid or missing API key |
| `ApiTimeout` | Request exceeded timeout |
| `ApiParseError` | Unexpected response structure |
