# SnapMind Project Plan

## Project Summary

SnapMind is an Android memory management app that automatically classifies and tags screenshots/photos using AI, making them searchable and retrievable at any time.

- Academic context: SW 2026 Term Project, Pusan National University
- Platform: Android (Kotlin)
- Core AI: TensorFlow Lite CNN (on-device) + Gemini API + Google Cloud Vision API + ML Kit OCR

## Problem Statement

Users save screenshots for later reference but often forget:

- why the screenshot was saved
- what text or context was inside it
- which category it belongs to
- how to find it later

SnapMind solves this by extracting text, classifying images, generating tags via Vision API, and auto-recommending memos via Gemini API.

## Scoring Requirements

This project is evaluated on the following criteria:

| Category | Points | Implementation |
| --- | --- | --- |
| Machine Learning | 50 | TFLite CNN model trained from scratch |
| External APIs (×3) | 60 | Gemini API · Google Cloud Vision API · YouTube Data API v3 |
| Database | 30 | Room DB |
| Jetpack Components (×4) | 30 | RecyclerView · Fragment · ViewPager2 · DrawerLayout |
| Coroutine | 20 | Async pipeline throughout |
| Download Manager | 20 | Glide image loading |
| External App Integration | 20 | Gallery share via ACTION_SEND |
| Stability | 30 | Relative evaluation (max 30) |
| **Base Total** | **230** | Excluding stability/completeness |

## Product Goals

- Import screenshots/photos via Android share sheet (ACTION_SEND).
- Store image memories locally with reliable file/URI handling.
- Extract OCR text with ML Kit.
- Classify image category with TensorFlow Lite CNN (trained from scratch).
- Generate visual tags with Google Cloud Vision API.
- Auto-recommend memo text via Gemini API.
- Navigate to YouTube videos from YouTube screenshot thumbnails using YouTube Data API v3.
- Search by OCR text, memo text, tag, category, and date (Room FTS).
- Provide fast navigation with `ViewPager2`, `BottomNavigationView`, and `DrawerLayout`.
- Keep core behavior offline and local-first; APIs degrade gracefully when offline.

## Initial Release Scope

| Area | Included |
| --- | --- |
| Import | `ACTION_SEND`, app-private image copy, import confirmation |
| Navigation | `ViewPager2`, bottom navigation (4 tabs), right-side DrawerLayout |
| OCR | ML Kit text recognition, normalized text persistence |
| Classification | Self-trained TFLite CNN, category confidence storage |
| Vision Tagging | Google Cloud Vision API visual label generation |
| Memo Recommendation | Gemini API auto-recommend memo, user accept/edit |
| YouTube Integration | ML Kit OCR title extraction + YouTube Data API v3 deep link |
| Tags | Auto-generated tags (OCR + Vision API), user tags, tag create/rename/delete |
| Search | Room FTS query by OCR/memo/tag/category/date |
| Memo | Edit memo, favorites, delete memory |
| Storage | Local Room DB and app-private image files |
| Image Loading | Glide with disk cache and processing status overlay |

## Out of Scope for Initial Release

- User accounts and authentication.
- Cloud sync and multi-device backup.
- Server-side image processing.
- In-app model training.
- Notion or other third-party note integrations.

## Core Feature Plan

### 1. Screenshot/Photo Import (외부 APP 연동 · 20점)

Users share an image into SnapMind via `ACTION_SEND`. The app validates input, copies the image to app-private storage, creates a `MemoryItem`, and queues the processing pipeline.

Implementation docs:

- `docs/features/screenshot-sharing.md`
- `docs/specs/permission-storage-spec.md`
- `docs/architecture/background-processing.md`

### 2. Main Navigation

The app shell uses `ViewPager2` for swipe navigation and `BottomNavigationView` for direct tab access.

Bottom navigation tabs (4):

| Index | Tab | Purpose |
| ---: | --- | --- |
| 0 | 홈 | Saved screenshots/photos grid |
| 1 | 즐겨찾기 | Favorited memories |
| 2 | 태그별 사진 | Browse by tag |
| 3 | 설정 | App settings |

Additional UI anchors:

- **Search button**: top-right of home screen (opens search)
- **FAB (+)**: bottom-right for uploading a new image

Jetpack components used (4개 · 30점):

- `Fragment` — each tab managed independently
- `ViewPager2` — swipe between tabs
- `DrawerLayout` — right-side slide menu
- `RecyclerView` — gallery grid and search result list

Implementation docs:

