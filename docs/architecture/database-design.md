# Database Design

## Goal

Define Room database entities, relationships, indexes, and query patterns for local-first memory management.

## Database Name

`snapmind.db`

## Entities

| Entity | Purpose |
| --- | --- |
| `MemoryItemEntity` | Main image memory record |
| `OcrTextEntity` | OCR full text and block-level extraction metadata |
| `TagEntity` | Unique tag definitions |
| `MemoryTagCrossRef` | Many-to-many relationship between memories and tags |
| `ClassificationEntity` | TFLite category prediction results |
| `MemoEntity` | User-authored memo content |

## Relationships

- One `MemoryItemEntity` has zero or one `OcrTextEntity`.
- One `MemoryItemEntity` has zero or many `ClassificationEntity` rows.
- One `MemoryItemEntity` has zero or one `MemoEntity`.
- One `MemoryItemEntity` has many `TagEntity` rows through `MemoryTagCrossRef`.

## Index Strategy

| Table | Index | Reason |
| --- | --- | --- |
| `memory_items` | `createdAt` | Date sorting |
| `memory_items` | `imageUri` unique | Prevent duplicate imports |
| `ocr_texts` | `memoryId` unique | Fast join from memory |
| `tags` | `name` unique | Tag lookup and de-duplication |
| `tags` | `source`, `isArchived` | Drawer tag management and popular tag lists |
| `memory_tag_cross_refs` | `memoryId`, `tagId` | Many-to-many joins |
| `memory_tag_cross_refs` | `tagId` | Tag usage counts for drawer filtering |
| `classifications` | `memoryId`, `label` | Category filtering |
| `classifications` | `label` | Drawer category counts |
| `memos` | `memoryId` unique | Detail screen memo loading |

## Search Optimization

Initial release uses SQL `LIKE` queries across:

- OCR full text.
- memo text.
- tag name.
- classification label.

## Drawer Filtering Queries

The Memory page drawer needs lightweight counts and immediate filter queries:

- Category list: group top-ranked `classifications.label` by visible memory count.
- Popular tags: group `tags` through `memory_tag_cross_refs`, excluding archived tags.
- Category filter: join `memory_items` with rank-1 `classifications`.
- Tag filter: join `memory_items` with `memory_tag_cross_refs`.
- Tag management: create/rename/archive user-managed `TagEntity` rows.

If result size grows, add FTS virtual table for OCR and memo content.

## Migration Policy

- Never use destructive migrations outside local debug builds.
- Every schema change must add a Room migration.
- Update `specs/room-schema.md` with entity and version changes.
- Add migration tests for production releases.
