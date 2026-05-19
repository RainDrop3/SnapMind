# Roadmap

## Product Scope

SnapMind helps users save screenshots/photos, extract text, classify image content, attach memos, and retrieve memories through search and filters.

## Milestones

| Milestone | Goal | Target Status | Progress |
| --- | --- | --- | --- |
| M0 Project Foundation | Package structure, dependency setup, ViewPager2 bottom navigation shell, DrawerLayout filter shell, Room skeleton | Not Started | 0% |
| M1 Capture and Import | Share sheet intake and gallery/photo import persistence | Not Started | 0% |
| M2 OCR Pipeline | ML Kit OCR extraction, persistence, and searchable text | Not Started | 0% |
| M3 AI Classification | TFLite image category inference and confidence storage | Not Started | 0% |
| M4 Auto-Tagging | Rule-based and AI-assisted tag generation | Not Started | 0% |
| M5 Search and Filter | Full-text-like search, tag/category/date filters | Not Started | 0% |
| M6 Memo Management | Manual memo editing, pin/archive/delete workflows | Not Started | 0% |
| M7 Stabilization | Offline behavior, performance tuning, tests, crash hardening | Not Started | 0% |

## Release Readiness Checklist

- [ ] App supports Android share intent for image input.
- [ ] Imported images are stored with stable local URIs.
- [ ] OCR runs asynchronously and persists recognized text blocks.
- [ ] TFLite classification runs off the main thread.
- [ ] Search returns results by OCR text, memo text, tags, and category.
- [ ] Room migrations are defined for every schema change.
- [ ] ViewModels expose stable UI state models.
- [ ] Error states are visible and retryable.
- [ ] Unit tests cover repositories, use cases, and tag rules.
- [ ] Instrumented tests cover import, search, and memo edit flows.

## Priority Order

1. Foundation: package structure, DI, Room, navigation.
2. Main app shell: ViewPager2 swipe navigation, bottom navigation access, and DrawerLayout category/tag filtering.
3. Data ingestion: screenshot sharing and photo import.
4. Processing: OCR, classification, auto-tagging.
5. Retrieval: search and filter.
6. Curation: memo management, archive, delete.
7. Quality: tests, performance, accessibility, crash handling.

## Out of Scope for Initial Release

- Cloud sync between devices.
- User accounts.
- Server-side image processing.
- Real-time camera capture.
- Custom model training inside the mobile app.