- `docs/features/swipe-navigation.md`
- `docs/specs/navigation-flow.md`
- `docs/specs/screen-specs.md`

### 3. Right-Side Drawer (DrawerLayout · Jetpack)

The app uses `DrawerLayout` as a right-side utility menu accessible from the main toolbar.

Drawer menu items:

| Item | Purpose |
| --- | --- |
| 🗑 휴지통 | View and restore deleted memories |
| 📄 PDF로 추출하기 | Export selected memories as PDF |
| 👨‍💻 개발자 소개 | Developer info page |
| 🏷 인기 태그 TOP 3 바로가기 | Shortcut to the 3 most-used tags |

Implementation docs:

- `docs/features/drawer-filtering.md`
- `docs/specs/screen-specs.md`

### 4. OCR Processing (Coroutine · 20점)

ML Kit extracts text from imported images on a background coroutine dispatcher. OCR text is normalized, stored in Room, and used for FTS search and tag generation.

Implementation docs:

- `docs/features/ocr-processing.md`
- `docs/architecture/ai-pipeline.md`

### 5. TFLite Image Classification (머신러닝 · 50점)

A self-trained CNN model (bundled as TFLite asset) classifies images into categories. Confidence score is stored with each result.

Categories: `chat` · `receipt` · `code` · `shopping` · `travel` · `food` · `document`

Implementation docs:

- `docs/features/ai-image-classification.md`
- `docs/specs/ml-training-spec.md`

### 6. Google Cloud Vision API — Image Labeling (API 2/3)

Vision API analyzes objects, scenes, and visual elements to generate precise auto-tags. Combined with ML Kit OCR to provide both text-based and vision-based tags simultaneously.

Implementation docs:

- `docs/features/auto-tagging.md`
- `docs/specs/retrofit-spec.md`

### 7. Gemini API — Memo Auto-Recommendation (API 1/3)

When an image is saved, Gemini API analyzes the image content and recommends a memo sentence explaining "why it was saved." The user can accept the recommendation or edit it freely.

Implementation docs:

- `docs/features/memo-management.md`
- `docs/specs/retrofit-spec.md`

### 8. YouTube Data API v3 — Video Navigation (API 3/3)

When TFLite classifies an image as a YouTube screenshot, ML Kit OCR extracts the video title. YouTube Data API v3 searches for the video and displays a deep-link button to open it in the YouTube app.

Implementation docs:

- `docs/specs/retrofit-spec.md`
- `docs/architecture/ai-pipeline.md`

### 9. Room DB — Local-First Storage (DB · 30점)

`MemoryItem` · `Tag` · N:M mapping table with Room FTS index for fast full-text search across OCR text, memo text, and tags.

Implementation docs:

- `docs/architecture/database-design.md`
- `docs/specs/room-schema.md`

### 10. Glide — Image Loading (다운로드 매니저 · 20점)

Glide loads and caches hundreds of thumbnails efficiently in the RecyclerView gallery. Disk caching ensures smooth scrolling, and processing status overlays (processing / done / error) are shown per item.

### 11. Auto-Tagging and Tag Management

Tags are generated from three sources: OCR keyword extraction, TFLite classification label, and Google Cloud Vision API visual labels. Users can also create, rename, delete, assign, and remove tags.

Implementation docs:

- `docs/features/auto-tagging.md`

### 12. Search and Filter (Room FTS)

Full-text search via Room FTS index across OCR text, memo text, tag names, and classification labels. Supports date range filtering.

Implementation docs:

- `docs/features/search-filter.md`
- `docs/architecture/database-design.md`

## Activity and Fragment Structure

| Component | Type | Role |
| --- | --- | --- |
| `MainActivity` | Activity | App shell: tabs, ViewPager2, DrawerLayout |
| `ShareActivity` | Activity | Receive external share intent (`ACTION_SEND`) |
| `DetailActivity` | Activity | Image detail: OCR, memo edit, tags, YouTube link |
| `HomeFragment` | Fragment | Memory grid (tab 0) |
| `FavoritesFragment` | Fragment | Favorited memories (tab 1) |
| `TagBrowseFragment` | Fragment | Photos by tag (tab 2) |
| `SettingsFragment` | Fragment | App settings (tab 3) |

Intent flow:

- `ShareActivity` → `MainActivity` (one-way, triggers import pipeline)
- `MainActivity` ↔ `DetailActivity` (two-way: memo edit result returned)

## Architecture Plan

