# Database Design

## Goal

Define Room database entities, relationships, indexes, FTS configuration, and query patterns for local-first memory management.

## Database Name

`snapmind.db`

## Entities

| Entity | Purpose |
| --- | --- |
| `MemoryItemEntity` | Main image memory record |
| `OcrTextEntity` | ML Kit OCR full text and block-level extraction metadata |
| `TagEntity` | Unique tag definitions (source: OCR / Vision API / TFLite / user) |
| `MemoryTagCrossRef` | N:M relationship between memories and tags |
| `ClassificationEntity` | TFLite CNN category prediction results |
| `VisionLabelEntity` | Google Cloud Vision API label results per memory |
| `MemoEntity` | User-authored and Gemini-recommended memo content |
| `MemoryFts` | FTS4/FTS5 virtual table over OCR text and memo text |

## Relationships

- One `MemoryItemEntity` has zero or one `OcrTextEntity`.
- One `MemoryItemEntity` has zero or many `ClassificationEntity` rows.
- One `MemoryItemEntity` has zero or many `VisionLabelEntity` rows.
- One `MemoryItemEntity` has zero or one `MemoEntity`.
- One `MemoryItemEntity` has many `TagEntity` rows through `MemoryTagCrossRef` (N:M).

## Key Fields

### MemoryItemEntity

| Field | Type | Notes |
| --- | --- | --- |
| `id` | Long PK | Auto-generated |
| `imageUri` | String UNIQUE | App-private URI |
| `createdAt` | Long | Epoch millis |
| `ocrStatus` | Enum | PENDING / RUNNING / SUCCESS / FAILED |
| `classificationStatus` | Enum | PENDING / RUNNING / SUCCESS / FAILED |
| `visionLabelStatus` | Enum | PENDING / RUNNING / SUCCESS / FAILED / SKIPPED |
| `geminiMemoStatus` | Enum | PENDING / RUNNING / SUGGESTED / ACCEPTED / DISMISSED / FAILED |
| `taggingStatus` | Enum | PENDING / RUNNING / SUCCESS / FAILED |
| `isFavorite` | Boolean | Favorites tab filter |
| `deletedAt` | Long? | Soft delete (trash); null = active |

### VisionLabelEntity

| Field | Type | Notes |
| --- | --- | --- |
| `id` | Long PK | |
| `memoryId` | Long FK | |
| `label` | String | e.g. "Food", "Receipt" |
| `score` | Float | Vision API confidence score |

### MemoEntity

| Field | Type | Notes |
| --- | --- | --- |
| `memoryId` | Long PK/FK | |
| `body` | String | Final user-saved memo text |
| `geminiSuggestion` | String? | Gemini API recommendation; null if not available |
| `updatedAt` | Long | |

## FTS Configuration

Room FTS virtual table for fast full-text search:

```kotlin
@Fts4(contentEntity = OcrTextEntity::class)
@Entity(tableName = "memory_fts")
data class MemoryFts(
    val ocrFullText: String,
    val memoBody: String
)
```

FTS supports searches across:

- OCR full text
- Memo body

Tag and category searches use standard `LIKE` or `IN` queries joining `TagEntity` and `ClassificationEntity`.

## Index Strategy

| Table | Index | Reason |
| --- | --- | --- |
| `memory_items` | `createdAt` | Date sorting |
| `memory_items` | `imageUri` UNIQUE | Prevent duplicate imports |
| `memory_items` | `deletedAt` | Trash / active filter |
| `memory_items` | `isFavorite` | Favorites tab |
| `ocr_texts` | `memoryId` UNIQUE | Fast join from memory |
| `tags` | `name` UNIQUE | Tag lookup and de-duplication |
| `tags` | `source`, `isArchived` | Tag management and popular tag lists |
| `memory_tag_cross_refs` | `memoryId`, `tagId` | N:M joins |
| `memory_tag_cross_refs` | `tagId` | Tag usage counts for top-3 shortcuts |
| `classifications` | `memoryId`, `label` | Category filtering |
| `classifications` | `label` | Drawer category counts |
| `vision_labels` | `memoryId` | Join from memory |
| `vision_labels` | `label`, `score` | High-confidence label lookup |
| `memos` | `memoryId` UNIQUE | Detail screen memo loading |

## Search Queries

| Query Type | Strategy |
| --- | --- |
| Full-text (OCR + memo) | Room FTS `MATCH` query |
| Tag filter | Join `memory_tag_cross_refs` + `tags` |
| Category filter | Join `classifications` by `label` |
| Date range | `WHERE createdAt BETWEEN ? AND ?` |
| Favorites | `WHERE isFavorite = 1` |
| Trash | `WHERE deletedAt IS NOT NULL` |
| Top-3 tags | `GROUP BY tagId ORDER BY COUNT(*) DESC LIMIT 3` |

## Drawer Utility Queries

- **인기 태그 TOP 3**: group `memory_tag_cross_refs` by `tagId`, order by count DESC, join `tags`, limit 3.
- **휴지통**: select `memory_items WHERE deletedAt IS NOT NULL`.
- **태그별 사진**: join `memory_items` with `memory_tag_cross_refs` filtered by selected `tagId`.

## Migration Policy

- Never use destructive migrations outside local debug builds.
- Every schema change must include a Room migration class.
- Update `specs/room-schema.md` with entity and version changes.
- Add migration tests before production releases.
