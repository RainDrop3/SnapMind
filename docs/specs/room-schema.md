# Room Schema

## Database

- Name: `snapmind.db`
- Version: `1`
- Export schema: `true`

## `memory_items`

| Column | Type | Nullable | Notes |
| --- | --- | --- | --- |
| `id` | Long | No | Primary key, auto-generated |
| `imageUri` | String | No | App-private URI/path |
| `sourceUri` | String | Yes | Original shared URI if retained |
| `mimeType` | String | Yes | Image MIME type |
| `createdAt` | Long | No | Epoch millis |
| `updatedAt` | Long | No | Epoch millis |
| `ocrStatus` | String | No | `PENDING/RUNNING/SUCCESS/FAILED` |
| `classificationStatus` | String | No | `PENDING/RUNNING/SUCCESS/FAILED` |
| `taggingStatus` | String | No | `PENDING/RUNNING/SUCCESS/FAILED` |
| `isPinned` | Boolean | No | Default false |
| `archivedAt` | Long | Yes | Null when active |

## `ocr_texts`

| Column | Type | Nullable | Notes |
| --- | --- | --- | --- |
| `memoryId` | Long | No | Primary key, foreign key |
| `fullText` | String | No | Normalized OCR text |
| `rawText` | String | Yes | Optional raw recognizer text |
| `createdAt` | Long | No | Epoch millis |

## `classifications`

| Column | Type | Nullable | Notes |
| --- | --- | --- | --- |
| `id` | Long | No | Primary key |
| `memoryId` | Long | No | Foreign key |
| `label` | String | No | Model label |
| `confidence` | Float | No | 0.0 to 1.0 |
| `modelVersion` | String | No | Model asset version |
| `rank` | Int | No | 1 for top prediction |

## `tags`

| Column | Type | Nullable | Notes |
| --- | --- | --- | --- |
| `id` | Long | No | Primary key |
| `name` | String | No | Unique normalized tag |
| `displayName` | String | No | UI label |
| `source` | String | No | `GENERATED/USER/SYSTEM` |
| `createdAt` | Long | No | Epoch millis |
| `updatedAt` | Long | No | Epoch millis |
| `isArchived` | Boolean | No | Hide from default drawer lists when true |

## `memory_tag_cross_refs`

| Column | Type | Nullable | Notes |
| --- | --- | --- | --- |
| `memoryId` | Long | No | Foreign key |
| `tagId` | Long | No | Foreign key |
| `assignedBy` | String | No | `AUTO/USER` |
| `createdAt` | Long | No | Epoch millis |

## `memos`

| Column | Type | Nullable | Notes |
| --- | --- | --- | --- |
| `memoryId` | Long | No | Primary key, foreign key |
| `body` | String | No | User memo text |
| `createdAt` | Long | No | Epoch millis |
| `updatedAt` | Long | No | Epoch millis |

## Indexes

- `memory_items(imageUri)` unique.
- `memory_items(createdAt)`.
- `ocr_texts(memoryId)` unique.
- `tags(name)` unique.
- `tags(source)`.
- `tags(isArchived)`.
- `memory_tag_cross_refs(memoryId, tagId)` primary composite.
- `memory_tag_cross_refs(tagId)`.
- `classifications(memoryId, label)`.
- `classifications(label)`.
- `memos(memoryId)` unique.

## Drawer Query Requirements

### Category Counts

Return visible memory count grouped by top classification label.

```sql
SELECT label, COUNT(DISTINCT memoryId) AS count
FROM classifications
WHERE rank = 1
GROUP BY label
ORDER BY count DESC, label ASC
```

### Popular Tag Counts

Return most-used active tags for the drawer.

```sql
SELECT tags.id, tags.name, tags.displayName, COUNT(memory_tag_cross_refs.memoryId) AS count
FROM tags
JOIN memory_tag_cross_refs ON tags.id = memory_tag_cross_refs.tagId
WHERE tags.isArchived = 0
GROUP BY tags.id
ORDER BY count DESC, tags.displayName ASC
LIMIT :limit
```