| Layer | Responsibility |
| --- | --- |
| UI | Activity/Fragment screens, rendering state and forwarding events |
| ViewModel | `StateFlow` UI state, event handling, coroutine orchestration |
| Domain | Use cases and domain models |
| Repository | Coordinates Room, file storage, OCR, TFLite, Vision API, Gemini API, YouTube API, tagging |
| Local Data | Room entities, DAOs, FTS virtual tables, migrations |
| AI / Remote | ML Kit OCR, TFLite classifier, Gemini API, Vision API, YouTube API via Retrofit |

Primary architecture docs:

- `docs/architecture/system-overview.md`
- `docs/architecture/android-architecture.md`
- `docs/architecture/database-design.md`
- `docs/architecture/ai-pipeline.md`
- `docs/architecture/background-processing.md`
- `docs/architecture/privacy-security.md`

## Technology Stack

| Area | Technology |
| --- | --- |
| Language | Kotlin |
| Architecture | MVVM, Repository pattern, Use cases |
| Async | Coroutine, Flow |
| UI | Android View system, Fragment, RecyclerView, ViewPager2 |
| Navigation | BottomNavigationView, DrawerLayout, Navigation Component |
| Database | Room DB + FTS virtual table |
| File Storage | App-private storage, ContentResolver |
| OCR | ML Kit Text Recognition |
| ML | TensorFlow Lite (self-trained CNN) |
| Image Loading | Glide (disk cache, thumbnail) |
| APIs | Gemini API · Google Cloud Vision API · YouTube Data API v3 |
| Network | Retrofit + Kotlinx Serialization |
| DI | Hilt |
| Background Work | Coroutine (foreground), WorkManager (durable processing) |
| Testing | JUnit, Room tests, ViewModel tests, instrumented UI tests |

## Data Lifecycle (Image Processing Pipeline)

```text
이미지 수신 (ACTION_SEND)
  → MIME validation
  → app-private image copy
  → MemoryItem insert (Room · pending)
  → OCR extraction (ML Kit · Coroutine background)
  → TFLite classification (CNN · Coroutine background)
  → Vision API labeling (Google Cloud Vision · Retrofit)
  → Gemini API memo recommendation (auto-suggest to user)
  → YouTube title match (if category == youtube · YouTube Data API)
  → Room DB confirm (FTS indexed · searchable)
```

OCR → TFLite → Vision API → Gemini API steps run on Coroutine background dispatchers and do not block the UI thread.

## Implementation Order

1. Project package structure and dependency setup (Hilt, Room, Retrofit, Glide, TFLite, ML Kit).
2. Room schema v1 with FTS virtual table and DAOs.
3. App-private image storage and import flow (ShareActivity → MainAcitvity).
4. Main app shell: ViewPager2 (4 tabs), BottomNavigationView, DrawerLayout (right-side).
5. OCR processing pipeline (ML Kit, Coroutine).
6. TFLite classification pipeline (self-trained CNN).
7. Google Cloud Vision API integration (auto-tagging).
8. Gemini API integration (memo auto-recommendation).
9. YouTube Data API integration (deep-link button on YouTube screenshots).
10. Auto-tagging (OCR + Vision API + TFLite labels).
11. Glide image loading with processing status overlays.
12. Search/filter with Room FTS.
13. Memo edit, favorites, delete.
14. Drawer menu items (trash, PDF export, developer info, top-3 tag shortcuts).
15. Testing, performance, privacy, and release hardening.

## Risk Register

| Risk | Impact | Mitigation |
| --- | --- | --- |
| Large images cause memory pressure | Crash or slow import | Decode with bounds, copy original separately, process downsampled bitmap |
| URI permission expires | Missing image after import | Copy into app-private storage during import |
| OCR/classification takes too long | Poor UX | Use background queue and visible processing status |
| API quota exceeded (Vision / Gemini / YouTube) | Feature unavailable | Degrade gracefully; show manual fallback without crash |
| Network unavailable when API is called | Feature unavailable | Queue API calls or skip with offline indicator |
| TFLite model classification accuracy | Wrong category tags | Provide category correction in detail screen |
| Tag noise from Vision API | Search/filter quality drops | Cap generated tags and allow user removal |
| Sensitive OCR text exposure | Privacy issue | Keep processing local for OCR; no raw image sent to APIs |

## Documentation Maintenance

- Feature scope lives in `docs/features/`.
- Implementation status lives in `docs/todos/` and `docs/progress/`.
- Technical contracts live in `docs/specs/`.
- Architecture decisions live in `docs/architecture/`.
- Update this plan only when project direction changes.
