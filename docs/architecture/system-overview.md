# System Overview

## Goal

Define how SnapMind ingests image memories, processes them through a multi-step AI pipeline (OCR, TFLite classification, Vision API labeling, Gemini memo recommendation), stores structured metadata, and exposes retrieval/editing workflows through Android UI.

## High-Level Components

| Component | Responsibility |
| --- | --- |
| Android UI | Screens, list/detail interactions, share intake confirmation |
| ViewModel Layer | UI state, events, coroutine orchestration |
| Domain Layer | Use cases for import, OCR, classification, Vision tagging, Gemini memo, YouTube search, search, memo update |
| Repository Layer | Coordinates local database, file storage, AI processors, and external APIs |
| Room Database | Stores memory records, OCR text, Vision labels, tags, categories, memo content, processing status |
| File Storage | Stores copied image files in app-private storage |
| ML Kit OCR | Extracts text from screenshots/photos on-device |
| TensorFlow Lite | Performs on-device image classification (self-trained CNN) |
| Google Cloud Vision API | Generates visual object/scene labels for auto-tagging |
| Gemini API | Recommends memo text based on image content |
| YouTube Data API v3 | Searches for video by title extracted via OCR; returns deep link |
| Glide | Thumbnail image loading with disk cache |
| WorkManager | Runs durable OCR/classification/API work after import |
| Retrofit | HTTP client for Gemini API, Vision API, YouTube Data API |

## Primary Data Lifecycle

1. User shares or imports an image via `ACTION_SEND`.
2. App validates image MIME type and copies it to app-private storage.
3. Repository creates a `MemoryItem` (status: pending) and enqueues processing.
4. Background coroutine runs ML Kit OCR and TFLite classification.
5. Vision API call generates visual labels for auto-tagging.
6. Gemini API call recommends a memo sentence; user accepts or edits.
7. If category is `youtube`, OCR title triggers a YouTube Data API search and deep-link button.
8. All results are confirmed in Room DB with FTS index; item becomes searchable.
9. User searches, filters, edits memo content, favorites, or deletes/archives items.

## Processing Pipeline

```text
이미지 수신 (ACTION_SEND)
  → MIME validation + app-private copy
  → Room insert (pending)
  → ML Kit OCR         [Coroutine · IO dispatcher]
  → TFLite classify    [Coroutine · Default dispatcher]
  → Vision API label   [Retrofit · IO dispatcher]
  → Gemini memo hint   [Retrofit · IO dispatcher]
  → YouTube deeplink   [Retrofit · IO dispatcher · if youtube category]
  → Room confirm       [FTS indexed]
```

## Processing Modes

| Mode | Description |
| --- | --- |
| Immediate | Small image processed after import; user sees progress overlay |
| Deferred | Large image or batch import queued via WorkManager |
| Retry | Failed steps retryable from detail screen |

## Core Constraints

- On-device processing (OCR, TFLite) must not block the main thread.
- Room is the source of truth for all UI state.
- Processing status is persisted so interrupted work can resume.
- Full-text search works offline via Room FTS.
- External API calls (Vision, Gemini, YouTube) degrade gracefully when offline.
- App must handle revoked URI permissions and missing files gracefully.
- Imported image files and OCR text are private by default; raw images are not sent to external services.

## Related Documents

- `docs/specs/permission-storage-spec.md`
- `docs/architecture/background-processing.md`
- `docs/architecture/privacy-security.md`
- `docs/specs/testing-strategy.md`
- `docs/specs/retrofit-spec.md`
