# System Overview

## Goal

Define how SnapMind ingests image memories, processes them with OCR and AI classification, stores structured metadata, and exposes retrieval/editing workflows through Android UI.

## High-Level Components

| Component | Responsibility |
| --- | --- |
| Android UI | Screens, list/detail interactions, share intake confirmation |
| ViewModel Layer | UI state, events, coroutine orchestration |
| Domain Layer | Use cases for import, OCR, classification, tagging, search, memo update |
| Repository Layer | Coordinates local database, file storage, AI processors, and optional API |
| Room Database | Stores memory records, OCR text, tags, categories, memo content, processing status |
| File Storage | Stores copied image files or persistent URI references |
| ML Kit OCR | Extracts text from screenshots/photos |
| TensorFlow Lite | Performs on-device image classification |
| Retrofit API | Reserved for optional remote metadata/model config endpoints |

## Primary Data Lifecycle

1. User shares or imports an image.
2. App validates image MIME type and access permission.
3. Repository stores a local image reference and creates a `MemoryItem`.
4. Background processing runs OCR and image classification.
5. Auto-tagging combines OCR keywords, classification results, and app rules.
6. Processed metadata is persisted in Room.
7. User searches, filters, edits memo content, or deletes/archive items.

## Processing Modes

| Mode | Description |
| --- | --- |
| Immediate | Small image is processed after import while user sees progress |
| Deferred | Large image or batch import is queued and processed in background |
| Retry | Failed OCR/classification tasks can be retried from detail screen |

## Core Constraints

- Image processing must not block the main thread.
- Room must be the source of truth for UI state.
- OCR/classification status must be stored so interrupted processing can resume.
- Search should work offline.
- App must handle revoked URI permissions and missing files gracefully.

